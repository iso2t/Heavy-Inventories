"""Coordinate four uploads with an append-only ledger in a GitHub draft release."""
import json
import os
import re

from platforms import CurseForge, GitHub, Modrinth
from release import LOADERS, ReleaseError, digest, json_bytes, metadata, require, verify_bundle, version_key

CONTRACT_FIELDS = ('schema', 'repository', 'version', 'tag', 'commit', 'minecraft', 'java', 'mod_id', 'channel',
                   'latest_minecraft', 'dependencies', 'changelog_sha256')
TARGETS = tuple((platform, loader) for platform in ('curseforge', 'modrinth') for loader in LOADERS)


def project_ids(env, required=True):
    cf, mr = env.get('CURSEFORGE_PROJECT_ID', ''), env.get('MODRINTH_PROJECT_ID', '')
    if required or cf:
        require(re.fullmatch(r'[1-9]\d*', cf), 'Set repository variable CURSEFORGE_PROJECT_ID to the numeric project ID')
    if required or mr:
        require(re.fullmatch(r'[A-Za-z0-9]{8}', mr), 'Set repository variable MODRINTH_PROJECT_ID to the permanent project ID')
    return {'curseforge': cf or '<CURSEFORGE_PROJECT_ID>', 'modrinth': mr or '<MODRINTH_PROJECT_ID>'}


def should_be_latest(meta, current):
    if meta['channel'] != 'release' or meta['minecraft'] != meta['latest_minecraft']:
        return False
    if not current:
        return True
    try:
        return version_key(meta['version']) > version_key(current['tag_name'].removeprefix('v'))
    except ReleaseError:
        return False


class Publisher:
    def __init__(self, github, platforms, meta, bundle, body, projects, evidence=None):
        self.github, self.platforms = github, platforms
        self.meta, self.bundle, self.body, self.projects = meta, bundle, body, projects
        self.evidence = evidence or {}
        require(isinstance(self.evidence, dict) and set(self.evidence) <= {p + ':' + l for p, l in TARGETS},
                'Recovery JSON keys must be platform:loader targets')
        require(all(isinstance(v, dict) for v in self.evidence.values()), 'Recovery entries must be objects')
        self.state = {'schema': 1, 'projects': projects, 'entries': {}}
        self.sequence = 0
        self.release = None
        self.assets = {}

    def restore(self):
        self.release = self.github.release(self.meta['tag'])
        if self.release is None:
            return False
        self.github.preflight(self.meta)
        require(self.release['tag_name'] == self.meta['tag'] and self.release['body'] == self.body
                and self.release['prerelease'] == (self.meta['channel'] != 'release'), 'Existing GitHub release metadata conflict')
        assets = self.github.assets(self.release['id'])
        require(len({a['name'] for a in assets}) == len(assets), 'Duplicate GitHub release asset names')
        self.assets = {a['name']: a for a in assets}
        if 'release-manifest.json' in self.assets:
            raw = self.github.read_asset(self.assets['release-manifest.json'])
            original = json.loads(raw)
            require(all(original.get(k) == self.meta.get(k) for k in CONTRACT_FIELDS), 'Existing release manifest/source conflict; use a new version')
            # Rebuilds can differ byte-for-byte. The original staged bundle is authoritative.
            canonical = self.bundle.parent / 'canonical'
            canonical.mkdir(exist_ok=True)
            require(not list(canonical.iterdir()), 'Canonical recovery directory must be empty')
            (canonical / 'release-manifest.json').write_bytes(raw)
            require(set(original.get('files', {})) == set(LOADERS), 'Invalid original loader manifest')
            for loader, info in original['files'].items():
                expected = f"{original['mod_id']}-{loader}-{original['minecraft']}-{original['version']}.jar"
                require(info['name'] == expected, 'Unsafe original artifact name')
            names = [f['name'] for f in original['files'].values()] + ['CHANGELOG.md', 'SHA256SUMS.txt']
            for name in names:
                require(name in self.assets, 'Original release bundle is incomplete; inspect draft assets')
                (canonical / name).write_bytes(self.github.read_asset(self.assets[name]))
            self.meta, self.bundle = verify_bundle(canonical), canonical
        states = sorted(name for name in self.assets if re.fullmatch(r'publish-state-\d{6}\.json', name))
        if states:
            require('release-manifest.json' in self.assets, 'Ledger without canonical manifest')
            require(states == [f'publish-state-{i:06}.json' for i in range(len(states))], 'Incomplete publication ledger')
            self.state = json.loads(self.github.read_asset(self.assets[states[-1]]))
            require(self.state.get('schema') == 1 and self.state.get('projects') == self.projects, 'Publishing destination/ledger conflict')
            require(self.state.get('manifest_sha256') == digest(json_bytes(self.meta)), 'Ledger does not describe canonical manifest')
            require(set(self.state.get('entries', {})) <= {p + ':' + l for p, l in TARGETS}, 'Unexpected ledger target')
            for key, entry in self.state['entries'].items():
                require(entry.get('status') in ('intent', 'confirmed'), 'Invalid ledger state')
                if entry['status'] == 'confirmed':
                    platform, loader = key.split(':')
                    receipt = entry.get('receipt', {})
                    require(receipt.get('id') and receipt.get('project_id') == self.projects[platform]
                            and receipt.get('sha512') == self.meta['files'][loader]['sha512'], 'Invalid ledger receipt')
            self.sequence = len(states)
        if not self.release['draft']:
            require(states and self.complete(), 'Published release is incomplete; inspect manually')
            print('Release already complete; no writes performed.')
            return True
        return False

    def complete(self):
        return all(self.state['entries'].get(p + ':' + l, {}).get('status') == 'confirmed' for p, l in TARGETS)

    def save(self):
        self.state['manifest_sha256'] = digest(json_bytes(self.meta))
        data = json_bytes(self.state)
        name = f'publish-state-{self.sequence:06}.json'
        # New assets, never replacement/deletion: a crash cannot erase the last durable state.
        self.github.add_asset(self.release['id'], name, data)
        self.sequence += 1
        backup = self.bundle.parent / 'receipts'
        backup.mkdir(exist_ok=True)
        (backup / name).write_bytes(data)

    def stage(self):
        if self.release is None:
            self.release = self.github.create(self.meta, self.body)
        names = [f['name'] for f in self.meta['files'].values()] + ['CHANGELOG.md', 'SHA256SUMS.txt', 'release-manifest.json']
        for name in names:
            data = (self.bundle / name).read_bytes()
            if name in self.assets:
                require(self.github.read_asset(self.assets[name]) == data, 'Incomplete draft asset conflict; inspect before retrying')
            else:
                self.github.add_asset(self.release['id'], name, data)
        if self.sequence == 0:
            self.save()

    def publish(self):
        if self.restore():
            return
        for adapter in self.platforms.values():
            adapter.preflight(self.meta)
        # Detect existing Modrinth conflicts before creating a draft or uploading to CurseForge.
        for loader in LOADERS:
            self.platforms['modrinth'].find(self.meta, loader, self.body)
        self.github.preflight(self.meta, allow_missing_tag=True)
        self.github.ensure_tag(self.meta)
        self.stage()
        for platform, loader in TARGETS:
            self.github.preflight(self.meta)  # Ref movement must stop subsequent writes.
            key, adapter = platform + ':' + loader, self.platforms[platform]
            entry = self.state['entries'].get(key)
            receipt = None
            if entry and entry['status'] == 'confirmed':
                recorded = entry['receipt']
                require(recorded['project_id'] == self.projects[platform] and recorded['sha512'] == self.meta['files'][loader]['sha512'], 'Receipt project/hash mismatch')
                if platform == 'modrinth':
                    adapter.find(self.meta, loader, self.body, recorded)
                # CurseForge receipt records API acceptance, not moderation status.
                continue
            if platform == 'modrinth':
                receipt = adapter.find(self.meta, loader, self.body)
                if entry and receipt is None:
                    evidence = self.evidence.get(key, {})
                    require(evidence.get('verified_absent') is True and evidence.get('project_verified') is True
                            and evidence.get('project_id') == self.projects[platform],
                            'Ambiguous Modrinth upload not visible yet; inspect the project before supplying verified_absent recovery')
            elif entry:
                evidence = self.evidence.get(key)
                require(evidence is not None, f'Ambiguous {key} upload. Inspect the project and supply recovery evidence; do not blindly retry.')
                if evidence.get('verified_absent') is True:
                    require(evidence.get('project_id') == self.projects[platform] and evidence.get('project_verified') is True,
                            'Retry requires maintainer verification that no upload exists on this project')
                else:
                    receipt = adapter.reconcile(evidence, self.meta, loader)
            if receipt is None:
                self.state['entries'][key] = {'status': 'intent'}
                self.save()
                receipt = adapter.publish(self.meta, loader, self.body, (self.bundle / self.meta['files'][loader]['name']).read_bytes())
            self.state['entries'][key] = {'status': 'confirmed', 'receipt': receipt}
            self.save()
        require(self.complete(), 'Publication is incomplete')
        self.github.preflight(self.meta)
        self.github.finalize(self.release['id'], should_be_latest(self.meta, self.github.latest()))
        print('All four uploads accepted; GitHub release published. Check CurseForge moderation separately.')


def run(mode, root, bundle):
    meta = verify_bundle(bundle)
    current, body = metadata(root, meta['tag'])
    require(all(current[k] == meta[k] for k in CONTRACT_FIELDS), 'Bundle does not match checked-out release source')
    env = os.environ
    projects = project_ids(env, required=mode != 'preview')
    if mode == 'preview':
        adapters = {'curseforge': CurseForge(projects['curseforge'], ''), 'modrinth': Modrinth(projects['modrinth'], '')}
        preview = {p + ':' + l: adapters[p].payload(meta, l, body) for p, l in TARGETS}
        path = bundle.parent / 'publish-preview.json'
        path.write_bytes(json_bytes(preview))
        print('Dry-run: no network requests or publishing credentials used. Preview: ' + str(path))
        summary = env.get('GITHUB_STEP_SUMMARY')
        if summary:
            with open(summary, 'a', encoding='utf-8') as handle:
                handle.write(f"## Release preview: {meta['tag']}\n\nCommit: `{meta['commit']}`\n\nMinecraft: {meta['minecraft']}; channel: {meta['channel']}\n\n")
                for loader, info in meta['files'].items():
                    handle.write(f"- {loader}: `{info['name']}` — SHA-256 `{info['sha256']}`\n")
                handle.write('\nSee the release-preview artifact for payloads and notes.\n')
        return
    for name in ('GITHUB_TOKEN', 'CURSEFORGE_TOKEN', 'MODRINTH_TOKEN'):
        require(bool(env.get(name)), 'Missing repository secret/token: ' + name)
    if mode == 'publish':
        require(env.get('PUBLISH_ENABLED') == 'true', 'Live publishing disabled; PUBLISH_ENABLED must be true')
    require(env.get('GITHUB_REPOSITORY') == meta['repository'], 'Publishing is restricted to the configured repository')
    github = GitHub(meta['repository'], env['GITHUB_TOKEN'])
    github.preflight(meta, allow_missing_tag=True)
    adapters = {'curseforge': CurseForge(projects['curseforge'], env['CURSEFORGE_TOKEN']),
                'modrinth': Modrinth(projects['modrinth'], env['MODRINTH_TOKEN'])}
    if mode == 'preflight':
        for adapter in adapters.values():
            adapter.preflight(meta)
        print('Read-only preflight passed. Upload permissions/scopes are finally enforced by upload endpoints; CurseForge project access cannot be proven by its author read API.')
        return
    evidence = json.loads(env.get('RELEASE_RECOVERY') or '{}')
    Publisher(github, adapters, meta, bundle, body, projects, evidence).publish()
