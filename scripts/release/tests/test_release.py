import copy
import io
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release
import platforms
import publisher

REPO = Path(__file__).resolve().parents[3]


class Fixture(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.stdout = patch('sys.stdout', new_callable=io.StringIO)
        self.stdout.start()
        self.addCleanup(self.stdout.stop)
        self.root = Path(self.temp.name)
        (self.root / 'release').mkdir()
        shutil.copyfile(REPO / 'release/publishing.json', self.root / 'release/publishing.json')
        (self.root / 'gradle.properties').write_text('version=4.0.0-rc.1\nminecraft_version=26.1\njava_version=25\nmod_id=heavyinventories\n')
        (self.root / 'changelogs').mkdir()
        self.body = '# 4.0.0-rc.1\n\n## Added\n\n- Inventory weight. Cloth Config is required on NeoForge clients.\n'
        self.notes = self.root / 'changelogs/4.0.0-rc.1.md'
        self.notes.write_text(self.body)
        self.meta, _ = release.metadata(self.root, commit='a' * 40)
        self.paths = {}
        for loader in release.LOADERS:
            path = self.root / loader / 'build/libs' / f'heavyinventories-{loader}-26.1-4.0.0-rc.1.jar'
            path.parent.mkdir(parents=True)
            with zipfile.ZipFile(path, 'w') as jar:
                if loader == 'fabric':
                    jar.writestr('fabric.mod.json', json.dumps({'id': 'heavyinventories', 'version': '4.0.0-rc.1'}))
                else:
                    jar.writestr('META-INF/neoforge.mods.toml', '[[mods]]\nmodId="heavyinventories"\nversion="4.0.0-rc.1"\n')
            self.paths[loader] = path.relative_to(self.root).as_posix()
        manifest = self.root / 'build/release/artifacts.json'
        manifest.parent.mkdir(parents=True)
        manifest.write_bytes(release.json_bytes(self.paths))
        self.bundle = self.root / 'build/release/bundle'
        with patch.object(release, 'git', return_value='a' * 40):
            self.meta = release.package(self.root, 'v4.0.0-rc.1', self.bundle)


class ValidationTests(Fixture):
    def test_supported_channels(self):
        for value, result in [('4.0.0', 'release'), ('4.0.0-rc.1', 'beta'), ('4.0.0-beta.2', 'beta'),
                              ('4.0.0-alpha.3', 'alpha'), ('26.1.0.0-rc.1', 'beta')]:
            self.assertEqual(release.channel(value), result)

    def test_unsupported_versions(self):
        for value in ('v4.0.0', '4.0', '4.0.0-snapshot', '4.0.0-rc.0', '4.0.0+fabric', '../4.0.0', '04.0.0', '4.0.0\n'):
            with self.subTest(value=value), self.assertRaises(release.ReleaseError):
                release.channel(value)

    def test_missing_notes(self):
        self.notes.unlink()
        with self.assertRaisesRegex(release.ReleaseError, 'Missing version'):
            release.metadata(self.root, commit='a' * 40)

    def test_bad_changelog(self):
        for body in ('', '# wrong\n- A change', '# 4.0.0-rc.1\n', self.body + '\n- TODO\n',
                     self.body + '\n## Empty\n', self.body + '\n[Guide](../README.md)\n',
                     self.body + '<!-- draft -->', self.body + '\nC:\\private\\notes',
                     self.body.replace('Cloth Config', 'Dependency')):
            with self.subTest(body=body), self.assertRaises((release.ReleaseError, IndexError)):
                self.notes.write_text(body)
                release.metadata(self.root, commit='a' * 40)

    def test_tag_must_match(self):
        with self.assertRaisesRegex(release.ReleaseError, 'Tag does not match'):
            release.metadata(self.root, 'v4.0.0', 'a' * 40)
        release.metadata(self.root, '', 'a' * 40)  # ordinary CI has no tag

    def test_exact_files_and_hashes(self):
        self.assertEqual(release.verify_bundle(self.bundle)['files'], self.meta['files'])
        (self.bundle / self.meta['files']['fabric']['name']).write_bytes(b'corrupt')
        with self.assertRaisesRegex(release.ReleaseError, 'hash mismatch'):
            release.verify_bundle(self.bundle)

    def test_extra_bundle_file(self):
        (self.bundle / 'sources.jar').write_bytes(b'extra')
        with self.assertRaisesRegex(release.ReleaseError, 'Unexpected file'):
            release.verify_bundle(self.bundle)

    def test_path_escape_and_extra_selection(self):
        for paths in ({**self.paths, 'common': 'common.jar'}, {**self.paths, 'fabric': '../evil.jar'}):
            (self.root / 'build/release/artifacts.json').write_bytes(release.json_bytes(paths))
            with patch.object(release, 'git', return_value='a' * 40), self.assertRaises(release.ReleaseError):
                release.package(self.root, 'v4.0.0-rc.1', self.root / ('out-' + str(len(paths))))

    def test_wrong_jar_version(self):
        with zipfile.ZipFile(self.root / self.paths['fabric'], 'w') as jar:
            jar.writestr('fabric.mod.json', '{"id":"heavyinventories","version":"old"}')
        with patch.object(release, 'git', return_value='a' * 40), self.assertRaisesRegex(release.ReleaseError, 'identity'):
            release.package(self.root, 'v4.0.0-rc.1', self.root / 'new-bundle')

    def test_preview_has_no_network_or_credentials(self):
        with patch.dict(os.environ, {}, clear=True), patch.object(release, 'git', return_value='a' * 40), \
                patch.object(platforms.Http, 'request', side_effect=AssertionError('Network used')):
            publisher.run('preview', self.root, self.bundle)
        payloads = release.read_json(self.bundle.parent / 'publish-preview.json')
        self.assertEqual(len(payloads), 4)
        self.assertEqual(payloads['modrinth:fabric']['version_number'], '4.0.0-rc.1+fabric')
        self.assertEqual(payloads['modrinth:neoforge']['loaders'], ['neoforge'])
        self.assertEqual(len(payloads['curseforge:neoforge']['relations']['projects']), 1)

    def test_disabled_publish_fails_before_network(self):
        env = {'CURSEFORGE_PROJECT_ID': '123', 'MODRINTH_PROJECT_ID': 'Abcd1234', 'GITHUB_TOKEN': 'secret',
               'MODRINTH_TOKEN': 'secret', 'CURSEFORGE_TOKEN': 'secret', 'PUBLISH_ENABLED': 'false'}
        with patch.dict(os.environ, env, clear=True), patch.object(release, 'git', return_value='a' * 40), \
                patch.object(platforms.Http, 'request', side_effect=AssertionError('Network used')), \
                self.assertRaisesRegex(release.ReleaseError, 'disabled'):
            publisher.run('publish', self.root, self.bundle)

    def test_latest_policy(self):
        self.assertFalse(publisher.should_be_latest(self.meta, None))
        stable = {**self.meta, 'version': '4.10.0', 'channel': 'release'}
        self.assertTrue(publisher.should_be_latest(stable, {'tag_name': 'v4.9.0'}))
        self.assertFalse(publisher.should_be_latest(stable, {'tag_name': 'v4.11.0'}))
        self.assertFalse(publisher.should_be_latest({**stable, 'minecraft': '1.21'}, None))
        self.assertFalse(publisher.should_be_latest(stable, {'tag_name': 'unknown'}))

    def test_resolve_rejects_unmerged_tag(self):
        from unittest.mock import Mock
        with patch.object(release, 'git', return_value='a' * 40), \
                patch.object(release.subprocess, 'run', return_value=Mock(returncode=1)), \
                self.assertRaisesRegex(release.ReleaseError, 'merged'):
            release.resolve(self.root, 'v4.0.0-rc.1')

    def test_invalid_destination_ids(self):
        for env in ({}, {'CURSEFORGE_PROJECT_ID': 'abc', 'MODRINTH_PROJECT_ID': 'Abcd1234'},
                    {'CURSEFORGE_PROJECT_ID': '123', 'MODRINTH_PROJECT_ID': '../other'}):
            with self.subTest(env=env), self.assertRaises(release.ReleaseError):
                publisher.project_ids(env)


class FakeGitHub:
    def __init__(self):
        self.value, self.files, self.writes = None, {}, 0
        self.fail_asset, self.fail_after_save = None, False
        self.fail_finalize = False

    def preflight(self, meta):
        pass

    def release(self, tag):
        return copy.deepcopy(self.value)

    def create(self, meta, body):
        self.writes += 1
        self.value = {'id': 1, 'tag_name': meta['tag'], 'body': body, 'draft': True, 'prerelease': meta['channel'] != 'release'}
        return copy.deepcopy(self.value)

    def assets(self, release_id):
        return [{'id': name, 'name': name} for name in self.files]

    def read_asset(self, asset):
        return self.files[asset['id']]

    def add_asset(self, release_id, name, data):
        self.writes += 1
        if self.fail_asset == name and not self.fail_after_save:
            self.fail_asset = None
            raise release.ReleaseError('simulated ledger failure')
        if name in self.files:
            raise release.ReleaseError('duplicate asset')
        self.files[name] = data
        if self.fail_asset == name:
            self.fail_asset = None
            raise release.ReleaseError('simulated lost asset response')

    def latest(self):
        return None

    def finalize(self, release_id, latest):
        self.writes += 1
        self.value['draft'] = False
        if self.fail_finalize:
            self.fail_finalize = False
            raise release.ReleaseError('lost finalization response')


class FakePlatform:
    def __init__(self, project):
        self.project, self.calls, self.remote = project, [], {}
        self.fail_loader, self.preflight_error = None, False

    def preflight(self, meta):
        if self.preflight_error:
            raise release.ReleaseError('preflight failed')

    def find(self, meta, loader, body, receipt=None):
        return self.remote.get(loader)

    def publish(self, meta, loader, body, data):
        self.calls.append(loader)
        receipt = {'id': str(len(self.calls)), 'sha512': release.digest(data, 'sha512'), 'project_id': self.project}
        self.remote[loader] = receipt
        if self.fail_loader == loader:
            self.fail_loader = None
            raise release.ReleaseError('lost upload response')
        return receipt

    def reconcile(self, evidence, meta, loader):
        return self.remote[loader]


class RecoveryTests(Fixture):
    def setUp(self):
        super().setUp()
        self.gh = FakeGitHub()
        self.projects = {'curseforge': '123', 'modrinth': 'Abcd1234'}
        self.adapters = {key: FakePlatform(value) for key, value in self.projects.items()}
        self.attempt = 0

    def attempt_publish(self, evidence=None):
        # Each Actions retry starts on a fresh runner with its freshly built input bundle.
        self.attempt += 1
        bundle = self.root / f'attempt{self.attempt}' / 'bundle'
        shutil.copytree(self.bundle, bundle)
        job = publisher.Publisher(self.gh, self.adapters, self.meta, bundle, self.body, self.projects, evidence)
        job.publish()
        return job

    def test_success_and_repeat_is_noop(self):
        self.attempt_publish()
        self.assertFalse(self.gh.value['draft'])
        self.assertEqual([len(p.calls) for p in self.adapters.values()], [2, 2])
        writes = self.gh.writes
        self.attempt_publish()
        self.assertEqual(self.gh.writes, writes)

    def test_preflight_failure_has_no_writes(self):
        self.adapters['modrinth'].preflight_error = True
        with self.assertRaises(release.ReleaseError):
            self.attempt_publish()
        self.assertEqual(self.gh.writes, 0)
        self.assertEqual(self.adapters['curseforge'].calls, [])

    def test_every_lost_external_response(self):
        for platform, loader in publisher.TARGETS:
            with self.subTest(platform=platform, loader=loader):
                self.gh = FakeGitHub()
                self.adapters = {key: FakePlatform(value) for key, value in self.projects.items()}
                self.adapters[platform].fail_loader = loader
                with self.assertRaises(release.ReleaseError):
                    self.attempt_publish()
                if platform == 'curseforge':
                    with self.assertRaisesRegex(release.ReleaseError, 'Ambiguous'):
                        self.attempt_publish()
                    self.attempt_publish({platform + ':' + loader: {'id': 1}})
                else:
                    self.attempt_publish()
                self.assertEqual([len(p.calls) for p in self.adapters.values()], [2, 2])

    def test_every_lost_ledger_response(self):
        for position in range(9):  # initial state plus intent/receipt for each of four targets
            with self.subTest(position=position):
                self.gh = FakeGitHub()
                self.adapters = {key: FakePlatform(value) for key, value in self.projects.items()}
                self.gh.fail_asset = f'publish-state-{position:06}.json'
                self.gh.fail_after_save = True
                with self.assertRaises(release.ReleaseError):
                    self.attempt_publish()
                evidence = {p + ':' + l: {'project_id': self.projects[p], 'project_verified': True, 'verified_absent': True}
                            for p, l in publisher.TARGETS if l not in self.adapters[p].remote}
                self.attempt_publish(evidence)
                self.assertEqual([len(p.calls) for p in self.adapters.values()], [2, 2])

    def test_every_failed_ledger_write(self):
        for position in range(9):
            with self.subTest(position=position):
                self.gh = FakeGitHub()
                self.adapters = {key: FakePlatform(value) for key, value in self.projects.items()}
                self.gh.fail_asset = f'publish-state-{position:06}.json'
                with self.assertRaises(release.ReleaseError):
                    self.attempt_publish()
                evidence = {p + ':' + l: {'id': 1} for p, l in publisher.TARGETS if p == 'curseforge' and l in self.adapters[p].remote}
                self.attempt_publish(evidence)
                self.assertEqual([len(p.calls) for p in self.adapters.values()], [2, 2])

    def test_every_staging_asset_failure(self):
        names = [f['name'] for f in self.meta['files'].values()] + ['CHANGELOG.md', 'SHA256SUMS.txt', 'release-manifest.json']
        for name in names:
            for saved in (True, False):
                with self.subTest(name=name, saved=saved):
                    self.gh = FakeGitHub()
                    self.adapters = {key: FakePlatform(value) for key, value in self.projects.items()}
                    self.gh.fail_asset, self.gh.fail_after_save = name, saved
                    with self.assertRaises(release.ReleaseError):
                        self.attempt_publish()
                    self.assertEqual([len(p.calls) for p in self.adapters.values()], [0, 0])
                    self.attempt_publish()
                    self.assertEqual([len(p.calls) for p in self.adapters.values()], [2, 2])

    def test_corrupt_canonical_asset_blocks_recovery(self):
        self.adapters['curseforge'].fail_loader = 'fabric'
        with self.assertRaises(release.ReleaseError):
            self.attempt_publish()
        self.gh.files[self.meta['files']['fabric']['name']] = b'corrupt'
        with self.assertRaisesRegex(release.ReleaseError, 'hash mismatch'):
            self.attempt_publish({'curseforge:fabric': {'id': 1}})
        self.assertEqual(self.adapters['curseforge'].calls, ['fabric'])

    def test_modrinth_absent_intent_needs_explicit_recovery(self):
        self.gh.fail_asset, self.gh.fail_after_save = 'publish-state-000005.json', True
        with self.assertRaises(release.ReleaseError):
            self.attempt_publish()
        with self.assertRaisesRegex(release.ReleaseError, 'Ambiguous Modrinth'):
            self.attempt_publish()
        self.assertEqual(self.adapters['modrinth'].calls, [])
        self.attempt_publish({'modrinth:fabric': {'project_id': self.projects['modrinth'], 'project_verified': True, 'verified_absent': True}})
        self.assertEqual(self.adapters['modrinth'].calls, ['fabric', 'neoforge'])

    def test_existing_modrinth_conflict_blocks_curseforge(self):
        with patch.object(self.adapters['modrinth'], 'find', side_effect=release.ReleaseError('conflict')):
            with self.assertRaisesRegex(release.ReleaseError, 'conflict'):
                self.attempt_publish()
        self.assertEqual(self.gh.writes, 0)
        self.assertEqual(self.adapters['curseforge'].calls, [])

    def test_receipt_write_failure_stops_and_preserves_intent(self):
        self.gh.fail_asset = 'publish-state-000002.json'
        with self.assertRaises(release.ReleaseError):
            self.attempt_publish()
        with self.assertRaisesRegex(release.ReleaseError, 'Ambiguous curseforge:fabric'):
            self.attempt_publish()
        self.assertEqual(self.adapters['curseforge'].calls, ['fabric'])
        self.attempt_publish({'curseforge:fabric': {'id': 1}})
        self.assertEqual(self.adapters['curseforge'].calls, ['fabric', 'neoforge'])

    def test_original_bytes_used_after_rebuild(self):
        self.adapters['curseforge'].fail_loader = 'fabric'
        with self.assertRaises(release.ReleaseError):
            self.attempt_publish()
        original = copy.deepcopy(self.meta['files'])
        # Simulate a byte-different rebuild at the same source revision.
        info = self.meta['files']['neoforge']
        path = self.bundle / info['name']
        with zipfile.ZipFile(path, 'a') as jar:
            jar.writestr('build-timestamp', 'different')
        data = path.read_bytes()
        info.update(size=len(data), sha256=release.digest(data), sha512=release.digest(data, 'sha512'))
        (self.bundle / 'release-manifest.json').write_bytes(release.json_bytes(self.meta))
        (self.bundle / 'SHA256SUMS.txt').write_text(release.checksums(self.meta))
        job = self.attempt_publish({'curseforge:fabric': {'id': 1}})
        self.assertEqual(job.meta['files'], original)
        self.assertEqual(self.adapters['curseforge'].remote['neoforge']['sha512'], original['neoforge']['sha512'])

    def test_conflicting_source_rejected(self):
        self.gh.fail_finalize = True
        with self.assertRaises(release.ReleaseError):
            self.attempt_publish()
        self.meta['commit'] = 'b' * 40
        with self.assertRaisesRegex(release.ReleaseError, 'manifest/source conflict'):
            self.attempt_publish()

    def test_lost_finalization_response_is_noop(self):
        self.gh.fail_finalize = True
        with self.assertRaises(release.ReleaseError):
            self.attempt_publish()
        writes = self.gh.writes
        self.attempt_publish()
        self.assertEqual(writes, self.gh.writes)


class TransportTests(unittest.TestCase):
    def test_write_not_retried_and_error_sanitized(self):
        from unittest.mock import Mock
        opener = Mock()
        opener.open.side_effect = urllib.error.HTTPError('https://example.invalid?secret', 503, 'secret', {}, io.BytesIO(b'secret'))
        api = platforms.Http('https://example.invalid', 'super-secret', opener=opener, sleep=lambda _: None)
        with self.assertRaises(release.ReleaseError) as error:
            api.request('POST', '/upload', value={'secret': 'super-secret'})
        self.assertEqual(opener.open.call_count, 1)
        self.assertNotIn('secret', str(error.exception))

    def test_get_retries_are_bounded(self):
        from unittest.mock import Mock
        opener = Mock()
        opener.open.side_effect = urllib.error.HTTPError('https://example.invalid', 429, '', {'Retry-After': '3600'}, None)
        sleeps = []
        api = platforms.Http('https://example.invalid', opener=opener, sleep=sleeps.append)
        with self.assertRaises(release.ReleaseError):
            api.get('/test')
        self.assertEqual(opener.open.call_count, 3)
        self.assertEqual(sleeps, [30, 30])

    def test_multipart_keeps_changelog_literal(self):
        body, content_type = platforms.multipart('metadata', {'changelog': '`$(danger)`\nSecond line'}, 'mod.jar', b'jar-bytes')
        self.assertIn(b'`$(danger)`\\nSecond line', body)
        self.assertIn(b'jar-bytes', body)
        self.assertIn('boundary=', content_type)

    def test_modrinth_conflicts_rejected(self):
        adapter = platforms.Modrinth('Abcd1234', '')
        meta = {'version': '4.0.0', 'channel': 'release', 'minecraft': '26.1', 'dependencies': {'fabric': []},
                'files': {'fabric': {'sha512': 'abc', 'name': 'file.jar'}}}
        value = adapter.payload(meta, 'fabric', 'body')
        value.update(id='xyz', files=[{'hashes': {'sha512': 'abc'}, 'filename': 'file.jar'}])
        adapter.verify(value, meta, 'fabric', 'body')
        value['files'][0]['hashes']['sha512'] = 'different'
        with self.assertRaisesRegex(release.ReleaseError, 'hash/name conflict'):
            adapter.verify(value, meta, 'fabric', 'body')

    def test_curseforge_reconciliation_checks_host_path_and_hash(self):
        adapter = platforms.CurseForge('123', '')
        data = b'jar'
        meta = {'files': {'fabric': {'name': 'mod.jar', 'sha512': release.digest(data, 'sha512')}}}
        evidence = {'id': 1234567, 'project_id': '123', 'project_verified': True,
                    'download_url': 'https://mediafilez.forgecdn.net/files/1234/567/mod.jar'}
        with patch.object(platforms, 'download', return_value=data):
            self.assertEqual(adapter.reconcile(evidence, meta, 'fabric')['id'], 1234567)
        with self.assertRaises(release.ReleaseError):
            adapter.reconcile({**evidence, 'project_id': '999'}, meta, 'fabric')
        with self.assertRaises(release.ReleaseError):
            adapter.reconcile({**evidence, 'download_url': 'https://evil.example/files/1234/567/mod.jar'}, meta, 'fabric')
        with patch.object(platforms, 'download', return_value=b'wrong'), self.assertRaises(release.ReleaseError):
            adapter.reconcile(evidence, meta, 'fabric')


if __name__ == '__main__':
    unittest.main()
