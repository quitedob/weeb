#!/usr/bin/env python3
"""Prepare a verified sanitized main with the complete current source, in an isolated repository.

Preparation never rewrites the source repository. Publication is a separate explicit command
with a verified release manifest and an exact expected remote main; it never mirror-pushes.
Historical credentials and recovery files remain private under ignored .local/history-release.
"""
import argparse
import datetime as dt
import hashlib
import inspect
import json
import math
import os
from pathlib import Path
import re
import subprocess
import sys
import uuid

from release_gate import GateError, digest, json_bytes, source_inventory, snapshot, validate_runtime, write_json

KEY_PATHS = {'src/main/resources/es/http.p12', 'src/main/resources/es/transport.p12'}
REF = 'refs/heads/main'
SSH = 'ssh -o BatchMode=yes -o ConnectTimeout=10'


class HistoryError(RuntimeError):
    pass


def command(repo, *args, data=None, env=None):
    result = subprocess.run(['git', '-c', 'core.sshCommand=' + SSH, *map(str, args)], cwd=repo,
                            input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            env={**os.environ, 'GIT_OPTIONAL_LOCKS': '0', **(env or {})})
    if result.returncode:
        # Diagnostics can contain remote credentials or historical content: never echo them.
        raise HistoryError('Git operation failed: ' + str(args[0]))
    return result.stdout


def remote_inventory(root, remote):
    rows = command(root, 'ls-remote', remote).decode().splitlines()
    refs = {ref: oid for oid, ref in (row.split() for row in rows)}
    if 'HEAD' in refs and refs['HEAD'] != refs.get(REF):
        raise HistoryError('Remote default branch is not the reviewed main')
    return {ref: oid for ref, oid in refs.items() if ref != 'HEAD'}


def expected_remote(refs, expected):
    if refs != {REF: expected}:
        raise HistoryError('Remote heads/tags differ from the reviewed single-main inventory')


def owned_output(root, output):
    parent = (root / '.local/history-release').resolve()
    resolved = output.resolve()
    if output.is_symlink() or resolved.parent != parent or resolved == parent:
        raise HistoryError('History output must be a direct owned child of .local/history-release')
    return resolved


def yaml_credentials(blob):
    stack = []
    suffixes = {'spring.datasource.password': 'mysql_password', 'weeb.password': 'group_password',
                'jwt.secret': 'jwt_signing_key', 'deepseek.api-key': 'deepseek_api_key'}
    for line in blob.decode('utf-8-sig', errors='replace').splitlines():
        match = re.match(r'^( *)([\w.-]+):\s*(.*)$', line)
        if not match:
            continue
        indent, name, value = len(match[1]), match[2], match[3]
        while stack and stack[-1][0] >= indent:
            stack.pop()
        path = '.'.join([key for _, key in stack] + [name])
        if not value or value.startswith('#'):
            stack.append((indent, name))
            continue
        kind = next((kind for suffix, kind in suffixes.items() if path.endswith(suffix)), None)
        value = re.split(r'\s+#', value, maxsplit=1)[0].strip().strip('\'"')
        if kind and value and not value.startswith('${') and value.lower() not in ('null', '~', 'redacted_credential'):
            if '\n' in value or '==>' in value:
                raise HistoryError('Historical credential requires additional scoped review')
            yield kind, value.encode()


def redact_short(blob, values):
    for value in values:
        patterns = [
            r'((?:password|passwd|pwd|secret|密码|口令)[ \t]*[:=：][ \t]*[\'"`]?){value}(?=[\'"` \t#|,;}}&\r\n]|$)',
            r'([\'"](?:password|passwd|pwd|secret|密码|口令)[\'"][ \t]*:[ \t]*[\'"]){value}(?=[\'"])',
            r'(<(?:password|passwd)>[ \t]*){value}(?=[ \t]*</(?:password|passwd)>)',
            r'((?:getConnection|DriverManagerDataSource)\([ \t]*[\'"][^\'"\r\n]*jdbc[^\'"\r\n]*[\'"][ \t]*,[ \t]*[\'"][^\'"\r\n]*[\'"][ \t]*,[ \t]*[\'"]){value}(?=[\'"])',
        ]
        for pattern in patterns:
            expression = pattern.encode().replace(b'{value}', re.escape(value))
            blob = re.sub(expression, lambda match: match[1] + b'REDACTED_CREDENTIAL', blob, flags=re.I)
    return blob


def redact_yaml(blob):
    stack, lines = [], []
    for line in blob.decode('utf-8-sig').splitlines(keepends=True):
        match = re.match(r'^( *)([\w.-]+):[ \t]*(.*?)(\r?\n)?$', line)
        if match:
            indent, name, value = len(match[1]), match[2], match[3]
            while stack and stack[-1][0] >= indent:
                stack.pop()
            path = '.'.join([key for _, key in stack] + [name])
            if not value or value.startswith('#'):
                stack.append((indent, name))
            elif any(path.endswith(suffix) for suffix in ('spring.datasource.password', 'weeb.password', 'jwt.secret', 'deepseek.api-key')):
                literal = re.split(r'\s+#', value, maxsplit=1)[0].strip().strip('\'"')
                if literal and not literal.startswith('${') and literal.lower() not in ('null', '~'):
                    line = match[1] + name + ': REDACTED_CREDENTIAL' + (match[4] or '')
        lines.append(line)
    return ''.join(lines).encode()


def discover_secrets(repo):
    configs, long_values, short_values, categories = set(), set(), set(), set()
    for version in command(repo, 'rev-list', REF, '--', 'src/main/resources/application.yml').splitlines():
        spec = version.decode() + ':src/main/resources/application.yml'
        oid = command(repo, 'rev-parse', spec).strip()
        if oid in configs:
            continue
        configs.add(oid)
        for kind, value in yaml_credentials(command(repo, 'cat-file', 'blob', oid.decode())):
            categories.add(kind)
            (long_values if len(value) >= 8 else short_values).add(value)
    return configs, long_values, short_values, categories


def tree(repo, revision):
    result = {}
    for row in command(repo, 'ls-tree', '-rz', revision).split(b'\0'):
        if row:
            metadata, path = row.split(b'\t', 1)
            mode, kind, oid = metadata.split()
            result[path.decode()] = (mode, kind, oid)
    return result


def scan(repo, long_values, short_values):
    records = command(repo, 'rev-list', '--objects', '--all').splitlines()
    hits, keys, private_keys = 0, set(), 0
    process = subprocess.Popen(['git', 'cat-file', '--batch'], cwd=repo, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    try:
        for record in records:
            oid, _, path = record.partition(b' ')
            if path.decode(errors='replace') in KEY_PATHS:
                keys.add(path.decode())
            process.stdin.write(oid + b'\n'); process.stdin.flush()
            header = process.stdout.readline().split()
            blob = process.stdout.read(int(header[2]))
            if process.stdout.read(1) != b'\n':
                raise HistoryError('Malformed Git object stream')
            if any(value in blob for value in long_values) or redact_short(blob, short_values) != blob:
                hits += 1
            if re.search(rb'-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----', blob):
                private_keys += 1
    finally:
        process.stdin.close(); process.wait(timeout=30); process.stdout.close()
    result = {'objects': len(records), 'credentialMatches': hits, 'retiredKeyPaths': sorted(keys), 'privateKeyObjects': private_keys}
    if hits or keys or private_keys:
        raise HistoryError('Candidate history scan failed; credential contents are not printed')
    return result


def verify_trees(original, clean, mapping, configs, long_values, short_values):
    changed, count = set(), 0
    for line in mapping.read_text().splitlines()[1:]:
        old, new = line.split()
        if set(new) == {'0'}:
            raise HistoryError('History rewrite unexpectedly pruned a commit')
        before, after = tree(original, old), tree(clean, new)
        if set(after) - set(before) or not set(before).difference(after) <= KEY_PATHS:
            raise HistoryError('Unexpected historical path addition/removal')
        for path, entry in after.items():
            if before[path] == entry:
                continue
            if entry[:2] != before[path][:2]:
                raise HistoryError('Unexpected historical mode/type change')
            raw = command(original, 'cat-file', 'blob', before[path][2].decode())
            for value in sorted(long_values, key=lambda value: (-len(value), value)):
                raw = raw.replace(value, b'REDACTED_CREDENTIAL')
            raw = redact_short(raw, short_values)
            if before[path][2] in configs:
                raw = redact_yaml(raw)
            if raw != command(clean, 'cat-file', 'blob', entry[2].decode()):
                raise HistoryError('Unexpected historical content change')
            changed.add(path)
        count += 1
    return {'verifiedCommitTrees': count, 'redactedContentPaths': sorted(changed), 'removedPaths': sorted(KEY_PATHS)}


def prepare(root, output, filter_python):
    output = owned_output(root, output)
    if output.exists():
        raise HistoryError('Preparation requires a new output directory')
    remote = command(root, 'remote', 'get-url', 'origin').decode().strip()
    expected = command(root, 'rev-parse', REF).decode().strip()
    expected_remote(remote_inventory(root, remote), expected)
    state = command(root, 'for-each-ref', '--format=%(refname) %(objectname)')
    index_before = digest((root / '.git/index').read_bytes())
    inventory = source_inventory(root)
    output.mkdir(parents=True)
    if not command(root, 'check-ignore', str(output)).strip():
        raise HistoryError('Recovery directory must be ignored by the source repository')
    source = output / 'source'
    snapshot(inventory, source)
    recovery = output / 'original-main.bundle'
    command(root, 'bundle', 'create', recovery, REF)
    command(root, 'bundle', 'verify', recovery)
    clean = output / 'candidate.git'
    command(root, 'clone', '--bare', '--no-local', '--single-branch', '--no-tags', '--branch', 'main', root, clean)
    if command(clean, 'rev-parse', REF).decode().strip() != expected:
        raise HistoryError('Source branch changed while cloning')
    configs, long_values, short_values, categories = discover_secrets(clean)
    replacements = output / 'private-replacements.txt'
    replacements.write_bytes(b'\n'.join(b'literal:' + value + b'==>REDACTED_CREDENTIAL'
                                       for value in sorted(long_values, key=lambda value: (-len(value), value))) + b'\n')
    callback = output / 'private-callback.py'
    callback.write_text('import re\n' + inspect.getsource(redact_short) + '\n' + inspect.getsource(redact_yaml)
                        + '\nSHORT = ' + repr(short_values) + '\nCONFIGS = ' + repr(configs) + '\n', encoding='utf-8')
    prefix = 'namespace = {}; exec(open(' + repr(str(callback)) + ', encoding="utf-8").read(), namespace); '
    args = [str(filter_python), '-m', 'git_filter_repo', '--sensitive-data-removal', '--no-fetch', '--force',
            '--prune-empty', 'never', '--prune-degenerate', 'never', '--invert-paths',
            '--replace-text', str(replacements), '--replace-message', str(replacements),
            '--blob-callback', prefix + 'blob.data = namespace["redact_short"](blob.data, namespace["SHORT"]); '
            + '\nif blob.original_id in namespace["CONFIGS"]: blob.data = namespace["redact_yaml"](blob.data)',
            '--message-callback', prefix + 'return namespace["redact_short"](message, namespace["SHORT"])']
    for path in sorted(KEY_PATHS):
        args.extend(['--path', path])
    with (output / 'private-filter.log').open('wb') as log:
        result = subprocess.run(args, cwd=clean, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        raise HistoryError('History filter failed; inspect private-filter.log locally')
    for name in command(clean, 'remote').decode().splitlines():
        command(clean, 'config', '--remove-section', 'remote.' + name)
    history = verify_trees(root, clean, clean / 'filter-repo/commit-map', configs, long_values, short_values)
    clean_base = command(clean, 'rev-parse', REF).decode().strip()
    # Build the candidate tree solely from the reviewed normalized snapshot in a separate index.
    git_env = {'GIT_INDEX_FILE': str(output / 'candidate.index')}
    command(clean, 'read-tree', '--empty', env=git_env)
    command(clean, '--work-tree=' + str(source), '-c', 'core.autocrlf=false', 'add', '--all', '--force', env=git_env)
    # Windows filesystems cannot convey executable bits through chmod/stat. Preserve
    # the canonical snapshot modes explicitly in this isolated index.
    for name, _, mode in inventory:
        if mode == '0755':
            command(clean, '--work-tree=' + str(source), 'update-index', '--chmod=+x', '--', name, env=git_env)
    candidate_tree = command(clean, 'write-tree', env=git_env).decode().strip()
    identity = {'GIT_AUTHOR_NAME': 'Weeb release preparation', 'GIT_AUTHOR_EMAIL': 'release-preparation@weeb.invalid',
                'GIT_COMMITTER_NAME': 'Weeb release preparation', 'GIT_COMMITTER_EMAIL': 'release-preparation@weeb.invalid'}
    candidate = command(clean, 'commit-tree', candidate_tree, '-p', clean_base,
                        data=b'Preserve verified fixes and P2 capacity work on sanitized history\n', env=identity).decode().strip()
    command(clean, 'update-ref', REF, candidate, clean_base)
    final_tree = tree(clean, REF)
    if set(final_tree) != {name for name, _, _ in inventory}:
        raise HistoryError('Candidate does not contain the complete source inventory')
    for name, raw, mode in inventory:
        entry = final_tree[name]
        if entry[0].decode() != '100' + mode[-3:] or command(clean, 'cat-file', 'blob', entry[2].decode()) != raw:
            raise HistoryError('Candidate content differs from frozen source: ' + name)
    scanned = scan(clean, long_values, short_values)
    command(clean, 'fsck', '--full')
    if command(root, 'for-each-ref', '--format=%(refname) %(objectname)') != state or digest((root / '.git/index').read_bytes()) != index_before:
        raise HistoryError('Original repository refs or index changed during preparation')
    current = source_inventory(root)
    if current != inventory:
        raise HistoryError('Source changed during preparation; prepare again after the writer barrier')
    checkout = output / 'candidate'
    command(root, '-c', 'core.autocrlf=false', 'clone', '--no-local', '--no-tags', clean, checkout)
    command(checkout, 'remote', 'remove', 'origin')
    files = [{'path': name, 'sha256': digest(raw), 'mode': mode} for name, raw, mode in inventory]
    report = {'formatVersion': 1, 'status': 'PREPARED', 'published': False, 'providerRevocationsVerified': False,
              'remote': remote, 'ref': REF, 'expectedOldCommit': expected, 'sanitizedBaseCommit': clean_base,
              'candidateCommit': candidate, 'candidateGitTree': candidate_tree, 'sourceTreeSha256': digest(json_bytes(files)),
              'credentialCategories': sorted(categories), **history, 'scan': scanned,
              'completeCurrentSourcePreserved': True, 'originalRepositoryRefsAndIndexUnchanged': True,
              'excludedRefs': ['refs/stash', 'remote-tracking refs', 'tags'], 'preparedAt': dt.datetime.now(dt.timezone.utc).isoformat()}
    write_json(output / 'public/history-manifest.json', report)
    write_json(output / 'public/source-files.json', files)
    print('Prepared history evidence: ' + str(output / 'public/history-manifest.json'))


def candidate_requirements(repo, revision):
    def contents(path):
        return command(repo, 'show', revision + ':' + path)
    registry = json.loads(contents('src/main/resources/sql/schema-manifest.json'))
    versions = []
    for migration in registry['migrations']:
        raw = b''.join(path.encode() + b'\0' + contents('src/main/resources/' + path).replace(b'\r\n', b'\n') + b'\0'
                       for path in migration['resources'])
        versions.append({'version': migration['version'], 'checksum': digest(raw), 'state': 'APPLIED'})
    return {'policy': json.loads(contents('ci/release-policy.json')), 'schemaVersions': versions,
            'dependencyLocks': {path: digest(contents(path)) for path in ('pom.xml', 'Vue/package-lock.json', '.mvn/wrapper/maven-wrapper.properties')},
            'schemaManifestSha256': digest(contents('src/main/resources/sql/schema-manifest.json')),
            'contractManifestSha256': digest(contents('docs/contracts.json')),
            'capacityToolSha256': digest(contents('scripts/capacity_probe.py'))}


def sha256(value):
    return isinstance(value, str) and re.fullmatch(r'[0-9a-f]{64}', value) is not None


def finite_number(value, minimum=0):
    return type(value) in (int, float) and math.isfinite(value) and value >= minimum


def verify_capacity_summary(summary, samples, target):
    if not samples:
        raise HistoryError('A required capacity endpoint has no measured requests')
    latencies = sorted(sample['latencyMs'] for sample in samples)
    size = len(samples)
    expected = {'requests': size, 'errors': 0, 'contractErrors': 0, 'privacyErrors': 0,
                'errorRate': 0, 'statusCounts': {'200': size}, 'businessCodeCounts': {'0': size},
                'payloadBytesTotal': sum(sample['payloadBytes'] for sample in samples),
                'payloadBytesMean': round(sum(sample['payloadBytes'] for sample in samples) / size, 2)}
    for percentile in (50, 95, 99):
        expected[f'p{percentile}Ms'] = round(latencies[math.ceil(percentile / 100 * size) - 1], 3)
    if (any(summary.get(key) != value for key, value in expected.items())
            or any(type(summary.get(key)) is not int for key in ('requests', 'errors', 'contractErrors', 'privacyErrors', 'payloadBytesTotal'))
            or any(not finite_number(summary.get(key)) for key in ('errorRate', 'p50Ms', 'p95Ms', 'p99Ms', 'payloadBytesMean'))
            or not finite_number(summary.get('wallSeconds')) or summary['wallSeconds'] <= 0
            or not finite_number(summary.get('requestsPerSecond')) or summary['requestsPerSecond'] <= 0
            or expected['p95Ms'] > target):
        raise HistoryError('Capacity summary does not match its measured samples or latency target')


def verify_capacity_resources(resources):
    if (resources.get('status') != 'RECORDED'
            or type(resources.get('samples')) is not int or resources['samples'] < 2
            or type(resources.get('peakObservedRssBytes')) is not int or resources['peakObservedRssBytes'] <= 0
            or not finite_number(resources.get('samplingIntervalMs')) or resources['samplingIntervalMs'] <= 0
            or any(not finite_number(resources.get(key)) for key in ('cpuSeconds', 'cpuPercentOneCore'))):
        raise HistoryError('Actual finite process CPU/RSS measurements are required')


def verify_capacity_queries(profile):
    def has_table_plan(value):
        if isinstance(value, dict):
            if (isinstance(value.get('table_name'), str) and value['table_name']
                    and isinstance(value.get('access_type'), str) and value['access_type']):
                return True
            return any(has_table_plan(child) for child in value.values())
        return isinstance(value, list) and any(has_table_plan(child) for child in value)

    queries = profile.get('queries')
    if (profile.get('status') != 'RECORDED' or not sha256(profile.get('harnessSha256'))
            or not isinstance(queries, list) or len(queries) != 2):
        raise HistoryError('Two captured search queries and EXPLAIN records are required')
    statements = set()
    for query in queries:
        if not isinstance(query, dict):
            raise HistoryError('Malformed captured query evidence')
        sql, parameters, explain = query.get('sql'), query.get('parameters'), query.get('explain')
        block = explain.get('query_block') if isinstance(explain, dict) else None
        if (not isinstance(sql, str) or not re.match(r'^SELECT\s+', sql.strip(), re.I)
                or not isinstance(parameters, dict) or not parameters
                or not isinstance(block, dict) or type(block.get('select_id')) is not int or block['select_id'] < 1
                or not has_table_plan(block)):
            raise HistoryError('Captured search SQL, parameters or MySQL query plan is missing')
        statements.add(sql.strip())
    if len(statements) != 2:
        raise HistoryError('Search count and result queries must be distinct captured statements')


def verify_capacity(capacity, required):
    policy = required['policy']['capacity']
    stages = capacity.get('stages', [])
    dataset = capacity.get('dataset', {})
    if (capacity.get('scope') != policy['scope'] or capacity.get('toolSha256') != required['capacityToolSha256']
            or type(dataset.get('messageCount')) is not int or dataset['messageCount'] < policy['minimumMessages']
            or type(dataset.get('groupCount')) is not int or dataset['groupCount'] < policy['minimumGroups']
            or [stage.get('concurrency') for stage in stages] != policy['concurrencyStages']
            or capacity.get('configuration', {}).get('paging') is not True):
        raise HistoryError('Capacity dataset, tool, paging or concurrency evidence differs from candidate policy')
    target = capacity.get('localTarget', {}).get('p95MsMaximum')
    if not finite_number(target) or target <= 0 or target > policy['p95MsMaximum']:
        raise HistoryError('Capacity target was relaxed relative to the reviewed policy')
    for stage in stages:
        endpoints = stage.get('endpoints', {})
        summary = stage.get('summary', {})
        samples = stage.get('samples', [])
        if (set(endpoints) != set(policy['endpoints']) or summary.get('requests', 0) < policy['minimumRequestsPerStage']
                or len(samples) != summary.get('requests') or stage.get('localTargetMet') is not True
                or stage.get('resources', {}).get('status') != 'RECORDED'):
            raise HistoryError('Required measured capacity routes, samples or resources are missing')
        verify_capacity_resources(stage['resources'])
        grouped = {name: [] for name in policy['endpoints']}
        for sample in samples:
            if (not isinstance(sample, dict) or sample.get('endpoint') not in grouped
                    or any(sample.get(key) is not True for key in ('success', 'contractValid', 'privacyValid'))
                    or type(sample.get('httpStatus')) is not int or sample['httpStatus'] != 200
                    or type(sample.get('businessCode')) is not int or sample['businessCode'] != 0
                    or not finite_number(sample.get('latencyMs'))
                    or type(sample.get('payloadBytes')) is not int or sample['payloadBytes'] <= 0):
                raise HistoryError('Capacity sample failed its response or privacy contract')
            grouped[sample['endpoint']].append(sample)
        verify_capacity_summary(summary, samples, target)
        for name, measured in grouped.items():
            verify_capacity_summary(endpoints[name], measured, target)
    privacy = capacity.get('responsePrivacyChecks', {})
    if (privacy.get('publicContainsNoFixturePasswordsOrBearerTokens') is not True
            or privacy.get('sensitiveResponseFieldErrors') != 0
            or privacy.get('checkedMeasuredResponses') != sum(stage['summary']['requests'] for stage in stages)
            or capacity.get('searchQueryProfile', {}).get('status') != 'RECORDED'):
        raise HistoryError('Capacity query-plan/privacy evidence is missing')
    verify_capacity_queries(capacity['searchQueryProfile'])
    after = capacity.get('datasetAfter', {})
    for key in ('messageCount', 'groupCount', 'dataFingerprint', 'generatorSha256'):
        if not dataset.get(key) or after.get(key) != dataset[key]:
            raise HistoryError('Capacity fixture changed during verification')


def verify_release(report, release, capacity, required):
    if (release.get('status') != 'PASS' or release.get('worktreeClean') is not True
            or release.get('kind') != 'release_candidate' or release.get('reproducibility') != 'PASS'
            or release.get('sourceCommit') != report['candidateCommit']
            or release.get('sourceTreeSha256') != report['sourceTreeSha256']
            or not sha256(report.get('sourceTreeSha256'))):
        raise HistoryError('Passing clean release evidence must identify this exact candidate')
    artifacts = release.get('artifacts', {})
    files = artifacts.get('frontendFiles', [])
    if (not sha256(artifacts.get('backendJarSha256')) or not sha256(artifacts.get('frontendTreeSha256'))
            or not files or any(not sha256(item.get('sha256')) or not item.get('path') for item in files)
            or digest(json_bytes(files)) != artifacts['frontendTreeSha256']):
        raise HistoryError('Complete valid backend/frontend artifact digests are required')
    for kind in ('backendTests', 'frontendTests'):
        tests = release.get(kind, {})
        suites = tests.get('suites', {})
        if (tests.get('status') != 'PASS' or not isinstance(suites, dict) or not suites
                or any(not isinstance(count, int) or count < 1 for count in suites.values())
                or tests.get('tests') != sum(suites.values())
                or any(tests.get(key, 1) for key in ('failures', 'errors', 'skipped'))):
            raise HistoryError('Candidate test evidence is incomplete')
    policy = required['policy']
    suites = release['backendTests']['suites']
    if any(not any(s == name or s.endswith('.' + name) for s in suites) for name in policy['backendRequiredSuites']):
        raise HistoryError('Required integration suites are missing')
    try:
        validate_runtime(release.get('runtime', {}), policy['runtimeRequiredChecks'])
    except GateError as error:
        raise HistoryError('Required runtime evidence is incomplete') from error
    colors = release.get('colorAudit', {})
    if colors.get('status') != 'PASS' or colors.get('findings') != [] or not colors.get('files'):
        raise HistoryError('Color audit evidence is incomplete')
    actual_versions = [{key: entry.get(key) for key in ('version', 'checksum', 'state')} for entry in release.get('schemaVersions', [])]
    if actual_versions != required['schemaVersions']:
        raise HistoryError('Validated migration versions/checksums do not match candidate resources')
    for key in ('dependencyLocks', 'schemaManifestSha256', 'contractManifestSha256'):
        if release.get(key) != required[key]:
            raise HistoryError('Candidate dependency/schema/contract evidence mismatch')
    dependencies = release.get('backendResolvedDependencies', {})
    if not dependencies or not all(sha256(value) for value in dependencies.values()):
        raise HistoryError('Resolved backend dependency digests are missing')
    for name, pinned in policy['serviceImages'].items():
        image = release.get('serviceImages', {}).get(name, {})
        image_id = image.get('imageId', '')
        if (pinned not in image.get('repoDigests', []) or not isinstance(image_id, str)
                or not image_id.startswith('sha256:') or not sha256(image_id[7:])):
            raise HistoryError('Integration service image evidence differs from candidate policy')
        # The capacity producer records immutable image IDs; the release producer also
        # resolves those same IDs to the repository digests pinned in candidate policy.
        if capacity.get('serviceImages', {}).get(name, {}).get('imageId') != image_id:
            raise HistoryError('Capacity service image differs from the pinned release image')
    if capacity.get('backendJarSha256') != release.get('artifacts', {}).get('backendJarSha256'):
        raise HistoryError('Capacity evidence must identify the exact candidate JAR')
    if capacity.get('status') != 'PASS':
        raise HistoryError('Capacity acceptance has not passed')
    verify_capacity(capacity, required)


def publish(root, output, release_path, capacity_path, dry_run):
    output = owned_output(root, output)
    report = json.loads((output / 'public/history-manifest.json').read_text())
    release = json.loads(release_path.read_text())
    capacity = json.loads(capacity_path.read_text())
    clean = output / 'candidate.git'
    if command(clean, 'rev-parse', REF).decode().strip() != report['candidateCommit']:
        raise HistoryError('Prepared candidate ref changed')
    verify_release(report, release, capacity, candidate_requirements(clean, report['candidateCommit']))
    expected_remote(remote_inventory(root, report['remote']), report['expectedOldCommit'])
    args = ['push', '--atomic', '--no-follow-tags', '--force-with-lease=' + REF + ':' + report['expectedOldCommit']]
    if dry_run:
        args.append('--dry-run')
    command(clean, *args, report['remote'], report['candidateCommit'] + ':' + REF)
    if dry_run:
        print('Dry-run passed for the exact candidate and expected main; nothing published')
        return
    expected_remote(remote_inventory(root, report['remote']), report['candidateCommit'])
    report.update(status='PUBLISHED', published=True, publishedAt=dt.datetime.now(dt.timezone.utc).isoformat())
    report['releaseManifestSha256'] = digest(release_path.read_bytes())
    report['capacityManifestSha256'] = digest(capacity_path.read_bytes())
    report['hostCachedObjectsAndOtherClonesPurged'] = False
    write_json(output / 'public/history-manifest.json', report)
    print('Published and verified ' + report['remote'] + ' ' + REF + ' at ' + report['candidateCommit'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['prepare', 'dry-run', 'publish'])
    parser.add_argument('--output', type=Path)
    parser.add_argument('--filter-python', type=Path, default=Path('.local/verification/history-tools/Scripts/python.exe'))
    parser.add_argument('--release-manifest', type=Path)
    parser.add_argument('--capacity-manifest', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    output = args.output or root / '.local/history-release' / (dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ-') + uuid.uuid4().hex[:8])
    if args.action == 'prepare':
        prepare(root, output.resolve(), args.filter_python.resolve())
    elif args.release_manifest and args.capacity_manifest:
        publish(root, output.resolve(), args.release_manifest.resolve(), args.capacity_manifest.resolve(), args.action == 'dry-run')
    else:
        raise HistoryError('Publication requires both exact release and capacity manifests')


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(str(error) if isinstance(error, HistoryError) else type(error).__name__, file=sys.stderr)
        sys.exit(1)
