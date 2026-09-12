import tempfile
import unittest
from pathlib import Path
from release_gate import GateError, junit_summary, validate_runtime


class ReleaseEvidenceGateTest(unittest.TestCase):
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
