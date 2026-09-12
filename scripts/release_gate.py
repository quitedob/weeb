#!/usr/bin/env python3
"""Build and verify two immutable source copies. No commit, deployment or history mutation.

Requires dedicated MySQL/Redis audit services; output stays in ignored .local/release.
Only public/*.json and the reviewed public screenshot are intended for CI artifact upload.
"""
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import secrets
import shutil
import socket
import subprocess
import sys
import time
import urllib.request
import uuid
import zipfile
import xml.etree.ElementTree as ET


class GateError(RuntimeError):
    pass


def digest(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(value):
    return (json.dumps(value, sort_keys=True, ensure_ascii=True, separators=(',', ':')) + '\n').encode()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(json_bytes(value))


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args])


def source_inventory(root):
    paths = git(root, 'ls-files', '--cached', '--others', '--exclude-standard', '-z').decode().split('\0')
    result = []
    for name in sorted(set(filter(None, paths))):
        path = root / name
        if not path.exists():
            continue  # Tracked deletion is represented by absence in the frozen tree.
        if path.is_symlink() or path.is_dir() or not path.resolve().is_relative_to(root):
            raise GateError('Source snapshot contains an unsupported link or nested repository: ' + name)
        raw = path.read_bytes()
        if path.suffix.lower() in {'.key', '.pem', '.p12', '.pfx', '.jks'} or re.search(rb'-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----', raw):
            raise GateError('Source snapshot contains private key material: ' + name)
        try:
            if b'\0' not in raw:
                raw = raw.decode('utf-8').replace('\r\n', '\n').encode('utf-8')
        except UnicodeError:
            pass
        result.append((name, raw, '0755' if name == 'mvnw' or path.suffix == '.sh' else '0644'))
    return result


def snapshot(inventory, destination):
    destination.mkdir(parents=True, exist_ok=False)
    for name, raw, mode in inventory:
        target = destination / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(raw)
        target.chmod(int(mode, 8))


def junit_summary(paths, required=()):
    """Count leaf cases, not nested suite totals; skips and missing suites fail closed."""
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    suites = {}
    for path in paths:
        root = ET.parse(path).getroot()
        for suite in root.iter('testsuite'):
            for case in suite.findall('testcase'):
                name = case.get('classname') or suite.get('name', '')
                totals['tests'] += 1
                suites[name] = suites.get(name, 0) + 1
                for kind in ('failure', 'error', 'skipped'):
                    if case.find(kind) is not None:
                        totals[{'failure': 'failures', 'error': 'errors', 'skipped': 'skipped'}[kind]] += 1
    missing = [name for name in required if not any(s == name or s.endswith('.' + name) for s in suites)]
    if not totals['tests'] or any(totals[key] for key in ('failures', 'errors', 'skipped')) or missing:
        raise GateError('JUnit gate failed: ' + json.dumps({**totals, 'missingSuites': missing}))
    return {**totals, 'suites': suites, 'status': 'PASS'}


def artifacts(source):
    jar = source / 'target/WEEB-0.0.1-SNAPSHOT.jar'
    files = [{'path': p.relative_to(source / 'Vue/dist').as_posix(), 'sha256': digest(p.read_bytes())}
             for p in sorted((source / 'Vue/dist').rglob('*')) if p.is_file()]
    if not jar.is_file() or not files:
        raise GateError('Build artifacts missing')
    return {'backendJarSha256': digest(jar.read_bytes()), 'frontendTreeSha256': digest(json_bytes(files)), 'frontendFiles': files}


def run(command, cwd, logfile, env=None):
    with logfile.open('wb') as output:
        process = subprocess.run(list(map(str, command)), cwd=cwd, env=env, stdout=output, stderr=subprocess.STDOUT)
    if process.returncode:
        raise GateError('Command failed; inspect private log ' + logfile.name)


def version(command, env):
    result = subprocess.run(command, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    return result.stdout.decode('utf-8', errors='replace').strip().splitlines()[:4]


def java_executable(env):
    # Windows Oracle javapath can spawn a child JVM; own the real process for reliable teardown.
    result = subprocess.run(['java', '-XshowSettings:properties', '-version'], env=env,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    match = re.search(r'^\s*java.home\s*=\s*(.+)$', result.stdout.decode('utf-8', errors='replace'), re.M)
    if not match:
        raise GateError('Cannot resolve the actual Java runtime')
    path = Path(match.group(1).strip()) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
    if not path.is_file():
        raise GateError('Resolved Java executable is missing')
    return str(path)


def validate_runtime(evidence, required):
    checks = evidence.get('checks', {})
    if evidence.get('status') != 'PASS' or not required or not isinstance(checks, dict) or not all(checks.get(name) is True for name in required):
        raise GateError('Required HTTP / WebSocket / browser evidence is missing or failed')
    if any(value is not True for value in checks.values()):
        raise GateError('Runtime check contains a failed or invalid result')


def service_images(policy):
    result = {}
    for service, port in (('mysql', 23306), ('redis', 16379)):
        containers = subprocess.check_output(['docker', 'ps', '--filter', 'publish=' + str(port), '--format', '{{.ID}}']).decode().split()
        if len(containers) != 1:
            raise GateError('Exactly one owned Docker audit service required on port ' + str(port))
        image_id = subprocess.check_output(['docker', 'inspect', '--format', '{{.Image}}', containers[0]]).decode().strip()
        digests = json.loads(subprocess.check_output(['docker', 'image', 'inspect', '--format', '{{json .RepoDigests}}', image_id]))
        if policy['serviceImages'][service] not in digests:
            raise GateError('Audit service image differs from pinned policy: ' + service)
        result[service] = {'imageId': image_id, 'repoDigests': digests}
    return result


def wait_http(url, processes, timeout=70):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        if any(p.poll() is not None for p in processes):
            raise GateError('Verification service exited; inspect private runtime logs')
        try:
            with urllib.request.urlopen(url, timeout=2) as response:
                if response.status == 200:
                    return
        except Exception:
            time.sleep(.5)
    raise GateError('Verification service did not become ready')


def runtime_environment(base_env):
    def allowed(key):
        normalized = key.upper().replace('.', '_')
        return not (normalized.startswith(('SPRING_', 'MYSQL_', 'REDIS_', 'JWT_', 'SERVER_'))
                    or normalized in {'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'})
    env = {key: value for key, value in base_env.items() if allowed(key)}
    env.update(MYSQL_URL=base_env['WEEB_TEST_MYSQL_URL'],
               MYSQL_USERNAME=base_env.get('WEEB_TEST_MYSQL_USERNAME', 'root'),
               MYSQL_PASSWORD=base_env['WEEB_TEST_MYSQL_PASSWORD'], REDIS_HOST='127.0.0.1',
               REDIS_PORT=base_env['WEEB_TEST_REDIS_PORT'], REDIS_DATABASE='0', JWT_SECRET=secrets.token_urlsafe(64),
               SERVER_ADDRESS='127.0.0.1', SERVER_PORT='18080', SPRING_PROFILES_ACTIVE='prod',
               ALLOWED_ORIGINS='http://127.0.0.1:18081,http://127.0.0.1:18080',
               ELASTICSEARCH_ENABLED='false', PASSWORD_RESET_FRONTEND_URL='http://127.0.0.1:18081/reset-password')
    return env


def runtime_probe(source, output, chrome, base_env):
    for port in (18080, 18081, 18082):
        with socket.socket() as probe:
            probe.settimeout(.3)
            if probe.connect_ex(('127.0.0.1', port)) == 0:
                raise GateError('Owned runtime verification port is occupied: ' + str(port))
    # Deployment overrides must not redirect disposable runtime verification to another service.
    env = runtime_environment(base_env)
    processes = []
    handles = []
    try:
        for command, name in [([java_executable(env), '-jar', str(source / 'target/WEEB-0.0.1-SNAPSHOT.jar')], 'application.log'),
                              (['node', str(source / 'scripts/preview_server.mjs')], 'preview.log')]:
            handle = (output / name).open('wb'); handles.append(handle)
            processes.append(subprocess.Popen(command, cwd=source, env=env, stdout=handle, stderr=subprocess.STDOUT))
        wait_http('http://127.0.0.1:18080/ws/info', processes)
        wait_http('http://127.0.0.1:18081/login', processes)
        run(['node', 'scripts/runtime_probe.mjs', str(output / 'public'), str(chrome)], source, output / 'runtime-probe.log', env)
        evidence = json.loads((output / 'public/runtime.json').read_text())
        policy = json.loads((source / 'ci/release-policy.json').read_text())
        validate_runtime(evidence, policy['runtimeRequiredChecks'])
        return evidence
    finally:
        for process in reversed(processes):
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    process.kill(); process.wait(timeout=5)
        for handle in handles:
            handle.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--allow-dirty', action='store_true', help='Build an explicitly non-release review snapshot')
    parser.add_argument('--offline', action='store_true', help='Use populated Maven/npm dependency caches; fail if missing')
    parser.add_argument('--chrome', type=Path, default=os.environ.get('CHROME_BIN'), help='Chrome/Chromium executable for browser gate')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    dirty = bool(git(root, 'status', '--porcelain', '--untracked-files=all').strip())
    if dirty and not args.allow_dirty:
        raise GateError('Release candidate requires clean Git state; --allow-dirty produces a review snapshot only')
    env = os.environ.copy()
    for key in ('WEEB_TEST_MYSQL_URL', 'WEEB_TEST_MYSQL_PASSWORD', 'WEEB_TEST_REDIS_PORT'):
        if not env.get(key):
            raise GateError('Required integration setting absent: ' + key)
    if env['WEEB_TEST_REDIS_PORT'] != '16379':
        raise GateError('Use the dedicated loopback Redis audit service on 16379')
    if not args.chrome or not args.chrome.is_file():
        raise GateError('CHROME_BIN or --chrome must point to an installed browser')
    # Ambient build settings must not silently change frozen output or disable test execution.
    for key in list(env):
        if key.startswith('VITE_') or key in {'MAVEN_ARGS', 'MAVEN_OPTS', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'NODE_OPTIONS'}:
            del env[key]
    output = root / '.local/release' / (dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ-') + uuid.uuid4().hex[:8])
    (output / 'public').mkdir(parents=True)
    manifest = {'formatVersion': 1, 'status': 'FAIL', 'productionReady': False, 'sourceCommit': git(root, 'rev-parse', 'HEAD').decode().strip(),
                'worktreeClean': not dirty, 'kind': 'review_snapshot' if dirty else 'release_candidate', 'offlineDependencyCache': args.offline,
                'environment': {'os': platform.platform(), 'python': platform.python_version(),
                                'java': version(['java', '-version'], env), 'node': version(['node', '--version'], env)}}
    try:
        inventory = source_inventory(root)
        if not dirty and (git(root, 'status', '--porcelain', '--untracked-files=all').strip()
                          or git(root, 'rev-parse', 'HEAD').decode().strip() != manifest['sourceCommit']):
            raise GateError('Source changed while capturing the clean candidate')
        files = [{'path': name, 'sha256': digest(raw), 'mode': mode} for name, raw, mode in inventory]
        manifest['sourceTreeSha256'] = digest(json_bytes(files))
        write_json(output / 'public/source-files.json', files)
        a, b = output / 'source-a', output / 'source-b'
        snapshot(inventory, a); snapshot(inventory, b)
        run([sys.executable, 'scripts/color_audit.py', str(output / 'public/color-audit.json')], a, output / 'color-audit.log', env)
        manifest['colorAudit'] = json.loads((output / 'public/color-audit.json').read_text())
        policy = json.loads((a / 'ci/release-policy.json').read_text())
        manifest['serviceImages'] = service_images(policy)
        manifest['externalGates'] = {gate: 'UNVERIFIED' for gate in policy['externalGates']}
        timestamp = git(root, 'show', '-s', '--format=%cI', 'HEAD').decode().strip()
        manifest['buildOutputTimestamp'] = timestamp
        manifest['dependencyLocks'] = {path: digest((a / path).read_bytes()) for path in ('pom.xml', 'Vue/package-lock.json', '.mvn/wrapper/maven-wrapper.properties')}
        manifest['schemaManifestSha256'] = digest((a / 'src/main/resources/sql/schema-manifest.json').read_bytes())
        manifest['contractManifestSha256'] = digest((a / 'docs/contracts.json').read_bytes())
        npm = shutil.which('npm.cmd' if os.name == 'nt' else 'npm')
        manifest['environment']['npm'] = version([npm, '--version'], env)
        for source, suffix in ((a, 'a'), (b, 'b')):
            print('Building isolated snapshot ' + suffix, flush=True)
            maven = [str(source / ('mvnw.cmd' if os.name == 'nt' else 'mvnw'))]
            manifest['environment'].setdefault('maven', version(maven + ['-v'], env))
            common = (['-o'] if args.offline else []) + ['-B', '-Dproject.build.outputTimestamp=' + timestamp]
            run(maven + common + (['clean', 'verify'] if suffix == 'a' else ['clean', 'package', '-DskipTests']), source, output / ('maven-' + suffix + '.log'), env)
            run([npm, 'ci', '--no-audit', '--no-fund'] + (['--offline'] if args.offline else []), source / 'Vue', output / ('npm-ci-' + suffix + '.log'), env)
            if suffix == 'a':
                manifest['backendTests'] = junit_summary(sorted((a / 'target/surefire-reports').glob('TEST-*.xml')), policy['backendRequiredSuites'])
                run([npm, 'run', 'test:run', '--', '--reporter=junit', '--outputFile=vitest-junit.xml'], source / 'Vue', output / 'vitest.log', env)
                manifest['frontendTests'] = junit_summary([a / 'Vue/vitest-junit.xml'])
            run([npm, 'run', 'build'], source / 'Vue', output / ('vite-' + suffix + '.log'), env)
        first, second = artifacts(a), artifacts(b)
        if first != second:
            raise GateError('Independent build artifact hashes differ')
        manifest['artifacts'] = first
        with zipfile.ZipFile(a / 'target/WEEB-0.0.1-SNAPSHOT.jar') as archive:
            manifest['backendResolvedDependencies'] = {name: digest(archive.read(name)) for name in sorted(archive.namelist()) if name.startswith('BOOT-INF/lib/') and name.endswith('.jar')}
        manifest['reproducibility'] = 'PASS'
        # CLI validation must work in the executable jar without starting the application.
        migration_env = {**env, 'MYSQL_URL': env['WEEB_TEST_MYSQL_URL'], 'MYSQL_USERNAME': env.get('WEEB_TEST_MYSQL_USERNAME', 'root'), 'MYSQL_PASSWORD': env['WEEB_TEST_MYSQL_PASSWORD']}
        run(['java', '-Dloader.main=com.web.migration.SchemaMigrationCli', '-cp', str(a / 'target/WEEB-0.0.1-SNAPSHOT.jar'),
             'org.springframework.boot.loader.launch.PropertiesLauncher', '--validate'], a, output / 'schema-validation.log', migration_env)
        manifest['schemaVersions'] = json.loads((output / 'schema-validation.log').read_text(encoding='utf-8'))
        manifest['runtime'] = runtime_probe(a, output, args.chrome.resolve(), env)
        manifest['status'] = 'PASS'
    except Exception as error:
        manifest['failure'] = str(error) if isinstance(error, GateError) else type(error).__name__
        raise
    finally:
        write_json(output / 'public/release-manifest.json', manifest)
        print('Release evidence: ' + str(output / 'public/release-manifest.json'), flush=True)


if __name__ == '__main__':
    try:
        main()
    except Exception as failure:
        print(str(failure) if isinstance(failure, GateError) else type(failure).__name__, file=sys.stderr)
        sys.exit(1)
