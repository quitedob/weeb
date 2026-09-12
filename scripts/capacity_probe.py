#!/usr/bin/env python3
"""Single-JVM LOCAL capacity measurement; never a production-capacity certification.

Python stdlib + Docker + the JDK used by release_gate are sufficient. No Maven,
dependency installation, shared database deletion, Redis flush or container stop.
Examples (MYSQL_PASSWORD or WEEB_TEST_MYSQL_PASSWORD must be set privately):
  python scripts/capacity_probe.py --jar <frozen.jar>
  python scripts/capacity_probe.py --jar <after.jar> --fixture <private/fixture.json> --paged
  python scripts/capacity_probe.py --fixture <private/fixture.json> --drop-fixture
Only public/result.json is suitable for publication. Keep private/ and fixture.json
ignored: they contain disposable account credentials, SQL and JVM logs.
"""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
import ctypes
import datetime as dt
import hashlib
import http.client
import json
import math
import os
from pathlib import Path
import platform
import re
import secrets
import socket
import subprocess
import sys
import threading
import time
import urllib.parse
import uuid
import zipfile

from release_gate import java_executable


ROOT = Path(__file__).resolve().parents[1]
RUN_ROOT = ROOT / '.local' / 'capacity'
DB_PATTERN = re.compile(r'weeb_audit_capacity_[a-f0-9]{24}')
TOOL_VERSION = 1


class ProbeError(RuntimeError):
    pass


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=True, sort_keys=True, indent=2) + '\n', encoding='utf-8')


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def owned_path(path):
    value = Path(path).resolve()
    if not value.is_relative_to(RUN_ROOT.resolve()):
        raise ProbeError('Run and fixture paths must stay under ignored .local/capacity')
    return value


def validate_database(database):
    if not DB_PATTERN.fullmatch(database):
        raise ProbeError('Database is outside the randomly named capacity-fixture namespace')
    return database


def validate_audit_url(url):
    if not re.fullmatch(r'jdbc:mysql://(?:127\.0\.0\.1|localhost):23306/weeb_audit(?:\?.*)?', url):
        raise ProbeError('Only loopback23306/weeb_audit service configuration is accepted')


def validate_container(name, port):
    # Inspect only port mappings/image identity; never dump container environment.
    data = json.loads(subprocess.check_output(['docker', 'inspect', '--format', '{{json .NetworkSettings.Ports}}', name]))
    mappings = [entry for entries in data.values() if entries for entry in entries]
    if not any(entry['HostIp'] == '127.0.0.1' and entry['HostPort'] == str(port) for entry in mappings):
        raise ProbeError('Container does not own the required loopback audit port')
    image = subprocess.check_output(['docker', 'inspect', '--format', '{{.Image}}', name]).decode().strip()
    return {'port': port, 'imageId': image}


class Database:
    def __init__(self, container, private):
        self.container, self.private = container, private

    def execute(self, sql, database=None):
        if database is not None:
            validate_database(database)
        # MYSQL_ROOT_PASSWORD already belongs to this owned container; never place
        # its value on the host command line or in public evidence.
        command = ['docker', 'exec', '-i', self.container, 'sh', '-c',
                   'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --user=root --batch --raw --skip-column-names "$@"',
                   'capacity-mysql']
        if database:
            command.append(database)
        result = subprocess.run(command, input=sql.encode('utf-8'), stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=180)
        if result.returncode:
            (self.private / 'mysql-error.log').write_bytes(result.stderr)
            raise ProbeError('Owned MySQL operation failed; inspect private/mysql-error.log')
        return result.stdout.decode('utf-8').strip()

    def verify(self, fixture):
        database = validate_database(fixture['database'])
        marker = self.execute('SELECT run_id FROM capacity_probe_fixture', database)
        if marker != fixture['runId'] or database != 'weeb_audit_capacity_' + marker:
            raise ProbeError('Database ownership marker does not match the private fixture')


def run_private(command, env, cwd, logfile, timeout=180):
    with logfile.open('wb') as log:
        result = subprocess.run(command, env=env, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
    if result.returncode:
        raise ProbeError('Private subprocess failed: ' + logfile.name)


def require_free_port(port):
    if not 1024 <= port <= 65535:
        raise ProbeError('Invalid application port')
    with socket.socket() as probe:
        probe.settimeout(.5)
        if probe.connect_ex(('127.0.0.1', port)) == 0:
            raise ProbeError('Application port is occupied; no existing process will be stopped')


def app_environment(database, password, port, redis_db):
    validate_database(database)
    # Ambient Spring properties/JSON and JVM injection options can override the
    # audited database, Redis address, profile or measured heap. The owned JVM
    # and migration CLI receive only this run's explicit service configuration.
    def inherited_allowed(key):
        normalized = key.upper().replace('.', '_')
        return not (normalized.startswith(('SPRING_', 'MYSQL_', 'REDIS_', 'JWT_', 'SERVER_'))
                    or normalized in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS'))
    env = {key: value for key, value in os.environ.items() if inherited_allowed(key)}
    mysql_url = f'jdbc:mysql://127.0.0.1:23306/{database}?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true&useServerPrepStmts=true&cachePrepStmts=true'
    env.update(MYSQL_URL=mysql_url, SPRING_DATASOURCE_URL=mysql_url,
               SPRING_DATASOURCE_USERNAME='root', SPRING_DATASOURCE_PASSWORD=password,
               MYSQL_USERNAME='root', MYSQL_PASSWORD=password, REDIS_HOST='127.0.0.1', REDIS_PORT='16379',
               SPRING_DATA_REDIS_HOST='127.0.0.1', SPRING_DATA_REDIS_PORT='16379', SPRING_DATA_REDIS_PASSWORD='',
               SPRING_DATA_REDIS_DATABASE=str(redis_db), REDIS_PASSWORD='', JWT_SECRET=secrets.token_urlsafe(64),
               SPRING_PROFILES_ACTIVE='prod', SERVER_ADDRESS='127.0.0.1', SERVER_PORT=str(port),
               ALLOWED_ORIGINS=f'http://127.0.0.1:{port}', ELASTICSEARCH_ENABLED='false',
               PASSWORD_RESET_FRONTEND_URL=f'http://127.0.0.1:{port}/reset-password')
    return env


class Application:
    def __init__(self, java, jar, env, private, heap):
        self.java, self.jar, self.env, self.private, self.heap = java, jar, env, private, heap
        self.process = self.log = None

    def __enter__(self):
        require_free_port(int(self.env['SERVER_PORT']))
        self.log = (self.private / 'application.log').open('wb')
        try:
            self.process = subprocess.Popen([self.java, '-Xms256m', '-Xmx' + self.heap, '-jar', str(self.jar)],
                                            cwd=self.private, env=self.env, stdout=self.log, stderr=subprocess.STDOUT)
            deadline = time.monotonic() + 90
            while time.monotonic() < deadline:
                if self.process.poll() is not None:
                    raise ProbeError('Owned JVM exited during startup; inspect private/application.log')
                try:
                    connection = http.client.HTTPConnection('127.0.0.1', int(self.env['SERVER_PORT']), timeout=2)
                    connection.request('GET', '/ws/info')
                    response = connection.getresponse()
                    response.read()
                    connection.close()
                    if response.status == 200:
                        return self
                except (OSError, http.client.HTTPException):
                    pass
                time.sleep(.5)
            raise ProbeError('Owned JVM readiness timed out')
        except BaseException:
            self.__exit__(None, None, None)
            raise

    def __exit__(self, *_):
        if self.process and self.process.poll() is None:
            self.process.terminate()
            try:
                self.process.wait(timeout=12)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=5)
        if self.log:
            self.log.close()


def request(port, path, token=None, body=None, timeout=15):
    headers = {'Accept': 'application/json', 'Accept-Encoding': 'identity'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    payload = None if body is None else json.dumps(body).encode()
    if payload is not None:
        headers['Content-Type'] = 'application/json'
    started = time.perf_counter()
    connection = http.client.HTTPConnection('127.0.0.1', port, timeout=timeout)
    try:
        connection.request('GET' if body is None else 'POST', path, payload, headers)
        response = connection.getresponse()
        raw = response.read()
        elapsed = (time.perf_counter() - started) * 1000
        try:
            result = json.loads(raw)
        except (ValueError, UnicodeDecodeError):
            result = None
        sample = {'latencyMs': elapsed, 'httpStatus': response.status, 'payloadBytes': len(raw),
                  'businessCode': result.get('code') if isinstance(result, dict) else None,
                  'success': response.status == 200 and isinstance(result, dict) and result.get('code') == 0}
        return sample, result
    except (OSError, http.client.HTTPException) as error:
        return {'latencyMs': (time.perf_counter() - started) * 1000, 'httpStatus': 'transport_error',
                'payloadBytes': 0, 'businessCode': None, 'success': False, 'errorType': type(error).__name__}, None
    finally:
        connection.close()


def account(port, username, password, register=False):
    body = {'username': username, 'password': password}
    if register:
        body.update(confirmPassword=password, email=username + '@example.invalid')
    sample, response = request(port, '/api/auth/' + ('register' if register else 'login'), body=body)
    if not sample['success'] or not response['data'].get('token'):
        raise ProbeError('Fixture account HTTP authentication failed (no credentials included)')
    return response['data']


def dataset_sql(fixture):
    """Deterministic database-side bulk generation, bounded to this owned database."""
    messages, groups = fixture['messages'], fixture['groups']
    actor, peer = fixture['actorId'], fixture['peerId']
    # IDs are stable inside the fixture. Users use high unique IDs because Redis is
    # shared infrastructure even though this process selects logical database15.
    count = max(messages, groups)
    digits = max(1, len(str(count - 1)))
    numbers = ' UNION ALL '.join(f'SELECT {i} AS n' for i in range(10))
    aliases = [f'd{i}' for i in range(digits)]
    sequence = ' + '.join(f'{name}.n*{10 ** i}' for i, name in enumerate(aliases))
    source = ' CROSS JOIN '.join(f'({numbers}) {name}' for name in aliases)
    return f"""CREATE TEMPORARY TABLE capacity_sequence(n INT PRIMARY KEY);
INSERT INTO capacity_sequence SELECT {sequence} FROM {source} WHERE ({sequence}) < {count};
INSERT INTO `group`(id,group_name,owner_id,group_description,status,max_members,member_count,is_visible,shared_chat_id,create_time,update_time)
SELECT 10001+n,CONCAT('Capacity group ',LPAD(n,6,'0')),IF(MOD(n,2)=0,{actor},{peer}),REPEAT('Fixture group description. ',8),1,500,2,1,20001+n,'2025-01-01','2025-01-01' FROM capacity_sequence WHERE n<{groups};
INSERT INTO shared_chat(id,chat_type,participant_1_id,participant_2_id,created_at,updated_at)
VALUES(101,'PRIVATE',LEAST({actor},{peer}),GREATEST({actor},{peer}),'2025-01-01','2025-01-01');
INSERT INTO shared_chat(id,chat_type,group_id,created_at,updated_at)
SELECT 20001+n,'GROUP',10001+n,'2025-01-01','2025-01-01' FROM capacity_sequence WHERE n<{groups};
INSERT INTO group_member(group_id,user_id,role,join_status,join_time)
SELECT id,{actor},IF(owner_id={actor},1,3),'ACCEPTED','2025-01-01' FROM `group`;
INSERT INTO group_member(group_id,user_id,role,join_status,join_time)
SELECT id,{peer},IF(owner_id={peer},1,3),'ACCEPTED','2025-01-01' FROM `group`;
INSERT INTO chat_list(id,user_id,shared_chat_id,group_id,target_id,target_info,type,unread_count,last_message,create_time,update_time)
SELECT CONCAT(shared_chat_id,'_',gm.user_id),gm.user_id,g.shared_chat_id,g.id,NULL,g.group_name,'GROUP',0,'','2025-01-01','2025-01-01'
FROM `group` g JOIN group_member gm ON gm.group_id=g.id;
INSERT INTO chat_list(id,user_id,shared_chat_id,target_id,target_info,type,unread_count,create_time,update_time)
VALUES(CONCAT('101_',{actor}),{actor},101,{peer},'Capacity peer','PRIVATE',0,'2025-01-01','2025-01-01'),
(CONCAT('101_',{peer}),{peer},101,{actor},'Capacity actor','PRIVATE',0,'2025-01-01','2025-01-01');
INSERT INTO message(id,client_message_id,sender_id,receiver_id,group_id,chat_id,content,message_type,status,is_recalled,created_at,updated_at)
SELECT 1000001+n,CONCAT('capacity-',n),IF(MOD(n,3)=0,{actor},{peer}),
IF(MOD(n,2)=0,IF(MOD(n,3)=0,{peer},{actor}),NULL),
IF(MOD(n,2)=1,10001+MOD(FLOOR(n/2),{groups}),NULL),
IF(MOD(n,2)=0,101,20001+MOD(FLOOR(n/2),{groups})),
JSON_OBJECT('content',CONCAT(IF(MOD(n,10)=0,'capacity alpha ',IF(MOD(n,10)=1,'capacity beta ','ordinary discussion ')),LPAD(n,7,'0'),' ',REPEAT('deterministic fixture text ',8)), 'contentType',1,'atUidList',JSON_ARRAY()),
1,1,0,TIMESTAMPADD(SECOND,n,'2025-01-01 00:00:00'),TIMESTAMPADD(SECOND,n,'2025-01-01 00:00:00')
FROM capacity_sequence WHERE n<{messages};
INSERT INTO chat_unread_count(user_id,chat_id,unread_count,last_read_message_id)
SELECT {actor},chat_id,COUNT(*),0 FROM message WHERE sender_id<>{actor} GROUP BY chat_id;
UPDATE chat_list cl LEFT JOIN chat_unread_count uc ON uc.chat_id=cl.shared_chat_id AND uc.user_id=cl.user_id
SET cl.unread_count=COALESCE(uc.unread_count,0);
UPDATE capacity_probe_fixture SET dataset_sha256='{fixture['datasetSha256']}',message_count={messages},group_count={groups};
DROP TEMPORARY TABLE capacity_sequence;
"""


def verify_dataset(db, fixture):
    db.verify(fixture)
    values = db.execute("SELECT COUNT(*),COALESCE(MIN(id),0),COALESCE(MAX(id),0),COALESCE(SUM(id),0) FROM message; SELECT COUNT(*) FROM `group`", fixture['database']).splitlines()
    expected_sum = fixture['messages'] * (2 * 1000001 + fixture['messages'] - 1) // 2
    expected = f"{fixture['messages']}\t1000001\t{1000000 + fixture['messages']}\t{expected_sum}"
    if values != [expected, str(fixture['groups'])]:
        raise ProbeError('Fixture cardinality/ID checksum changed; refuse same-data comparison')
    fingerprint = db.execute("SELECT COALESCE(SUM(CRC32(CONCAT_WS('#',id,sender_id,receiver_id,group_id,chat_id,content,message_type,is_recalled))),0) FROM message; "
                             "SELECT COALESCE(SUM(CRC32(CONCAT_WS('#',id,group_name,owner_id,status,is_visible,member_count))),0) FROM `group`; "
                             "SELECT COALESCE(SUM(CRC32(CONCAT_WS('#',group_id,user_id,role,join_status,kicked_at))),0) FROM group_member", fixture['database']).splitlines()
    if fixture.get('dataFingerprint') and fingerprint != fixture['dataFingerprint']:
        raise ProbeError('Fixture content or membership fingerprint changed; refuse same-data comparison')
    fixture['dataFingerprint'] = fingerprint
    return {'messageCount': fixture['messages'], 'groupCount': fixture['groups'], 'messageIdSum': expected_sum,
            'ownedGroups': (fixture['groups'] + 1) // 2, 'joinedOtherGroups': fixture['groups'] // 2,
            'privateMessageCount': (fixture['messages'] + 1) // 2,
            'groupMessageCount': fixture['messages'] // 2, 'generatorSha256': fixture['datasetSha256'],
            'dataFingerprint': fingerprint, 'fingerprintMethod': 'SQL SUM(CRC32(canonical message/group/member values)); cardinality and ID sum also checked'}


def endpoints(paged, page_size=10):
    paging = f'?page=0&size={page_size}' if paged else ''
    return [('search', f'/api/search/messages?q=capacity%20alpha&page=0&size={page_size}&sortBy=relevance'),
            ('history', f'/api/chats/101/messages?page=1&size={page_size}'),
            ('unread', '/api/chats/unread/stats'),
            ('owned_groups', '/api/groups/my-created' + paging),
            ('joined_groups', '/api/groups/my-groups' + (paging + '&excludeOwned=true' if paged else ''))]


def percentile(values, percent):
    """Nearest-rank percentile; a small sample is reported, never hidden."""
    if not values:
        return None
    ordered = sorted(values)
    return round(ordered[max(0, math.ceil(percent / 100 * len(ordered)) - 1)], 3)


def summarize(samples, seconds):
    latencies = [sample['latencyMs'] for sample in samples]
    errors = sum(not sample['success'] for sample in samples)
    return {'requests': len(samples), 'errors': errors, 'errorRate': errors / len(samples) if samples else None,
            'statusCounts': dict(Counter(str(s['httpStatus']) for s in samples)),
            'businessCodeCounts': dict(Counter(str(s['businessCode']) for s in samples)),
            'contractErrors': sum(s.get('contractValid') is False for s in samples),
            'privacyErrors': sum(s.get('privacyValid') is False for s in samples),
            'p50Ms': percentile(latencies, 50), 'p95Ms': percentile(latencies, 95), 'p99Ms': percentile(latencies, 99),
            'wallSeconds': round(seconds, 4), 'requestsPerSecond': round(len(samples) / seconds, 3) if seconds else None,
            'payloadBytesTotal': sum(s['payloadBytes'] for s in samples),
            'payloadBytesMean': round(sum(s['payloadBytes'] for s in samples) / len(samples), 2) if samples else None}


def sensitive_response_fields(value):
    if isinstance(value, dict):
        return any((key.lower().replace('_', '') in {'password', 'token', 'accesstoken', 'refreshtoken', 'authorization', 'jwtsecret'}
                    and child not in (None, '')) or sensitive_response_fields(child) for key, child in value.items())
    if isinstance(value, list):
        return any(sensitive_response_fields(child) for child in value)
    return False


def check_response(sample, body, name, fixture, paged, size):
    data = body.get('data') if isinstance(body, dict) else None
    valid = False
    if name == 'search':
        valid = isinstance(data, dict) and isinstance(data.get('list'), list) and 0 < len(data['list']) <= size and data.get('total', 0) > 0
    elif name == 'history':
        valid = isinstance(data, list) and 0 < len(data) <= size
    elif name == 'unread':
        valid = isinstance(data, dict) and isinstance(data.get('unreadList'), list) and data.get('totalUnread', 0) > 0
    elif name in ('owned_groups', 'joined_groups'):
        expected = (fixture['groups'] + 1) // 2 if name == 'owned_groups' else fixture['groups'] // 2 if paged else fixture['groups']
        valid = (isinstance(data, dict) and data.get('total') == expected and data.get('page') == 0 and data.get('size') == size
                 and isinstance(data.get('list'), list) and len(data['list']) == min(size, expected)) if paged else isinstance(data, list) and len(data) == expected
    privacy = not sensitive_response_fields(body)
    return {**sample, 'contractValid': valid, 'privacyValid': privacy, 'success': sample['success'] and valid and privacy}


def process_usage(pid):
    if os.name == 'nt':
        from ctypes import wintypes
        kernel, psapi = ctypes.WinDLL('kernel32', use_last_error=True), ctypes.WinDLL('psapi', use_last_error=True)
        kernel.OpenProcess.restype = wintypes.HANDLE
        kernel.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
        kernel.CloseHandle.argtypes = [wintypes.HANDLE]
        class Memory(ctypes.Structure):
            _fields_ = [('cb', wintypes.DWORD), ('faults', wintypes.DWORD)] + [(name, ctypes.c_size_t) for name in
                ('peakWorkingSet', 'workingSet', 'peakPagedPool', 'pagedPool', 'peakNonPagedPool', 'nonPagedPool', 'pagefile', 'peakPagefile')]
        handle = kernel.OpenProcess(0x0410, False, pid)
        if not handle:
            raise OSError('Cannot query owned JVM')
        try:
            times = [wintypes.FILETIME() for _ in range(4)]
            kernel.GetProcessTimes.argtypes = [wintypes.HANDLE] + [ctypes.POINTER(wintypes.FILETIME)] * 4
            if not kernel.GetProcessTimes(handle, *[ctypes.byref(value) for value in times]):
                raise OSError('Cannot read process CPU times')
            memory = Memory(); memory.cb = ctypes.sizeof(memory)
            psapi.GetProcessMemoryInfo.argtypes = [wintypes.HANDLE, ctypes.POINTER(Memory), wintypes.DWORD]
            if not psapi.GetProcessMemoryInfo(handle, ctypes.byref(memory), memory.cb):
                raise OSError('Cannot read process memory')
            cpu = sum((value.dwHighDateTime << 32) + value.dwLowDateTime for value in times[2:]) / 1e7
            return {'cpuSeconds': cpu, 'rssBytes': memory.workingSet}
        finally:
            kernel.CloseHandle(handle)
    if sys.platform.startswith('linux'):
        fields = Path(f'/proc/{pid}/stat').read_text().rsplit(')', 1)[1].split()
        rss = int(re.search(r'^VmRSS:\s+(\d+)', Path(f'/proc/{pid}/status').read_text(), re.M)[1]) * 1024
        return {'cpuSeconds': (int(fields[11]) + int(fields[12])) / os.sysconf('SC_CLK_TCK'), 'rssBytes': rss}
    raise OSError('Process CPU/RSS sampler supports Windows and Linux only')


class ResourceSampler:
    def __init__(self, pid):
        self.pid, self.samples, self.errors = pid, [], []
        self.stop = threading.Event()
        self.thread = threading.Thread(target=self.loop, daemon=True)

    def loop(self):
        while True:
            try:
                self.samples.append({'monotonic': time.monotonic(), **process_usage(self.pid)})
            except OSError as error:
                self.errors.append(type(error).__name__)
            if self.stop.wait(.5):
                break

    def finish(self):
        self.stop.set(); self.thread.join(timeout=3)
        try:
            self.samples.append({'monotonic': time.monotonic(), **process_usage(self.pid)})
        except OSError as error:
            self.errors.append(type(error).__name__)
        if not self.samples:
            return {'status': 'UNAVAILABLE', 'reason': 'Process query failed', 'errorTypes': sorted(set(self.errors))}
        first, last = self.samples[0], self.samples[-1]
        elapsed = last['monotonic'] - first['monotonic']
        return {'status': 'RECORDED', 'samples': len(self.samples), 'samplingIntervalMs': 500,
                'peakObservedRssBytes': max(s['rssBytes'] for s in self.samples),
                'cpuSeconds': round(last['cpuSeconds'] - first['cpuSeconds'], 4),
                'cpuPercentOneCore': round(100 * (last['cpuSeconds'] - first['cpuSeconds']) / elapsed, 2) if elapsed > 0 else None,
                'note': 'Observed process RSS and CPU; CPU100% means one logical core, not whole-machine saturation.'}


def measure(port, token, routes, concurrency, count, timeout, pid, fixture, paged, page_size):
    sampler = ResourceSampler(pid)
    sampler.thread.start()
    def one(index):
        name, path = routes[index % len(routes)]
        sample, body = request(port, path, token, timeout=timeout)
        return {'endpoint': name, **check_response(sample, body, name, fixture, paged, page_size)}
    started = time.perf_counter()
    try:
        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            samples = list(executor.map(one, range(count)))
        elapsed = time.perf_counter() - started
    finally:
        resources = sampler.finish()
    return {'concurrency': concurrency, 'summary': summarize(samples, elapsed), 'resources': resources,
            'endpoints': {name: summarize([s for s in samples if s['endpoint'] == name], elapsed) for name, _ in routes},
            'samples': samples}


QUERY_CAPTURE = r'''import java.util.*;
import com.web.service.MessageSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
public class QueryCapture {
  static final List<Map<String,Object>> result = new ArrayList<>();
  static class Capture extends NamedParameterJdbcTemplate {
    Capture() { super(new DriverManagerDataSource()); }
    void add(String sql, SqlParameterSource params) {
      result.add(Map.of("sql",sql,"parameters",((MapSqlParameterSource)params).getValues()));
    }
    @Override public <T> T queryForObject(String sql, SqlParameterSource params, Class<T> type) {
      add(sql,params); return type.cast(Long.valueOf(0));
    }
    @Override public <T> List<T> query(String sql, SqlParameterSource params, RowMapper<T> mapper) {
      add(sql,params); return List.of();
    }
  }
  public static void main(String[] args) throws Exception {
    new MessageSearchService(new Capture()).search(Long.valueOf(args[0]),"capacity alpha",0,10,null,null,null,null,null,"relevance");
    System.out.println("CAPACITY_QUERY_JSON="+new ObjectMapper().writeValueAsString(result));
  }
}
'''


def bind_captured_sql(sql, parameters):
    def literal(value):
        if isinstance(value, bool):
            return '1' if value else '0'
        if isinstance(value, int):
            return str(value)
        if isinstance(value, str) and '\\' not in value and '\x00' not in value:
            return "'" + value.replace("'", "''") + "'"
        raise ProbeError('Unsupported captured parameter type; do not guess its SQL representation')
    # Protect quoted SQL literals/identifiers. Only parameters outside them expand.
    tokens = re.compile(r"'(?:''|[^'])*'|\"(?:\"\"|[^\"])*\"|`[^`]*`|:([A-Za-z][A-Za-z0-9_]*)")
    return tokens.sub(lambda match: literal(parameters[match[1]]) if match[1] else match[0], sql)


def query_profile(jar, java, env, private, db, fixture):
    directory = private / 'query-capture'; directory.mkdir()
    with zipfile.ZipFile(jar) as archive:
        for name in archive.namelist():
            if name.startswith(('BOOT-INF/classes/', 'BOOT-INF/lib/')) and not name.endswith('/'):
                target = (directory / name).resolve()
                if not target.is_relative_to(directory.resolve()):
                    raise ProbeError('Unsafe archive member')
                target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(archive.read(name))
    source = directory / 'QueryCapture.java'; source.write_text(QUERY_CAPTURE, encoding='utf-8')
    classpath = os.pathsep.join([str(directory), str(directory / 'BOOT-INF/classes'), str(directory / 'BOOT-INF/lib/*')])
    javac = str(Path(java).with_name('javac.exe' if os.name == 'nt' else 'javac'))
    run_private([javac, '-encoding', 'UTF-8', '-cp', classpath, str(source)], env, directory, private / 'query-compile.log')
    run_private([java, '-cp', classpath, 'QueryCapture', str(fixture['actorId'])], env, directory, private / 'query-capture.log')
    lines = (private / 'query-capture.log').read_text(encoding='utf-8').splitlines()
    captured = json.loads(next(line.split('=', 1)[1] for line in lines if line.startswith('CAPACITY_QUERY_JSON=')))
    if len(captured) != 2 or any(not item['sql'].strip().upper().startswith('SELECT ') for item in captured):
        raise ProbeError('Unexpected production search builder contract; no guessed query will be profiled')
    records = []
    for item in captured:
        statement = bind_captured_sql(item['sql'], item['parameters'])
        plan = json.loads(db.execute('EXPLAIN FORMAT=JSON ' + statement, fixture['database']))
        records.append({**item, 'explain': plan})
    write_json(private / 'captured-search.json', records)
    return {'status': 'RECORDED', 'method': 'Actual selected-JAR MessageSearchService builder; capture-only JDBC subclass; EXPLAIN FORMAT=JSON',
            'harnessSha256': hashlib.sha256(QUERY_CAPTURE.encode()).hexdigest(), 'queries': records}


def jar_provenance(jar):
    digest = sha256(jar)
    result = {'jarSha256': digest, 'jarBytes': jar.stat().st_size, 'source': {'status': 'UNAVAILABLE', 'reason': 'No matching release manifest'}}
    manifest = jar.parent.parent.parent / 'public/release-manifest.json'
    if manifest.is_file():
        data = json.loads(manifest.read_text(encoding='utf-8'))
        if data.get('artifacts', {}).get('backendJarSha256') == digest:
            result['source'] = {'status': 'MATCHED_RELEASE_MANIFEST', 'manifestSha256': sha256(manifest),
                                **{key: data.get(key) for key in ('sourceCommit', 'sourceTreeSha256', 'worktreeClean')}}
    with zipfile.ZipFile(jar) as archive:
        result['schemaManifestSha256'] = hashlib.sha256(archive.read('BOOT-INF/classes/sql/schema-manifest.json')).hexdigest()
    return result


def docker_stats(containers):
    try:
        result = subprocess.run(['docker', 'stats', '--no-stream', '--format', '{{json .}}', *containers],
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=15)
        if result.returncode:
            raise OSError('Docker stats failed')
        rows = [json.loads(line) for line in result.stdout.decode().splitlines()]
        return {'status': 'RECORDED', 'containers': [{key: row.get(key) for key in ('Name', 'CPUPerc', 'MemUsage', 'MemPerc', 'PIDs')} for row in rows]}
    except (OSError, ValueError, subprocess.TimeoutExpired):
        return {'status': 'UNAVAILABLE', 'reason': 'Docker stats query unsupported or failed'}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path)
    parser.add_argument('--fixture', type=Path, help='Reuse the same private fixture.json and its retained database')
    parser.add_argument('--drop-fixture', action='store_true', help='Drop only the matching capacity database; never Redis or containers')
    parser.add_argument('--messages', type=int, default=100000)
    parser.add_argument('--groups', type=int, default=1000, help='Half owned, half joined under the fixture peer')
    parser.add_argument('--requests', type=int, default=75, help='Measured requests per stage; counts are evenly mixed over5 endpoints')
    parser.add_argument('--concurrency', default='1,5,20')
    parser.add_argument('--warmup', type=int, default=5, help='Separate sequential warmup requests before each stage')
    parser.add_argument('--timeout', type=float, default=15)
    parser.add_argument('--port', type=int, default=18080)
    parser.add_argument('--redis-db', type=int, default=15)
    parser.add_argument('--heap', default='1g')
    parser.add_argument('--paged', action='store_true')
    parser.add_argument('--page-size', type=int, default=10)
    parser.add_argument('--p95-target-ms', type=float, default=1000)
    parser.add_argument('--docker-stats', action='store_true')
    parser.add_argument('--skip-explain', action='store_true')
    parser.add_argument('--mysql-container', default='weeb-audit-mysql-20260911')
    parser.add_argument('--redis-container', default='weeb-audit-redis-20260911')
    args = parser.parse_args(argv)
    concurrency = [int(value) for value in args.concurrency.split(',')]
    if not concurrency or any(value < 1 or value > 20 for value in concurrency):
        parser.error('Concurrency must contain values1..20 (in-flight HTTP requests, not users)')
    if not (1 <= args.messages <= 1000000 and 2 <= args.groups <= 10000 and 5 <= args.requests <= 10000
            and args.requests % 5 == 0 and 0 <= args.warmup <= 100 and 1 <= args.timeout <= 60
            and 1 <= args.redis_db <= 15 and 1 <= args.page_size <= 100 and args.p95_target_ms > 0):
        parser.error('Dataset, sampling or timeout bounds invalid')
    if not re.fullmatch(r'(?:[1-9][0-9]*m|[1-4]g)', args.heap) or (args.heap.endswith('m') and not 256 <= int(args.heap[:-1]) <= 4096):
        parser.error('Heap must be a bounded JVM size such as512m or1g')
    validate_audit_url(os.environ.get('WEEB_TEST_MYSQL_URL', 'jdbc:mysql://127.0.0.1:23306/weeb_audit'))
    if os.environ.get('WEEB_TEST_REDIS_PORT', '16379') != '16379':
        raise ProbeError('Only the loopback16379 audit Redis is accepted')
    run_id = uuid.uuid4().hex[:24]
    output = RUN_ROOT / (dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + run_id[:8])
    private = output / 'private'; private.mkdir(parents=True)
    db = Database(args.mysql_container, private)
    images = {'mysql': validate_container(args.mysql_container, 23306), 'redis': validate_container(args.redis_container, 16379)}
    if args.fixture:
        fixture = json.loads(owned_path(args.fixture).read_text(encoding='utf-8'))
        db.verify(fixture)
    else:
        if args.drop_fixture:
            parser.error('--drop-fixture requires an existing private fixture')
        fixture = {'runId': run_id, 'database': 'weeb_audit_capacity_' + run_id, 'messages': args.messages, 'groups': args.groups}
    if args.drop_fixture:
        db.execute('DROP DATABASE `' + validate_database(fixture['database']) + '`')
        print(json.dumps({'status': 'OWNED_DATABASE_DROPPED', 'runId': fixture['runId']}))
        return 0
    if args.fixture and not fixture.get('seedComplete'):
        raise ProbeError('Incomplete fixture cannot be reused; preserve for diagnosis or explicitly drop its owned database')
    if not args.jar or not args.jar.resolve().is_file():
        parser.error('--jar must identify an existing packaged JAR; this tool never builds one')
    jar = args.jar.resolve()
    password = os.environ.get('WEEB_TEST_MYSQL_PASSWORD') or os.environ.get('MYSQL_PASSWORD')
    if not password:
        raise ProbeError('Set audit MySQL password through the environment, never command-line arguments')
    env = app_environment(fixture['database'], password, args.port, args.redis_db)
    java = java_executable(env)
    if not args.fixture:
        db.execute('CREATE DATABASE `' + validate_database(fixture['database']) + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
        db.execute('CREATE TABLE capacity_probe_fixture(run_id VARCHAR(24) PRIMARY KEY,dataset_sha256 CHAR(64),message_count INT,group_count INT); '
                   + "INSERT INTO capacity_probe_fixture(run_id) VALUES ('" + run_id + "')", fixture['database'])
        write_json(private / 'fixture.json', fixture)
    run_private([java, '-Dloader.main=com.web.migration.SchemaMigrationCli', '-cp', str(jar),
                 'org.springframework.boot.loader.launch.PropertiesLauncher', '--apply'], env, private, private / 'migration.log')
    if not args.fixture:
        first_user = 1000000000000 + int(run_id[:10], 16)
        db.execute(f'ALTER TABLE `user` AUTO_INCREMENT={first_user}', fixture['database'])
    report = {'formatVersion': TOOL_VERSION, 'scope': 'LOCAL_SINGLE_JVM_BASELINE', 'productionCapacityVerified': False,
              'startedAtUtc': dt.datetime.now(dt.timezone.utc).isoformat(), 'artifact': jar_provenance(jar),
              'toolSha256': sha256(Path(__file__)), 'host': {'os': platform.system(), 'release': platform.release(), 'logicalCpuCount': os.cpu_count()},
              'serviceImages': images, 'configuration': {'heapMin': '256m', 'heapMax': args.heap, 'profile': 'prod',
                  'redisLogicalDatabase': args.redis_db, 'paging': args.paged, 'pageSize': args.page_size, 'httpPort': args.port},
              'method': {'concurrencyStages': concurrency, 'measuredRequestsPerStage': args.requests,
                  'warmupRequestsPerStage': args.warmup, 'timeoutSeconds': args.timeout, 'percentile': 'nearest-rank',
                  'load': 'closed-loop thread pool, one authenticated actor, equal endpoint mix, no think time, new HTTP connection per request',
                  'latency': 'client elapsed from connect/request through full response-body read; warmup excluded',
                  'payload': 'response body bytes, Accept-Encoding identity; excludes headers',
                  'rateLimits': 'Production per-user/IP/path protections retained; large runs may deliberately measure limiting errors',
                  'pagingDifference': 'legacy my-groups includes owned groups; paged joined route uses excludeOwned=true'},
              'localTarget': {'p95MsMaximum': args.p95_target_ms, 'errorRateMaximum': 0, 'note': 'Local chosen target; not a user SLA or max-user capacity'},
              'stages': []}
    report['backendJarSha256'] = report['artifact']['jarSha256']
    report['sourceTreeSha256'] = report['artifact']['source'].get('sourceTreeSha256')
    containers = [args.mysql_container, args.redis_container]
    try:
        with Application(java, jar, env, private, args.heap) as application:
            print(json.dumps({'event': 'owned_jvm_started', 'pid': application.process.pid, 'port': args.port, 'runDirectory': str(output)}), flush=True)
            if not args.fixture:
                fixture['username'] = 'cap_' + run_id[:12]
                fixture['password'] = 'Aa9!' + secrets.token_hex(14)
                peer_name = 'cap_' + run_id[12:24]
                a = account(args.port, fixture['username'], fixture['password'], True)
                b = account(args.port, peer_name, 'Aa9!' + secrets.token_hex(14), True)
                fixture.update(actorId=a['user']['id'], peerId=b['user']['id'], datasetSha256='0' * 64)
                # Hash the actual deterministic seed SQL before filling its own checksum field.
                fixture['datasetSha256'] = hashlib.sha256(dataset_sql(fixture).encode()).hexdigest()
                write_json(private / 'fixture.json', fixture)
                sql = dataset_sql(fixture)
                (private / 'dataset.sql').write_text(sql, encoding='utf-8')
                db.execute(sql, fixture['database'])
                fixture['seedComplete'] = True
            else:
                write_json(private / 'fixture.json', fixture)
            report['dataset'] = verify_dataset(db, fixture)
            write_json(private / 'fixture.json', fixture)
            report['dataset']['fixtureRunId'] = fixture['runId']
            token = account(args.port, fixture['username'], fixture['password'])['token']
            report['dockerBefore'] = docker_stats(containers) if args.docker_stats else {'status': 'NOT_COLLECTED', 'reason': '--docker-stats not selected'}
            routes = endpoints(args.paged, args.page_size)
            report['method']['endpoints'] = dict(routes)
            for level in concurrency:
                print(json.dumps({'event': 'stage_started', 'concurrency': level}), flush=True)
                warmup = []
                for index in range(args.warmup):
                    name, path = routes[index % len(routes)]
                    sample, body = request(args.port, path, token, timeout=args.timeout)
                    warmup.append(check_response(sample, body, name, fixture, args.paged, args.page_size))
                stage = measure(args.port, token, routes, level, args.requests, args.timeout, application.process.pid, fixture, args.paged, args.page_size)
                stage['warmup'] = summarize(warmup, sum(s['latencyMs'] for s in warmup) / 1000)
                stage['localTargetMet'] = (stage['summary']['errors'] == 0 and stage['summary']['p95Ms'] <= args.p95_target_ms
                    and all(value['requests'] > 0 and value['errors'] == 0 and value['p95Ms'] <= args.p95_target_ms for value in stage['endpoints'].values()))
                report['stages'].append(stage)
                write_json(output / 'public/result.json', report)
                print(json.dumps({'event': 'stage_finished', 'concurrency': level, **stage['summary']}), flush=True)
            report['dockerAfter'] = docker_stats(containers) if args.docker_stats else {'status': 'NOT_COLLECTED', 'reason': '--docker-stats not selected'}
        # Query analysis runs after the measured JVM stops, so javac/EXPLAIN do not contend with timings.
        report['jvmStopped'] = application.process.poll() is not None
        report['searchQueryProfile'] = {'status': 'NOT_COLLECTED', 'reason': '--skip-explain selected'} if args.skip_explain else query_profile(jar, java, env, private, db, fixture)
        report['datasetAfter'] = verify_dataset(db, fixture)
        report['allLocalTargetsMet'] = len(report['stages']) == len(concurrency) and all(stage['localTargetMet'] for stage in report['stages'])
        public_text = json.dumps(report)
        safe = all(value not in public_text for value in (token, fixture['password'], password, env['JWT_SECRET']))
        report['responsePrivacyChecks'] = {'checkedMeasuredResponses': sum(s['summary']['requests'] for s in report['stages']),
            'sensitiveResponseFieldErrors': sum(s['summary']['privacyErrors'] for s in report['stages']),
            'publicContainsNoFixturePasswordsOrBearerTokens': safe,
            'scope': 'Response sensitive-field detection and exact secret exclusion; not a substitute for object-authorization tests'}
        report['status'] = 'PASS' if report['allLocalTargetsMet'] and safe and report['jvmStopped'] else 'FAIL'
    except BaseException as error:
        report['status'] = 'FAIL'
        report['failureType'] = type(error).__name__
        raise
    finally:
        report['completedAtUtc'] = dt.datetime.now(dt.timezone.utc).isoformat()
        report['retainedOwnedFixture'] = True
        write_json(output / 'public/result.json', report)
        print(json.dumps({'event': 'results_written', 'status': report.get('status'), 'publicResult': str(output / 'public/result.json'),
                          'privateFixture': str(private / 'fixture.json')}), flush=True)
    return 0 if report['status'] == 'PASS' else 1


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ProbeError, OSError, subprocess.SubprocessError, ValueError) as error:
        # Never print underlying JDBC/command/environment errors that might contain secrets.
        print(json.dumps({'status': 'INCOMPLETE', 'errorType': type(error).__name__, 'message': 'Capacity probe failed; inspect the ignored private logs.'}), file=sys.stderr)
        sys.exit(1)
