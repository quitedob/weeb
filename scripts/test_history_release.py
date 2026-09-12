import copy
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

from history_release import (HistoryError, REF, command, expected_remote, owned_output, prepare,
                             redact_short, redact_yaml, scan, verify_release, yaml_credentials)
from release_gate import digest, json_bytes
from capacity_probe import summarize


def publication_fixture():
    report = {'candidateCommit': 'candidate', 'sourceTreeSha256': 'a' * 64}
    tests = {'status': 'PASS', 'tests': 2, 'failures': 0, 'errors': 0, 'skipped': 0, 'suites': {'FixtureTest': 2}}
    files = [{'path': 'index.html', 'sha256': 'b' * 64}]
    required = {'policy': {'backendRequiredSuites': ['FixtureTest'], 'runtimeRequiredChecks': ['registration'],
                          'serviceImages': {'mysql': 'mysql@sha256:' + 'c' * 64, 'redis': 'redis@sha256:' + '9' * 64},
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
               'serviceImages': {name: {'repoDigests': [pinned], 'imageId': 'sha256:' + value * 64}
                             for (name, pinned), value in zip(required['policy']['serviceImages'].items(), ('5', '6'))}}
    names = required['policy']['capacity']['endpoints']
    dataset = {'messageCount': 100000, 'groupCount': 1000, 'dataFingerprint': ['fixture'], 'generatorSha256': '4' * 64}
    capacity = {'status': 'PASS', 'backendJarSha256': '1' * 64, 'scope': 'LOCAL_SINGLE_JVM_BASELINE',
                'toolSha256': '3' * 64, 'dataset': dataset, 'datasetAfter': dataset,
                'configuration': {'paging': True, 'pageSize': 10},
                'localTarget': {'p95MsMaximum': 1000},
                'serviceImages': {name: {'port': port, 'imageId': release['serviceImages'][name]['imageId']}
                                  for name, port in [('mysql', 23306), ('redis', 16379)]},
                'searchQueryProfile': {'status': 'RECORDED', 'harnessSha256': '7' * 64, 'queries': [
                    {'sql': 'SELECT COUNT(*) FROM message WHERE sender_id=:viewerId', 'parameters': {'viewerId': 1001},
                     'explain': {'query_block': {'select_id': 1, 'table': {'table_name': 'message', 'access_type': 'ref'}}}},
                    {'sql': 'SELECT id FROM message WHERE sender_id=:viewerId ORDER BY id DESC LIMIT :size',
                     'parameters': {'viewerId': 1001, 'size': 10},
                     'explain': {'query_block': {'select_id': 1, 'ordering_operation': {
                         'using_filesort': False, 'table': {'table_name': 'message', 'access_type': 'ref'}}}}}]},
                'responsePrivacyChecks': {'publicContainsNoFixturePasswordsOrBearerTokens': True,
                                         'sensitiveResponseFieldErrors': 0, 'checkedMeasuredResponses': 225},
                'stages': []}
    for level in [1, 5, 20]:
        samples = [{'endpoint': names[index % len(names)], 'latencyMs': 40 + index * .731,
                    'httpStatus': 200, 'businessCode': 0, 'payloadBytes': 200 + index,
                    'success': True, 'contractValid': True, 'privacyValid': True} for index in range(75)]
        capacity['stages'].append({'concurrency': level, 'summary': summarize(samples, 2), 'localTargetMet': True,
                                  'resources': {'status': 'RECORDED', 'samples': 5, 'samplingIntervalMs': 500,
                                                'peakObservedRssBytes': 300000000, 'cpuSeconds': .75, 'cpuPercentOneCore': 37.5},
                                  'endpoints': {name: summarize([sample for sample in samples if sample['endpoint'] == name], 2)
                                                for name in names}, 'samples': samples})
    return report, release, capacity, required


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
        report, release, capacity, required = publication_fixture()
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


class CapacityEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.report, self.release, self.capacity, self.required = publication_fixture()

    def verify(self, capacity=None):
        verify_release(self.report, self.release, capacity or self.capacity, self.required)

    def test_accepts_actual_producer_summaries_with_distinct_nearest_rank_percentiles(self):
        self.verify()
        stage = self.capacity['stages'][0]
        self.assertNotEqual(stage['summary']['p50Ms'], stage['summary']['p95Ms'])
        self.assertNotEqual(stage['summary']['p95Ms'], stage['summary']['p99Ms'])

    def test_dataset_cardinalities_require_real_integer_counts(self):
        for key in ('messageCount', 'groupCount'):
            for value in (float('inf'), float('nan'), True, '100000', 100000.5):
                with self.subTest(key=key, value=value):
                    altered = copy.deepcopy(self.capacity)
                    altered['dataset'][key] = value
                    altered['datasetAfter'][key] = value
                    with self.assertRaises(HistoryError):
                        self.verify(altered)

    def test_raw_http_code_latency_payload_and_endpoint_must_match_success_claims(self):
        invalid = [('httpStatus', 500), ('httpStatus', '200'), ('businessCode', 1), ('businessCode', False),
                   ('latencyMs', -1), ('latencyMs', float('nan')), ('latencyMs', float('inf')),
                   ('latencyMs', True), ('payloadBytes', 0), ('payloadBytes', 1.5), ('endpoint', 'unknown')]
        for key, value in invalid:
            with self.subTest(key=key, value=value):
                altered = copy.deepcopy(self.capacity)
                altered['stages'][0]['samples'][0][key] = value
                with self.assertRaises(HistoryError):
                    self.verify(altered)
        altered = copy.deepcopy(self.capacity)
        for sample in altered['stages'][0]['samples']:
            sample.update(httpStatus=500, businessCode=1, latencyMs=100000, endpoint='search')
        with self.assertRaises(HistoryError):
            self.verify(altered)

    def test_endpoint_counts_and_percentiles_are_recomputed_from_corresponding_raw_samples(self):
        for scope in ('summary', 'search'):
            for key, value in [('requests', 74), ('p95Ms', 1), ('p95Ms', float('nan')),
                               ('statusCounts', {'200': 1}), ('businessCodeCounts', {'1': 75}),
                               ('payloadBytesTotal', 1), ('payloadBytesMean', 1)]:
                with self.subTest(scope=scope, key=key):
                    altered = copy.deepcopy(self.capacity)
                    target = altered['stages'][0]['summary'] if scope == 'summary' else altered['stages'][0]['endpoints'][scope]
                    target[key] = value
                    with self.assertRaises(HistoryError):
                        self.verify(altered)
        altered = copy.deepcopy(self.capacity)
        altered['stages'][0]['samples'][0]['endpoint'] = 'history'
        with self.assertRaises(HistoryError):
            self.verify(altered)

    def test_underreported_latency_and_wrong_percentile_rounding_cannot_pass(self):
        altered = copy.deepcopy(self.capacity)
        for sample in altered['stages'][2]['samples']:
            sample['latencyMs'] = 5000
        with self.assertRaises(HistoryError):
            self.verify(altered)
        altered = copy.deepcopy(self.capacity)
        stage = altered['stages'][0]
        # 75 * .95 = 71.25: nearest rank uses item72, not item71 or interpolation.
        stage['summary']['p95Ms'] = round(sorted(sample['latencyMs'] for sample in stage['samples'])[70], 3)
        with self.assertRaises(HistoryError):
            self.verify(altered)

    def test_resource_status_without_real_finite_measurements_is_rejected(self):
        for replacement in ({'status': 'RECORDED'}, {'status': 'UNAVAILABLE'}):
            altered = copy.deepcopy(self.capacity); altered['stages'][0]['resources'] = replacement
            with self.assertRaises(HistoryError):
                self.verify(altered)
        for key, value in [('samples', 0), ('samples', True), ('peakObservedRssBytes', 0),
                           ('samplingIntervalMs', 0), ('cpuSeconds', -1), ('cpuSeconds', float('nan')),
                           ('cpuPercentOneCore', float('inf'))]:
            with self.subTest(key=key):
                altered = copy.deepcopy(self.capacity); altered['stages'][0]['resources'][key] = value
                with self.assertRaises(HistoryError):
                    self.verify(altered)

    def test_query_status_requires_two_distinct_selects_parameters_and_mysql_plans(self):
        altered = copy.deepcopy(self.capacity); altered['searchQueryProfile'] = {'status': 'RECORDED'}
        with self.assertRaises(HistoryError):
            self.verify(altered)
        for key, value in [('sql', ''), ('sql', 'DELETE FROM message'), ('parameters', {}),
                           ('explain', {}), ('explain', {'query_block': {'select_id': 1}}),
                           ('explain', {'query_block': {'select_id': 1, 'table': None}}),
                           ('explain', {'query_block': {'select_id': 1, 'nested_loop': [{}]}})]:
            with self.subTest(key=key, value=value):
                altered = copy.deepcopy(self.capacity); altered['searchQueryProfile']['queries'][0][key] = value
                with self.assertRaises(HistoryError):
                    self.verify(altered)
        for queries in ([], self.capacity['searchQueryProfile']['queries'][:1],
                        [self.capacity['searchQueryProfile']['queries'][0]] * 2):
            altered = copy.deepcopy(self.capacity); altered['searchQueryProfile']['queries'] = queries
            with self.assertRaises(HistoryError):
                self.verify(altered)

    def test_both_capacity_images_must_match_the_release_images_resolved_to_policy_pins(self):
        for name in ('mysql', 'redis'):
            for images in ({}, {name: {'imageId': 'sha256:' + '0' * 64}}):
                altered = copy.deepcopy(self.capacity); altered['serviceImages'] = images
                with self.assertRaises(HistoryError):
                    self.verify(altered)
            altered = copy.deepcopy(self.capacity)
            altered['serviceImages'][name]['imageId'] = 'sha256:' + '0' * 64
            with self.assertRaises(HistoryError):
                self.verify(altered)


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
            (root / 'mvnw').write_text('#!/bin/sh\nexit 0\n')
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
            self.assertTrue(command(output / 'candidate.git', 'ls-tree', 'main', 'mvnw').startswith(b'100755 blob '))
            self.assertEqual([REF], command(output / 'candidate.git', 'for-each-ref', '--format=%(refname)').decode().splitlines())
            self.assertEqual(b'', command(output / 'candidate', 'status', '--porcelain'))
            self.assertEqual('refs/heads/main', report['ref'])
            scan(output / 'candidate.git', {b'fixture-history-token-123456789'}, {b'admin'})


if __name__ == '__main__':
    unittest.main()
