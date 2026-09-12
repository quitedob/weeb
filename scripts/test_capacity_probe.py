"""Focused stdlib tests for evidence integrity, fixture safety and real HTTP sampling."""
import http.server
from contextlib import ExitStack
import hashlib
import io
import json
from pathlib import Path
import subprocess
import tempfile
import threading
import time
import unittest
from unittest.mock import Mock, patch

import capacity_probe as probe


class CapacitySafetyTest(unittest.TestCase):
    def test_ambient_spring_service_and_jvm_overrides_cannot_escape_owned_fixture(self):
        ambient = {
            'PATH': 'preserved-tool-path', 'JAVA_HOME': 'preserved-java-home',
            'SPRING_DATASOURCE_URL': 'jdbc:mysql://production/real_data',
            'spring.datasource.username': 'external-user',
            'SPRING_APPLICATION_JSON': '{"spring":{"datasource":{"url":"jdbc:mysql://production/data"}}}',
            'SPRING_CONFIG_LOCATION': 'file:/external/application.yml',
            'SPRING_CONFIG_ADDITIONAL_LOCATION': 'file:/external/extra.yml',
            'SPRING_DATA_REDIS_URL': 'redis://external:6379/0',
            'MYSQL_URL': 'jdbc:mysql://external/data', 'MYSQL_PASSWORD': 'ambient-password',
            'REDIS_HOST': 'external', 'JWT_SECRET': 'ambient-jwt',
            'JAVA_TOOL_OPTIONS': '-Xmx8g -Dspring.datasource.url=jdbc:mysql://external/data',
            'JDK_JAVA_OPTIONS': '-Dspring.profiles.active=external', '_JAVA_OPTIONS': '-Xmx8g',
        }
        database = 'weeb_audit_capacity_' + 'a' * 24
        with patch.dict(probe.os.environ, ambient, clear=True):
            env = probe.app_environment(database, 'isolated-fixture-password', 18080, 15)
        self.assertEqual('preserved-tool-path', env['PATH'])
        self.assertEqual('preserved-java-home', env['JAVA_HOME'])
        self.assertTrue(env['SPRING_DATASOURCE_URL'].startswith('jdbc:mysql://127.0.0.1:23306/' + database + '?'))
        self.assertEqual(env['MYSQL_URL'], env['SPRING_DATASOURCE_URL'])
        self.assertEqual('root', env['SPRING_DATASOURCE_USERNAME'])
        self.assertEqual('isolated-fixture-password', env['SPRING_DATASOURCE_PASSWORD'])
        self.assertEqual(('127.0.0.1', '16379', '15'),
                         tuple(env[key] for key in ('SPRING_DATA_REDIS_HOST', 'SPRING_DATA_REDIS_PORT', 'SPRING_DATA_REDIS_DATABASE')))
        self.assertEqual('prod', env['SPRING_PROFILES_ACTIVE'])
        self.assertNotEqual('ambient-jwt', env['JWT_SECRET'])
        for key in ('SPRING_APPLICATION_JSON', 'SPRING_CONFIG_LOCATION', 'SPRING_CONFIG_ADDITIONAL_LOCATION',
                    'SPRING_DATA_REDIS_URL', 'spring.datasource.username', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'):
            self.assertNotIn(key, env)
        self.assertNotIn('jdbc:mysql://production/real_data', env.values())

    def test_hashing_streams_multiple_chunks_without_python_311_file_digest(self):
        content = b'capacity-provenance\x00' * 100000
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'artifact.bin'
            path.write_bytes(content)
            with patch.object(hashlib, 'file_digest', side_effect=AssertionError('Python3.10 has no file_digest'), create=True):
                self.assertEqual(hashlib.sha256(content).hexdigest(), probe.sha256(path))

    def test_shared_or_remote_databases_and_paths_are_rejected_before_execution(self):
        for value in ('weeb', 'weeb_audit', 'weeb_audit_capacity_old', 'weeb_audit_capacity_' + 'a' * 24 + '; DROP DATABASE weeb'):
            with self.assertRaises(probe.ProbeError):
                probe.validate_database(value)
        for url in ('jdbc:mysql://example.com:23306/weeb_audit', 'jdbc:mysql://127.0.0.1:3306/weeb_audit',
                    'jdbc:mysql://127.0.0.1:23306/production', 'jdbc:mysql://localhost:23306/weeb_audit/other'):
            with self.assertRaises(probe.ProbeError):
                probe.validate_audit_url(url)
        with self.assertRaises(probe.ProbeError):
            probe.owned_path(probe.ROOT / 'public' / 'fixture.json')
        db = probe.Database('never-called', Path('.'))
        with patch.object(probe.subprocess, 'run') as run:
            with self.assertRaises(probe.ProbeError):
                db.execute('SELECT 1', 'weeb_audit')
            run.assert_not_called()

    def test_fixture_requires_matching_database_marker(self):
        identity = 'a' * 24
        db = probe.Database('unused', Path('.'))
        db.execute = Mock(return_value='b' * 24)
        with self.assertRaises(probe.ProbeError):
            db.verify({'database': 'weeb_audit_capacity_' + identity, 'runId': identity})

    def test_actual_named_sql_binding_preserves_literals_and_rejects_unknown_values(self):
        sql = "SELECT ':actor', `:actor`, :actor, :q, :size"
        self.assertEqual("SELECT ':actor', `:actor`, 7, 'a''b', 10",
                         probe.bind_captured_sql(sql, {'actor': 7, 'q': "a'b", 'size': 10}))
        with self.assertRaises(probe.ProbeError):
            probe.bind_captured_sql('SELECT :value', {'value': object()})

    def test_business_errors_ignored_pagination_and_sensitive_fields_cannot_pass(self):
        sample = {'success': True, 'httpStatus': 200, 'latencyMs': 1, 'payloadBytes': 1, 'businessCode': 0}
        fixture = {'groups': 20}
        ignored = probe.check_response(sample, {'data': list(range(10))}, 'owned_groups', fixture, True, 10)
        self.assertFalse(ignored['success'])
        secret = probe.check_response(sample, {'data': [{'id': 1, 'password': 'must-not-appear'}]}, 'history', fixture, False, 10)
        self.assertFalse(secret['success'])
        self.assertFalse(secret['privacyValid'])
        proper = probe.check_response(sample, {'data': {'list': list(range(10)), 'total': 10, 'page': 0, 'size': 10}},
                                      'owned_groups', fixture, True, 10)
        self.assertTrue(proper['success'])
        self.assertFalse(probe.check_response({**sample, 'success': False, 'businessCode': 1003}, {'data': [1]},
                                              'history', fixture, False, 10)['success'])

    def test_percentiles_and_status_counts_include_failures(self):
        samples = [{'latencyMs': index, 'success': index != 100, 'httpStatus': 500 if index == 100 else 200,
                    'businessCode': -1 if index == 100 else 0, 'payloadBytes': 8} for index in range(1, 101)]
        result = probe.summarize(samples, 2)
        self.assertEqual((50, 95, 99), (result['p50Ms'], result['p95Ms'], result['p99Ms']))
        self.assertEqual({'200': 99, '500': 1}, result['statusCounts'])
        self.assertEqual(.01, result['errorRate'])
        self.assertEqual(50, result['requestsPerSecond'])
        self.assertEqual(800, result['payloadBytesTotal'])
        self.assertIsNone(probe.summarize([], 0)['p95Ms'])

    def test_owned_jvm_is_killed_after_graceful_shutdown_timeout(self):
        application = probe.Application('unused', Path('unused'), {}, Path('.'), '1g')
        application.log = io.BytesIO()
        application.process = Mock()
        application.process.poll.return_value = None
        application.process.wait.side_effect = [subprocess.TimeoutExpired('owned', 12), 0]
        application.__exit__(None, None, None)
        application.process.terminate.assert_called_once()
        application.process.kill.assert_called_once()
        self.assertTrue(application.log.closed)


class ProbeExitStatusTest(unittest.TestCase):
    def test_completed_run_reports_threshold_failure_to_ci_and_success_exits_zero(self):
        # Exercise the real orchestration/report/exit path; isolate external JVM,
        # Docker and HTTP boundaries so this test never starts a service.
        for latency, expected_status, expected_exit in ((1001, 'FAIL', 1), (999, 'PASS', 0)):
            with self.subTest(status=expected_status), tempfile.TemporaryDirectory() as directory, ExitStack() as stack:
                run_root = Path(directory)
                fixture_path = run_root / 'fixture.json'
                fixture = {'runId': 'a' * 24, 'database': 'weeb_audit_capacity_' + 'a' * 24,
                           'seedComplete': True, 'groups': 2, 'messages': 10,
                           'username': 'disposable-actor', 'password': 'fixture-secret-not-in-report'}
                fixture_path.write_text(json.dumps(fixture), encoding='utf-8')
                jar = run_root / 'fake.jar'; jar.write_bytes(b'isolated test artifact')
                summary = {'requests': 5, 'errors': 0, 'privacyErrors': 0, 'p95Ms': latency}
                stage = {'summary': summary, 'endpoints': {name: {'requests': 1, 'errors': 0, 'p95Ms': latency}
                                                          for name, _ in probe.endpoints(False, 10)}}
                application = Mock()
                application.process.pid = 123
                application.process.poll.return_value = 0
                stack.enter_context(patch.object(probe, 'RUN_ROOT', run_root))
                stack.enter_context(patch.dict(probe.os.environ, {'WEEB_TEST_MYSQL_PASSWORD': 'database-secret-not-in-report'}, clear=True))
                stack.enter_context(patch.object(probe, 'validate_container', return_value={'port': 23306}))
                stack.enter_context(patch.object(probe, 'Database'))
                stack.enter_context(patch.object(probe, 'java_executable', return_value='unused-java'))
                stack.enter_context(patch.object(probe, 'run_private'))
                stack.enter_context(patch.object(probe, 'jar_provenance', return_value={'jarSha256': 'a' * 64, 'source': {}}))
                app_type = stack.enter_context(patch.object(probe, 'Application'))
                app_type.return_value.__enter__.return_value = application
                stack.enter_context(patch.object(probe, 'verify_dataset', return_value={'messages': 10, 'groups': 2}))
                stack.enter_context(patch.object(probe, 'account', return_value={'token': 'bearer-secret-not-in-report'}))
                stack.enter_context(patch.object(probe, 'measure', return_value=stage))
                stack.enter_context(patch('sys.stdout', new=io.StringIO()))
                result = probe.main(['--jar', str(jar), '--fixture', str(fixture_path), '--requests', '5',
                                     '--concurrency', '1', '--warmup', '0', '--skip-explain'])
                reports = list(run_root.glob('*/public/result.json'))
                self.assertEqual(1, len(reports))
                report = json.loads(reports[0].read_text(encoding='utf-8'))
                self.assertEqual(expected_status, report['status'])
                self.assertEqual(expected_exit, result)
                self.assertEqual(5, report['responsePrivacyChecks']['checkedMeasuredResponses'])


class HttpMeasurementTest(unittest.TestCase):
    def test_sampler_uses_bounded_real_concurrency_and_full_response_bytes(self):
        state = {'active': 0, 'peak': 0}
        lock = threading.Lock()
        class Handler(http.server.BaseHTTPRequestHandler):
            def do_GET(self):
                with lock:
                    state['active'] += 1
                    state['peak'] = max(state['peak'], state['active'])
                time.sleep(.03)
                data = {'totalUnread': 1, 'unreadList': [{'chat_id': 101, 'unread_count': 1}]}
                raw = json.dumps({'code': 0, 'message': 'OK', 'data': data}).encode()
                self.send_response(200); self.send_header('Content-Length', str(len(raw))); self.end_headers()
                self.wfile.write(raw)
                with lock:
                    state['active'] -= 1
            def log_message(self, *_):
                pass
        server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
        try:
            result = probe.measure(server.server_port, 'local-test-token', [('unread', '/unread')],
                                   3, 12, 2, probe.os.getpid(), {'groups': 2}, False, 10)
            self.assertEqual(12, result['summary']['requests'])
            self.assertEqual(0, result['summary']['errors'])
            self.assertGreater(result['summary']['payloadBytesTotal'], 0)
            self.assertGreater(state['peak'], 1)
            self.assertLessEqual(state['peak'], 3)
            self.assertGreater(result['summary']['p50Ms'], 20)
            if probe.os.name == 'nt' or probe.sys.platform.startswith('linux'):
                self.assertEqual('RECORDED', result['resources']['status'])
                self.assertGreater(result['resources']['peakObservedRssBytes'], 0)
        finally:
            server.shutdown(); server.server_close(); thread.join(timeout=3)


if __name__ == '__main__':
    unittest.main()
