import copy
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

from history_release import (HistoryError, REF, command, expected_remote, owned_output, prepare,
                             redact_short, redact_yaml, scan, verify_release, yaml_credentials)
from release_gate import digest, json_bytes


class HistorySafetyTests(unittest.TestCase):
    def test_short_redaction_preserves_usernames_and_environment_placeholders(self):
        blob = b'user: admin\npassword: admin\n<password>admin</password>\nsecret: ${JWT_SECRET}\n{"password":"admin", "username":"admin"}\n'
        redacted = redact_short(blob, {b'admin'})
        self.assertIn(b'user: admin', redacted)
        self.assertIn(b'password: REDACTED_CREDENTIAL', redacted)
        self.assertIn(b'<password>REDACTED_CREDENTIAL</password>', redacted)
        self.assertIn(b'${JWT_SECRET}', redacted)
        self.assertIn(b'{"password":"REDACTED_CREDENTIAL", "username":"admin"}', redacted)

    def test_yaml_redaction_uses_credential_paths_without_changing_unrelated_values(self):
        blob = b'spring:\n  datasource:\n    username: admin\n    password: admin\njwt:\n  secret: ${JWT_SECRET}\nother:\n  password: example\n'
        self.assertEqual([('mysql_password', b'admin')], list(yaml_credentials(blob)))
        cleaned = redact_yaml(blob)
        self.assertIn(b'username: admin', cleaned)
        self.assertIn(b'secret: ${JWT_SECRET}', cleaned)
        self.assertIn(b'  password: example', cleaned)

    def test_remote_ref_growth_or_concurrent_main_change_is_rejected(self):
        expected_remote({REF: 'old'}, 'old')
        for refs in ({REF: 'new'}, {REF: 'old', 'refs/tags/v1': 'tag'}, {REF: 'old', 'refs/pull/1/head': 'pr'}, {}):
            with self.assertRaises(HistoryError):
                expected_remote(refs, 'old')

    def test_output_cannot_target_source_root_or_an_unowned_directory(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder).resolve()
            self.assertEqual(root / '.local/history-release/run', owned_output(root, root / '.local/history-release/run'))
            for output in (root, root.parent / 'external', root / '.local/history-release', root / '.local/history-release/run/nested'):
                with self.assertRaises(HistoryError):
                    owned_output(root, output)

    def test_publication_requires_same_clean_tested_candidate_and_capacity_artifact(self):
        report = {'candidateCommit': 'candidate', 'sourceTreeSha256': 'a' * 64}
        tests = {'status': 'PASS', 'tests': 2, 'failures': 0, 'errors': 0, 'skipped': 0, 'suites': {'FixtureTest': 2}}
        files = [{'path': 'index.html', 'sha256': 'b' * 64}]
        required = {'policy': {'backendRequiredSuites': ['FixtureTest'], 'runtimeRequiredChecks': ['registration'],
                              'serviceImages': {'mysql': 'mysql@sha256:' + 'c' * 64},
                              'capacity': {'scope': 'LOCAL_SINGLE_JVM_BASELINE', 'concurrencyStages': [1, 5, 20],
                                           'minimumMessages': 100000, 'minimumGroups': 1000, 'minimumRequestsPerStage': 75,
                                           'p95MsMaximum': 1000, 'endpoints': ['search', 'history', 'unread', 'owned_groups', 'joined_groups']}},
                    'schemaVersions': [{'version': '001', 'checksum': 'd' * 64, 'state': 'APPLIED'}],
                    'dependencyLocks': {'pom.xml': 'e' * 64}, 'schemaManifestSha256': 'f' * 64, 'contractManifestSha256': '0' * 64,
                    'capacityToolSha256': '3' * 64}
        release = {'status': 'PASS', 'kind': 'release_candidate', 'worktreeClean': True, 'reproducibility': 'PASS',
                   'sourceCommit': 'candidate', 'sourceTreeSha256': 'a' * 64, 'backendTests': tests,
                   'frontendTests': tests, 'artifacts': {'backendJarSha256': '1' * 64, 'frontendFiles': files, 'frontendTreeSha256': digest(json_bytes(files))},
                   'runtime': {'status': 'PASS', 'checks': {'registration': True}},
                   'colorAudit': {'status': 'PASS', 'files': 1, 'findings': []},
                   'schemaVersions': required['schemaVersions'], 'dependencyLocks': required['dependencyLocks'],
                   'schemaManifestSha256': required['schemaManifestSha256'], 'contractManifestSha256': required['contractManifestSha256'],
                   'backendResolvedDependencies': {'library.jar': '2' * 64},
                   'serviceImages': {'mysql': {'repoDigests': [required['policy']['serviceImages']['mysql']]}}}
        names = required['policy']['capacity']['endpoints']
        dataset = {'messageCount': 100000, 'groupCount': 1000, 'dataFingerprint': ['fixture'], 'generatorSha256': '4' * 64}
        summary = {'requests': 75, 'errors': 0, 'contractErrors': 0, 'privacyErrors': 0, 'p95Ms': 100}
        capacity = {'status': 'PASS', 'backendJarSha256': '1' * 64, 'scope': 'LOCAL_SINGLE_JVM_BASELINE',
                    'toolSha256': '3' * 64, 'dataset': dataset, 'datasetAfter': dataset, 'configuration': {'paging': True},
                    'localTarget': {'p95MsMaximum': 1000}, 'searchQueryProfile': {'status': 'RECORDED'},
                    'responsePrivacyChecks': {'publicContainsNoFixturePasswordsOrBearerTokens': True,
                                             'sensitiveResponseFieldErrors': 0, 'checkedMeasuredResponses': 225},
                    'stages': [{'concurrency': level, 'summary': summary, 'localTargetMet': True,
                                'resources': {'status': 'RECORDED'},
                                'endpoints': {name: {**summary, 'requests': 15} for name in names},
                                'samples': [{'success': True, 'contractValid': True, 'privacyValid': True} for _ in range(75)]}
                               for level in [1, 5, 20]]}
        verify_release(report, release, capacity, required)
        for key, value in [('worktreeClean', False), ('kind', 'review_snapshot'), ('sourceCommit', 'other'),
                           ('sourceTreeSha256', 'other'), ('reproducibility', 'FAIL')]:
            altered = copy.deepcopy(release); altered[key] = value
            with self.assertRaises(HistoryError):
                verify_release(report, altered, capacity, required)
        altered = copy.deepcopy(release); altered['backendTests']['skipped'] = 1
        with self.assertRaises(HistoryError):
            verify_release(report, altered, capacity, required)
        for missing in ('artifacts', 'runtime', 'schemaVersions', 'colorAudit', 'backendResolvedDependencies', 'serviceImages'):
            altered = copy.deepcopy(release); altered.pop(missing)
            with self.assertRaises(HistoryError):
                verify_release(report, altered, capacity, required)
        for altered in ({'status': 'FAIL', 'backendJarSha256': '1' * 64}, {'status': 'PASS', 'backendJarSha256': 'old-jar'}, {'status': 'PASS'}):
            with self.assertRaises(HistoryError):
                verify_release(report, release, altered, required)
        for missing in ('stages', 'dataset', 'searchQueryProfile', 'responsePrivacyChecks'):
            altered = copy.deepcopy(capacity); altered.pop(missing)
            with self.assertRaises(HistoryError):
                verify_release(report, release, altered, required)
        for value in (1001, float('nan')):
            altered = copy.deepcopy(capacity); altered['stages'][2]['endpoints']['search']['p95Ms'] = value
            with self.assertRaises(HistoryError):
                verify_release(report, release, altered, required)


class CompleteHistoryPreparationTests(unittest.TestCase):
    def test_real_filter_preserves_dirty_source_stash_index_and_every_other_historical_file(self):
        project = Path(__file__).resolve().parents[1]
        filter_python = project / '.local/verification/history-tools/Scripts/python.exe'
        if not filter_python.is_file():
            self.skipTest('Dedicated git-filter-repo interpreter is not configured')
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder) / 'source'; root.mkdir()
            remote = Path(folder) / 'remote.git'
            command(root, 'init', '-b', 'main')
            command(root, 'config', 'user.name', 'History fixture')
            command(root, 'config', 'user.email', 'fixture@example.invalid')
            command(root, 'config', 'core.autocrlf', 'false')
            config = root / 'src/main/resources/application.yml'; config.parent.mkdir(parents=True)
            config.write_text('spring:\n  datasource:\n    username: admin\n    password: admin\njwt:\n  secret: fixture-history-token-123456789\n')
            keys = root / 'src/main/resources/es'; keys.mkdir()
            (keys / 'http.p12').write_bytes(b'fixture-keystore-binary')
            (root / 'logic.txt').write_text('retained original function\n')
            (root / 'old-config.json').write_text('{"password":"admin","username":"admin"}\n')
            (root / '.gitignore').write_text('/.local/\n')
            command(root, 'add', '--all'); command(root, 'commit', '-m', 'original')
            config.write_text('spring:\n  datasource:\n    password: ${MYSQL_PASSWORD}\njwt:\n  secret: ${JWT_SECRET}\n')
            (root / 'old-config.json').write_text('{"username":"admin"}\n')
            (keys / 'http.p12').unlink()
            command(root, 'add', '--all'); command(root, 'commit', '-m', 'environment config')
            command(root, 'clone', '--bare', root, remote)
            command(root, 'remote', 'add', 'origin', remote)
            (root / 'logic.txt').write_text('private stash content\n')
            command(root, 'stash', 'push', '-m', 'local-only fixture stash')
            (root / 'logic.txt').write_text('latest fixed function\n')
            command(root, 'add', 'logic.txt')
            (root / 'new-feature.txt').write_text('untracked feature preserved\n')
            refs = command(root, 'for-each-ref', '--format=%(refname) %(objectname)')
            index = (root / '.git/index').read_bytes()
            output = root / '.local/history-release/test'
            prepare(root.resolve(), output, filter_python)
            report = json.loads((output / 'public/history-manifest.json').read_text())
            self.assertEqual(2, report['verifiedCommitTrees'])
            self.assertFalse(report['published'])
            self.assertTrue(report['completeCurrentSourcePreserved'])
            self.assertEqual(refs, command(root, 'for-each-ref', '--format=%(refname) %(objectname)'))
            self.assertEqual(index, (root / '.git/index').read_bytes())
            self.assertEqual(b'latest fixed function\n', command(output / 'candidate.git', 'show', 'main:logic.txt'))
            self.assertEqual(b'untracked feature preserved\n', command(output / 'candidate.git', 'show', 'main:new-feature.txt'))
            self.assertEqual([REF], command(output / 'candidate.git', 'for-each-ref', '--format=%(refname)').decode().splitlines())
            self.assertEqual(b'', command(output / 'candidate', 'status', '--porcelain'))
            self.assertEqual('refs/heads/main', report['ref'])
            scan(output / 'candidate.git', {b'fixture-history-token-123456789'}, {b'admin'})


if __name__ == '__main__':
    unittest.main()
