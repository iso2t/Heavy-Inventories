"""Small API adapters. Never retry a write whose outcome may be ambiguous."""
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

from release import ReleaseError, digest, json_bytes, require

USER_AGENT = 'iso2t/Heavy-Inventories-publisher (https://github.com/iso2t/Heavy-Inventories)'


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Http:
    def __init__(self, base, token=None, token_header='Authorization', opener=None, sleep=time.sleep):
        self.base = base.rstrip('/')
        self.token = token
        self.token_header = token_header
        self.opener = opener or urllib.request.build_opener(NoRedirect)
        self.sleep = sleep

    def request(self, method, path, value=None, data=None, content_type=None, binary=False, missing=False):
        url = self.base + path
        headers = {'User-Agent': USER_AGENT, 'Accept': 'application/octet-stream' if binary else 'application/json'}
        if self.token:
            headers[self.token_header] = self.token
        if value is not None:
            data, content_type = json_bytes(value), 'application/json'
        if content_type:
            headers['Content-Type'] = content_type
        for attempt in range(3 if method == 'GET' else 1):
            request = urllib.request.Request(url, data=data, headers=headers, method=method)
            try:
                with self.opener.open(request, timeout=60) as response:
                    body = response.read()
                    return body if binary else json.loads(body) if body else None
            except urllib.error.HTTPError as error:
                if error.code == 404 and missing:
                    return None
                if method == 'GET' and error.code in (301, 302, 303, 307, 308) and binary:
                    # Signed GitHub asset redirects must not carry the API token.
                    return download(error.headers.get('Location', ''), ('release-assets.githubusercontent.com', 'objects.githubusercontent.com'))
                if method == 'GET' and error.code in (429, 500, 502, 503, 504) and attempt < 2:
                    retry = error.headers.get('Retry-After', '')
                    self.sleep(min(int(retry), 30) if retry.isdigit() else 2 ** attempt)
                    continue
                raise ReleaseError(f'{urllib.parse.urlparse(self.base).hostname}: {method} failed (HTTP {error.code}); inspect permissions/status before retrying') from None
            except (urllib.error.URLError, TimeoutError, OSError):
                if method == 'GET' and attempt < 2:
                    self.sleep(2 ** attempt)
                    continue
                raise ReleaseError(f'{urllib.parse.urlparse(self.base).hostname}: {method} connection failed; a write may already have succeeded') from None
            except (ValueError, UnicodeError):
                raise ReleaseError('Invalid API response; a write may already have succeeded') from None

    def get(self, path, **kwargs):
        return self.request('GET', path, **kwargs)


def download(url, allowed_hosts, max_bytes=100 * 1024 * 1024):
    parsed = urllib.parse.urlparse(url)
    require(parsed.scheme == 'https' and parsed.hostname in allowed_hosts and not parsed.username and not parsed.password,
            'Download URL must use an approved HTTPS host')
    # No credentials and no redirects to arbitrary hosts.
    try:
        with urllib.request.build_opener(NoRedirect).open(urllib.request.Request(url, headers={'User-Agent': USER_AGENT}), timeout=60) as response:
            body = response.read(max_bytes + 1)
    except (urllib.error.URLError, TimeoutError, OSError):
        raise ReleaseError('Artifact download failed; no upload was retried') from None
    require(len(body) <= max_bytes, 'Artifact download exceeds size limit')
    return body


def multipart(field, metadata, filename, data):
    boundary = 'hi-' + uuid.uuid4().hex
    require('"' not in filename and '\n' not in filename and '\r' not in filename, 'Invalid upload filename')
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"\r\nContent-Type: application/json\r\n\r\n'.encode()
            + json_bytes(metadata)
            + f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: application/java-archive\r\n\r\n'.encode()
            + data + f'\r\n--{boundary}--\r\n'.encode())
    return body, 'multipart/form-data; boundary=' + boundary


class GitHub:
    def __init__(self, repository, token):
        self.repository = repository
        self.api = Http('https://api.github.com', 'Bearer ' + token)
        self.upload = Http('https://uploads.github.com', 'Bearer ' + token)
        self.path = '/repos/' + repository

    def tag_sha(self, tag):
        value = self.api.get(self.path + '/git/ref/tags/' + urllib.parse.quote(tag, safe=''))['object']
        for _ in range(5):
            if value['type'] == 'commit':
                return value['sha']
            require(value['type'] == 'tag', 'Release ref is not a commit/tag')
            value = self.api.get(self.path + '/git/tags/' + value['sha'])['object']
        raise ReleaseError('Too many nested annotated tags')

    def preflight(self, meta):
        repo = self.api.get(self.path)
        require(repo.get('permissions', {}).get('push'), 'GitHub token needs contents: write')
        require(self.tag_sha(meta['tag']) == meta['commit'], 'Remote release tag moved or differs from bundle')

    def release(self, tag):
        return self.api.get(self.path + '/releases/tags/' + urllib.parse.quote(tag, safe=''), missing=True)

    def create(self, meta, body):
        return self.api.request('POST', self.path + '/releases', value={
            'tag_name': meta['tag'], 'target_commitish': meta['commit'], 'name': 'Heavy Inventories ' + meta['version'],
            'body': body, 'draft': True, 'prerelease': meta['channel'] != 'release', 'make_latest': 'false',
        })

    def assets(self, release_id):
        result = []
        for page in range(1, 101):
            batch = self.api.get(f'{self.path}/releases/{release_id}/assets?per_page=100&page={page}')
            result.extend(batch)
            if len(batch) < 100:
                return result
        raise ReleaseError('Too many release assets')

    def read_asset(self, asset):
        return self.api.get(f"{self.path}/releases/assets/{asset['id']}", binary=True)

    def add_asset(self, release_id, name, data):
        return self.upload.request('POST', f'{self.path}/releases/{release_id}/assets?name=' + urllib.parse.quote(name, safe=''),
                                   data=data, content_type='application/octet-stream')

    def latest(self):
        return self.api.get(self.path + '/releases/latest', missing=True)

    def finalize(self, release_id, latest):
        return self.api.request('PATCH', f'{self.path}/releases/{release_id}', value={'draft': False, 'make_latest': 'true' if latest else 'false'})


class Modrinth:
    def __init__(self, project, token):
        self.project = project
        self.api = Http('https://api.modrinth.com/v2', token)

    def preflight(self, meta):
        project = self.api.get('/project/' + self.project)
        require(project['id'] == self.project and project['project_type'] == 'mod', 'Modrinth project ID/type mismatch')
        require(meta['minecraft'] in {g['version'] for g in self.api.get('/tag/game_version')}, 'Minecraft version missing on Modrinth')
        require(set(meta['files']) <= {g['name'] for g in self.api.get('/tag/loader')}, 'Loader missing on Modrinth')
        for deps in meta['dependencies'].values():
            for dep in deps:
                actual = self.api.get('/project/' + dep['modrinth'])
                require(actual['id'] == dep['modrinth'] and actual['slug'] == dep['slug'], 'Modrinth dependency identity changed')
        # Also proves private-version reads work where available. Upload scope is enforced by POST.
        self.api.get('/project/' + self.project + '/version')

    def payload(self, meta, loader, body):
        return {'name': f"Heavy Inventories {meta['version']} ({loader.title() if loader == 'fabric' else 'NeoForge'})",
                'version_number': meta['version'] + '+' + loader, 'project_id': self.project,
                'changelog': body, 'version_type': meta['channel'], 'game_versions': [meta['minecraft']],
                'loaders': [loader], 'featured': False, 'file_parts': ['file'], 'primary_file': 'file',
                'dependencies': [{'project_id': d['modrinth'], 'dependency_type': 'required'} for d in meta['dependencies'][loader]]}

    def verify(self, version, meta, loader, body):
        expected = self.payload(meta, loader, body)
        require(all(version.get(k) == expected[k] for k in ('project_id', 'version_number', 'version_type', 'game_versions', 'loaders', 'changelog')), 'Existing Modrinth version metadata conflict')
        actual_deps = {(d['project_id'], d['dependency_type']) for d in version['dependencies']}
        require(actual_deps == {(d['project_id'], d['dependency_type']) for d in expected['dependencies']}, 'Existing Modrinth dependency conflict')
        files = version['files']
        require(len(files) == 1 and files[0]['hashes']['sha512'] == meta['files'][loader]['sha512'] and files[0]['filename'] == meta['files'][loader]['name'], 'Existing Modrinth file hash/name conflict')
        return {'id': version['id'], 'sha512': meta['files'][loader]['sha512'], 'project_id': self.project}

    def find(self, meta, loader, body, receipt=None):
        if receipt:
            return self.verify(self.api.get('/version/' + receipt['id']), meta, loader, body)
        matches = [v for v in self.api.get('/project/' + self.project + '/version') if v['version_number'] == meta['version'] + '+' + loader]
        require(len(matches) <= 1, 'Multiple Modrinth versions match; inspect manually')
        return self.verify(matches[0], meta, loader, body) if matches else None

    def publish(self, meta, loader, body, data):
        payload, content_type = multipart('data', self.payload(meta, loader, body), meta['files'][loader]['name'], data)
        value = self.api.request('POST', '/version', data=payload, content_type=content_type)
        return self.verify(value, meta, loader, body)


class CurseForge:
    def __init__(self, project, token):
        self.project = project
        self.api = Http('https://minecraft.curseforge.com', token, 'X-Api-Token')
        self.versions = {}

    def preflight(self, meta):
        rows = self.api.get('/api/game/versions')
        for name in (meta['minecraft'], 'Fabric', 'NeoForge'):
            matches = [r['id'] for r in rows if r['name'] == name]
            require(len(matches) == 1, 'Missing/ambiguous CurseForge game or loader tag: ' + name)
            self.versions[name] = matches[0]
        # The author API has no documented project/permission or file lookup endpoint.
        # This validates metadata access, not project upload permission or dependency ownership.

    def payload(self, meta, loader, body):
        loader_name = 'Fabric' if loader == 'fabric' else 'NeoForge'
        value = {'displayName': f"Heavy Inventories {meta['version']} ({loader_name})", 'changelog': body,
                 'changelogType': 'markdown', 'releaseType': meta['channel'],
                 'relations': {'projects': [{'slug': d['slug'], 'projectID': d['curseforge'], 'type': 'requiredDependency'} for d in meta['dependencies'][loader]]}}
        if self.versions:
            value['gameVersions'] = [self.versions[meta['minecraft']], self.versions[loader_name]]
        else:
            value['gameVersionNames'] = [meta['minecraft'], loader_name]
        return value

    def publish(self, meta, loader, body, data):
        payload, content_type = multipart('metadata', self.payload(meta, loader, body), meta['files'][loader]['name'], data)
        value = self.api.request('POST', f'/api/projects/{self.project}/upload-file', data=payload, content_type=content_type)
        require(isinstance(value.get('id'), int) and value['id'] > 0, 'CurseForge returned no valid receipt; inspect the project before retrying')
        return {'id': value['id'], 'sha512': meta['files'][loader]['sha512'], 'project_id': self.project,
                'status': 'accepted; moderation/public availability not verified'}

    def reconcile(self, evidence, meta, loader):
        require(evidence.get('project_id') == self.project and evidence.get('project_verified') is True,
                'CurseForge reconciliation requires maintainer verification of the file on this project')
        file_id = evidence.get('id')
        require(isinstance(file_id, int) and file_id > 0, 'Invalid CurseForge file ID')
        url = evidence.get('download_url', '')
        path = urllib.parse.unquote(urllib.parse.urlparse(url).path)
        # ForgeCDN's file path carries the file ID in two numeric components.
        require(path == f"/files/{file_id // 1000}/{file_id % 1000}/{meta['files'][loader]['name']}", 'CurseForge CDN path does not match file ID/name')
        data = download(url, ('edge.forgecdn.net', 'mediafilez.forgecdn.net'))
        require(digest(data, 'sha512') == meta['files'][loader]['sha512'], 'Reconciled CurseForge file hash mismatch')
        return {'id': file_id, 'project_id': self.project, 'sha512': digest(data, 'sha512'),
                'status': 'reconciled from CDN; project and metadata attested by maintainer'}
