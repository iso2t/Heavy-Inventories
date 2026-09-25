"""Release validation/packaging. Standard library only; validation never uses tokens."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

LOADERS = ('fabric', 'neoforge')
VERSION = re.compile(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:\.(0|[1-9]\d*))?(?:-(alpha|beta|rc)\.([1-9]\d*))?')


class ReleaseError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise ReleaseError(message)


def json_bytes(value):
    return (json.dumps(value, indent=2, sort_keys=True) + '\n').encode('utf-8')


def digest(data, algorithm='sha256'):
    return hashlib.new(algorithm, data).hexdigest()


def read_json(path):
    return json.loads(Path(path).read_text(encoding='utf-8'))


def git(*args, root='.'):
    result = subprocess.run(['git', *args], cwd=root, capture_output=True, text=True)
    require(result.returncode == 0, 'Git validation failed: ' + ' '.join(args))
    return result.stdout.strip()


def channel(version):
    match = VERSION.fullmatch(version)
    require(match is not None, 'Unsupported version; use X.Y.Z[-alpha.N|-beta.N|-rc.N] (four numeric components also supported).')
    return 'alpha' if match[5] == 'alpha' else 'beta' if match[5] else 'release'


def version_key(version):
    channel(version)
    return tuple(int(part) for part in version.split('-')[0].split('.'))


def properties(root):
    result = {}
    for line in (root / 'gradle.properties').read_text(encoding='utf-8').splitlines():
        if line.strip() and not line.lstrip().startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            require(key.strip() not in result, 'Duplicate Gradle property')
            result[key.strip()] = value.strip()
    return result


def policy(root):
    value = read_json(root / 'release/publishing.json')
    require(value.get('schema') == 1, 'Unsupported publishing policy schema')
    require(re.fullmatch(r'[\w.-]+/[\w.-]+', value.get('repository', '')), 'Invalid repository')
    require(value.get('release_branches'), 'No trusted release branches configured')
    require(set(value.get('dependencies', {})) == set(LOADERS), 'Expected both loader dependency lists')
    for dependencies in value['dependencies'].values():
        require(dependencies, 'Missing dependency configuration')
        for dep in dependencies:
            require(re.fullmatch(r'[A-Za-z0-9]{8}', dep.get('modrinth', '')), 'Invalid Modrinth dependency ID')
            require(isinstance(dep.get('curseforge'), int) and dep['curseforge'] > 0, 'Invalid CurseForge dependency ID')
            require(re.fullmatch(r'[a-z0-9-]+', dep.get('slug', '')), 'Invalid dependency slug')
    return value


def notes(root, version):
    path = root / 'changelogs' / (version + '.md')
    require(path.is_file(), 'Missing version changelog: ' + path.name)
    text = path.read_text(encoding='utf-8').strip() + '\n'
    require(bool(text.strip()) and text.splitlines()[0] == '# ' + version, 'Changelog heading must match the version')
    require(re.search(r'^- \S.+', text, re.M), 'Changelog needs at least one real change')
    require(not re.search(r'\b(TODO|TBD|PLACEHOLDER)\b|<version>|<!--', text, re.I), 'Remove changelog placeholders/comments')
    require(not re.search(r'(?m)^##[^\n]*\n\s*(?=##|\Z)', text), 'Remove empty changelog sections')
    require(not re.search(r'<[^>]+>|[A-Za-z]:[\\/]|file://|codex://', text), 'Changelog contains HTML or a local/task path')
    # Absolute web links keep the identical Markdown useful on all three destinations.
    for target in re.findall(r'\]\(([^)]+)\)', text):
        require(re.fullmatch(r'https://[^\s]+', target), 'Use absolute HTTPS changelog links')
    require(not re.search(r'(?m)^\s*\[[^\]]+\]:', text), 'Use inline HTTPS links, not reference links')
    require('Cloth Config' in text and 'NeoForge' in text and 'client' in text.lower(),
            'Release notes must explain the NeoForge client Cloth Config requirement')
    return text


def metadata(root, tag=None, commit=None):
    props, rules = properties(root), policy(root)
    version = props['version']
    release_channel = channel(version)
    require(not tag or tag == 'v' + version, 'Tag does not match gradle.properties version')
    require(re.fullmatch(r'[0-9]+(?:\.[0-9]+)+', props['minecraft_version']), 'Expected an exact Minecraft version')
    body = notes(root, version)
    return {
        'schema': 1, 'repository': rules['repository'], 'version': version,
        'tag': 'v' + version, 'commit': commit or git('rev-parse', 'HEAD', root=root),
        'minecraft': props['minecraft_version'], 'java': int(props['java_version']),
        'mod_id': props['mod_id'], 'channel': release_channel,
        'latest_minecraft': rules['latest_minecraft'], 'dependencies': rules['dependencies'],
        'changelog_sha256': digest(body.encode('utf-8')),
    }, body


def resolve(root, tag):
    require(tag.startswith('v'), 'Release tag must start with v')
    channel(tag[1:])
    rules = policy(root)
    sha = git('rev-parse', '--verify', 'refs/tags/' + tag + '^{commit}', root=root)
    trusted = False
    for branch in rules['release_branches']:
        require(re.fullmatch(r'[A-Za-z0-9_./-]+', branch) and '..' not in branch, 'Invalid release branch')
        check = subprocess.run(['git', 'merge-base', '--is-ancestor', sha, 'refs/remotes/origin/' + branch], cwd=root, capture_output=True)
        trusted |= check.returncode == 0
    require(trusted, 'Tag must point to a commit merged into a configured release branch (main by default)')
    output = os.environ.get('GITHUB_OUTPUT')
    if output:
        with open(output, 'a', encoding='utf-8') as handle:
            handle.write(f'tag={tag}\nsha={sha}\n')
    return sha


def jar_identity(data, loader, meta):
    import io
    import tomllib
    with zipfile.ZipFile(io.BytesIO(data)) as jar:
        names = jar.namelist()
        require(not any('/test/' in n or 'lifecycle-test' in n for n in names), 'Test harness in release jar')
        if loader == 'fabric':
            value = json.loads(jar.read('fabric.mod.json'))
            require(value['id'] == meta['mod_id'] and value['version'] == meta['version'], 'Fabric jar identity mismatch')
        else:
            value = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode('utf-8'))
            require(any(m.get('modId') == meta['mod_id'] and m.get('version') == meta['version'] for m in value['mods']), 'NeoForge jar identity mismatch')


def package(root, tag, out):
    meta, body = metadata(root, tag)
    outputs = read_json(root / 'build/release/artifacts.json')
    require(set(outputs) == set(LOADERS), 'Artifact manifest must contain exactly Fabric and NeoForge')
    out.mkdir(parents=True, exist_ok=True)
    require(not list(out.iterdir()), 'Bundle directory must be empty; choose a new output directory')
    meta['files'] = {}
    for loader in LOADERS:
        path = (root / outputs[loader]).resolve()
        require(path.is_relative_to((root / loader / 'build/libs').resolve()), 'Artifact outside loader build/libs')
        expected = f"{meta['mod_id']}-{loader}-{meta['minecraft']}-{meta['version']}.jar"
        require(path.name == expected, 'Unexpected distributable filename: ' + path.name)
        data = path.read_bytes()
        jar_identity(data, loader, meta)
        (out / path.name).write_bytes(data)
        meta['files'][loader] = {'name': path.name, 'size': len(data), 'sha256': digest(data), 'sha512': digest(data, 'sha512')}
    meta['provenance'] = {'run_id': os.environ.get('GITHUB_RUN_ID', 'local'), 'run_attempt': os.environ.get('GITHUB_RUN_ATTEMPT', 'local')}
    (out / 'CHANGELOG.md').write_bytes(body.encode('utf-8'))
    (out / 'release-manifest.json').write_bytes(json_bytes(meta))
    (out / 'SHA256SUMS.txt').write_text(checksums(meta), encoding='utf-8', newline='\n')
    verify_bundle(out)
    return meta


def checksums(meta):
    return ''.join(f"{f['sha256']}  {f['name']}\n" for f in meta['files'].values()) + f"{meta['changelog_sha256']}  CHANGELOG.md\n"


def verify_bundle(bundle):
    meta = read_json(bundle / 'release-manifest.json')
    require(meta.get('schema') == 1 and set(meta.get('files', {})) == set(LOADERS), 'Invalid release manifest')
    require(re.fullmatch(r'[0-9a-f]{40}', meta.get('commit', '')), 'Invalid manifest commit')
    require(meta['tag'] == 'v' + meta['version'] and channel(meta['version']) == meta['channel'], 'Invalid manifest version/channel')
    for loader, info in meta['files'].items():
        expected = f"{meta['mod_id']}-{loader}-{meta['minecraft']}-{meta['version']}.jar"
        require(info['name'] == expected, 'Invalid manifest artifact name')
        data = (bundle / info['name']).read_bytes()
        require(len(data) == info['size'] and digest(data) == info['sha256'] and digest(data, 'sha512') == info['sha512'], 'Artifact hash mismatch')
        jar_identity(data, loader, meta)
    require(digest((bundle / 'CHANGELOG.md').read_bytes()) == meta['changelog_sha256'], 'Changelog hash mismatch')
    require((bundle / 'SHA256SUMS.txt').read_text(encoding='utf-8') == checksums(meta), 'Checksum file mismatch')
    expected_files = {f['name'] for f in meta['files'].values()} | {'CHANGELOG.md', 'SHA256SUMS.txt', 'release-manifest.json'}
    require({p.name for p in bundle.iterdir()} == expected_files, 'Unexpected file in release bundle')
    return meta


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['validate', 'resolve', 'package', 'preview', 'preflight', 'publish'])
    parser.add_argument('--root', type=Path, default=Path('.'))
    parser.add_argument('--tag', default=os.environ.get('RELEASE_TAG'))
    parser.add_argument('--bundle', type=Path, default=Path('build/release/bundle'))
    args = parser.parse_args()
    if args.command == 'resolve':
        require(bool(args.tag), 'Missing release tag')
        print(resolve(args.root, args.tag))
    elif args.command == 'validate':
        print(json.dumps(metadata(args.root, args.tag)[0], indent=2))
    elif args.command == 'package':
        print(json.dumps(package(args.root, args.tag, args.bundle), indent=2))
    else:
        from publisher import run
        run(args.command, args.root, args.bundle)


if __name__ == '__main__':
    try:
        main()
    except (ReleaseError, KeyError, ValueError, OSError, zipfile.BadZipFile) as error:
        # Do not print HTTP responses, request headers, or arbitrary server bodies.
        print('Release stopped: ' + str(error), file=sys.stderr)
        sys.exit(1)
