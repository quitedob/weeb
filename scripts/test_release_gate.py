import tempfile
import unittest
from pathlib import Path
from release_gate import GateError, junit_summary, validate_runtime, runtime_environment


class ReleaseEvidenceGateTest(unittest.TestCase):
    def test_runtime_cannot_inherit_deployment_overrides_or_jvm_injection(self):
        source = {'WEEB_TEST_MYSQL_URL': 'jdbc:mysql://127.0.0.1:23306/weeb_audit',
                  'WEEB_TEST_MYSQL_PASSWORD': 'disposable-fixture', 'WEEB_TEST_REDIS_PORT': '16379',
                  'PATH': 'retained', 'SPRING_APPLICATION_JSON': '{"spring":{"datasource":{"url":"external"}}}',
                  'spring.datasource.url': 'external', 'redis_host': 'external',
                  'MYSQL_URL': 'external', 'JAVA_TOOL_OPTIONS': '-javaagent:external',
                  'jwt.secret': 'old', 'REDIS_DATABASE': '9', 'server.address': '0.0.0.0', 'server_port': '8080',
                  'CAMPUS_MEDIA_DIRECTORY': '/deployed/private', 'AVATAR_DIRECTORY': '/deployed/avatars'}
        actual = runtime_environment(source)
        for key in ('SPRING_APPLICATION_JSON', 'spring.datasource.url', 'redis_host', 'JAVA_TOOL_OPTIONS', 'jwt.secret', 'server.address', 'server_port'):
            self.assertNotIn(key, actual)
        self.assertEqual(actual['MYSQL_URL'], source['WEEB_TEST_MYSQL_URL'])
        self.assertEqual(actual['REDIS_HOST'], '127.0.0.1')
        self.assertEqual(actual['REDIS_DATABASE'], '0')
        self.assertEqual(actual['SERVER_ADDRESS'], '127.0.0.1')
        self.assertEqual(actual['SERVER_PORT'], '18080')
        self.assertEqual(actual['CAMPUS_MEDIA_DIRECTORY'], '.local/campus-runtime-media')
        self.assertEqual(actual['AVATAR_DIRECTORY'], '.local/runtime-avatars')
        self.assertEqual(actual['PATH'], 'retained')
        self.assertGreaterEqual(len(actual['JWT_SECRET']), 64)

    def report(self, xml, required=()):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'TEST-fixture.xml'
            path.write_text(xml, encoding='utf-8')
            return junit_summary([path], required)

    def test_nested_totals_are_not_double_counted(self):
        result = self.report('<testsuites tests="1"><testsuite name="a" tests="1"><testcase classname="app.Required" name="real"/></testsuite></testsuites>', ['Required'])
        self.assertEqual(result['tests'], 1)

    def test_skipped_integration_is_not_a_green_release(self):
        with self.assertRaises(GateError):
            self.report('<testsuite><testcase classname="Required"><skipped/></testcase></testsuite>', ['Required'])

    def test_missing_integration_suite_fails_even_when_unit_suite_passes(self):
        with self.assertRaises(GateError):
            self.report('<testsuite><testcase classname="Unit"/></testsuite>', ['Required'])

    def test_empty_failed_and_error_reports_are_rejected(self):
        for case in ('', '<testcase><failure/></testcase>', '<testcase><error/></testcase>'):
            with self.subTest(case=case), self.assertRaises(GateError):
                self.report('<testsuite>' + case + '</testsuite>')

    def test_runtime_evidence_requires_all_named_boolean_successes(self):
        for checks in ({}, {'browser': 'true'}, {'browser': True}, {'browser': True, 'http': False}):
            with self.subTest(checks=checks), self.assertRaises(GateError):
                validate_runtime({'status': 'PASS', 'checks': checks}, ['browser', 'http'])
        validate_runtime({'status': 'PASS', 'checks': {'browser': True, 'http': True}}, ['browser', 'http'])


if __name__ == '__main__':
    unittest.main()
