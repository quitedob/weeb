// Disposable real HTTP/STOMP/browser acceptance. Never write credentials or wire payloads.
import { createRequire } from 'node:module';
import { randomBytes, randomInt, randomUUID } from 'node:crypto';
import { spawn } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { campusFixture, finishCampusFixture } from './campus_runtime_probe.mjs';
import { campusBrowser } from './campus_browser_probe.mjs';
const require = createRequire(new URL('../Vue/package.json', import.meta.url));
const WebSocket = require('ws');
const { Client } = require('@stomp/stompjs');
const output = resolve(process.argv[2]);
mkdirSync(output, { recursive: true });
const base = 'http://127.0.0.1:18080';
const front = 'http://127.0.0.1:18081';
const report = { status: 'FAIL', checks: {}, checkedAt: new Date().toISOString() };
const clients = [];
let campus;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
function check(name, value) { report.checks[name] = Boolean(value); if (!value) throw new Error(name); }
async function until(test, label, timeout = 10000) {
  const end = Date.now() + timeout;
  while (Date.now() < end) { if (await test()) return; await delay(80); }
  throw new Error(label);
}
async function api(path, token, method = 'GET', body) {
  const response = await fetch(base + path, { method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(10000) });
  return { status: response.status, body: await response.json().catch(() => null) };
}
const ok = response => response.status >= 200 && response.status < 300 && (!response.body || response.body.code === 0);
async function account() {
  const username = 'release_' + randomBytes(5).toString('hex');
  const password = 'Aa9!' + randomBytes(12).toString('hex');
  const result = await api('/api/auth/register', null, 'POST', { username, password, confirmPassword: password,
    email: username + '@example.invalid', phone: '139' + String(randomInt(100000000)).padStart(8, '0') });
  check('registration', ok(result) && result.body?.data?.token);
  return { username, password, ...result.body.data, id: result.body.data.user.id };
}
async function stomp(token) {
  let ready, reject;
  const connection = new Promise((a, b) => { ready = a; reject = b; });
  const client = new Client({ webSocketFactory: () => new WebSocket('ws://127.0.0.1:18080/ws/websocket', { origin: base }),
    connectHeaders: { Authorization: `Bearer ${token}` }, reconnectDelay: 0, connectionTimeout: 7000,
    heartbeatIncoming: 0, heartbeatOutgoing: 0, debug: () => {}, onConnect: ready,
    onStompError: () => reject(new Error('stomp_authentication')), onWebSocketError: () => reject(new Error('stomp_connection')) });
  clients.push(client); client.activate();
  await Promise.race([connection, delay(8000).then(() => { throw new Error('stomp_timeout'); })]);
  return client;
}

async function browser(account, shared, marker) {
  const child = spawn(process.argv[3], ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
    '--remote-debugging-address=127.0.0.1', '--remote-debugging-port=18082',
    '--user-data-dir=' + resolve(output, '../browser-profile'), 'about:blank'], { windowsHide: true, stdio: 'ignore' });
  let socket;
  let request;
  try {
    await until(async () => fetch('http://127.0.0.1:18082/json/version').then(r => r.ok).catch(() => false), 'browser_start');
    const page = await fetch('http://127.0.0.1:18082/json/new?about:blank', { method: 'PUT' }).then(r => r.json());
    socket = new WebSocket(page.webSocketDebuggerUrl);
    await new Promise((a, b) => { socket.once('open', a); socket.once('error', b); });
    const pending = new Map(); let id = 0;
    const network = []; const exceptions = []; const sockets = [];
    let connected = false, markerSentByStomp = false;
    socket.on('message', raw => {
      const message = JSON.parse(raw);
      if (message.id && pending.has(message.id)) { const value = pending.get(message.id); pending.delete(message.id); clearTimeout(value.timer); message.error ? value.reject(new Error('browser_protocol')) : value.resolve(message.result); }
      if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.text);
      if (message.method === 'Network.responseReceived') network.push({ url: message.params.response.url, status: message.params.response.status });
      if (message.method === 'Network.webSocketCreated') sockets.push(message.params.url);
      if (message.method === 'Network.webSocketFrameReceived' && message.params.response.payloadData.includes('CONNECTED')) connected = true;
      if (message.method === 'Network.webSocketFrameSent') {
        const payload = message.params.response.payloadData;
        if (payload.includes('SEND') && payload.includes(marker) && payload.includes('/app/chat/private')) markerSentByStomp = true;
      }
    });
    request = (method, params = {}) => new Promise((resolve, reject) => {
      const number = ++id; const timer = setTimeout(() => { pending.delete(number); reject(new Error('browser_protocol_timeout')); }, 6000);
      pending.set(number, { resolve, reject, timer }); socket.send(JSON.stringify({ id: number, method, params }));
    });
    const evaluate = async expression => {
      const result = await request('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
      if (result.exceptionDetails) {
        writeFileSync(resolve(output, 'browser-evaluation.json'), JSON.stringify({
          className: result.exceptionDetails.exception?.className, line: result.exceptionDetails.lineNumber,
          column: result.exceptionDetails.columnNumber,
        }));
        throw new Error('browser_evaluation');
      }
      return result.result.value;
    };
    await request('Page.enable'); await request('Runtime.enable'); await request('Network.enable');
    await request('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
    await request('Page.navigate', { url: front + '/login' });
    await until(() => evaluate('!!document.querySelector("#username")'), 'browser_login_render');
    await evaluate(`(() => { for (const [id, value] of Object.entries(${JSON.stringify({ username: account.username, password: account.password })})) {
      const input = document.getElementById(id); input.value = value; input.dispatchEvent(new Event('input', { bubbles: true }));
    } document.querySelector('form').requestSubmit(); })()`);
    await until(() => evaluate('location.pathname !== "/login"'), 'browser_login_submit');
    check('browserLogin', network.some(r => r.url === front + '/api/auth/login' && r.status === 200));
    connected = false;
    await request('Page.navigate', { url: front + '/chat/private/' + shared });
    await until(() => evaluate('!!document.querySelector(".input-box textarea")'), 'browser_chat_render');
    await until(() => connected, 'browser_stomp_connected');
    await evaluate(`(() => { const input = document.querySelector('.input-box textarea'); input.value = ${JSON.stringify(marker)};
      input.dispatchEvent(new Event('input', { bubbles: true })); input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true })); })()`);
    await until(async () => {
      const history = await api('/api/chats/' + shared + '/messages?page=1&size=50', account.token);
      return ok(history) && history.body.data.some(m => m.content?.content === marker);
    }, 'browser_message_persisted');
    check('browserMessagePersisted', true);
    check('browserStompSend', markerSentByStomp);
    await until(() => evaluate(`(() => { const rows = [...document.querySelectorAll('.message-item')].filter(row => row.querySelector('.text-message')?.textContent.trim() === ${JSON.stringify(marker)});
      return rows.length === 1 && !!rows[0].querySelector('.message-actions') && /^✓{1,2}$/.test(rows[0].querySelector('.message-status')?.textContent.trim() || ''); })()`), 'browser_message_confirmed');
    check('browserMessageRendered', true);
    check('browserSameOriginSockJs', network.some(r => r.url.startsWith(front + '/ws/info') && r.status === 200)
      && sockets.some(url => url.startsWith('ws://127.0.0.1:18081/ws/')));
    const screenshot = await request('Page.captureScreenshot', { format: 'png' });
    writeFileSync(resolve(output, 'browser-chat.png'), Buffer.from(screenshot.data, 'base64'));
    if (campus) await campusBrowser({ fixture: campus, request, evaluate, until, check, api, ok, output });
    check('browserNoUncaughtExceptions', exceptions.length === 0);
    check('browserNoApiServerErrors', !network.some(r => r.url.startsWith(front + '/api/') && r.status >= 500));
  } catch (error) {
    try {
      if (request) {
        const screenshot = await request('Page.captureScreenshot', { format: 'png' });
        writeFileSync(resolve(output, 'browser-failure.png'), Buffer.from(screenshot.data, 'base64'));
      }
    } catch {}
    throw error;
  } finally {
    try { if (request) await request('Browser.close'); } catch {}
    socket?.close();
    if (child.exitCode === null) child.kill();
  }
}

try {
  const a = await account(), b = await account(), outsider = await account();
  check('expiryUnits', a.expiresIn > 0 && a.expiresIn < 172801 && a.expiresAt > Date.now());
  const profile = await api('/api/users/' + b.id, a.token);
  check('profileContract', ok(profile) && profile.body.data.user?.id === b.id && profile.body.data.userStats);
  const follow = await api('/api/follow/' + b.id, a.token, 'POST');
  const unfollow = await api('/api/users/' + b.id + '/follow', a.token, 'DELETE');
  const followState = await api('/api/follow/check/' + b.id, a.token);
  check('followAliases', ok(follow) && ok(unfollow) && followState.body?.data === false);
  const room = await api('/api/chats', a.token, 'POST', { targetId: String(b.id) });
  check('privateChatCreation', ok(room) && room.body.data.sharedChatId);
  const shared = room.body.data.sharedChatId;
  const denied = await api(`/api/chats/${shared}/sync?afterMessageId=0&size=20`, outsider.token);
  check('outsiderSyncDenied', denied.status === 403);
  const receiver = await stomp(b.token), sender = await stomp(a.token);
  const received = [], acknowledged = [];
  receiver.subscribe('/user/queue/private', frame => received.push(JSON.parse(frame.body)));
  sender.subscribe('/user/queue/private', frame => acknowledged.push(JSON.parse(frame.body)));
  await delay(300);
  const clientMessageId = randomUUID(), marker = 'release-' + randomUUID();
  const body = { clientMessageId, content: marker, messageType: 1 };
  const sent = await api(`/api/chats/${shared}/messages`, a.token, 'POST', body);
  const retried = await api(`/api/chats/${shared}/messages`, a.token, 'POST', body);
  check('retryReturnsSameMessage', ok(sent) && ok(retried) && sent.body.data.id === retried.body.data.id);
  const collision = await api(`/api/chats/${shared}/messages`, a.token, 'POST', { ...body, content: marker + '-changed' });
  check('idCollisionRejected', !ok(collision) && collision.status >= 400 && collision.status < 500);
  await until(() => received.some(m => m.messageId === sent.body.data.id), 'recipient_delivery');
  await until(() => acknowledged.some(m => m.clientMessageId === clientMessageId), 'sender_acknowledgement');
  check('recipientDelivery', true); check('senderAcknowledgement', true);
  const synced = await api(`/api/chats/${shared}/sync?afterMessageId=0&size=100`, b.token);
  check('sqlSyncDeduplicates', ok(synced) && synced.body.data.list.filter(m => m.id === sent.body.data.id).length === 1);
  const newer = await api(`/api/chats/${shared}/messages`, a.token, 'POST', { content: 'newer-' + randomUUID(), clientMessageId: randomUUID() });
  check('newerMessagePersisted', ok(newer));
  const receipt = await api(`/api/chats/${shared}/read`, b.token, 'POST', { lastReadMessageId: sent.body.data.id });
  const unread = await api('/api/chats/unread/stats', b.token);
  check('boundedRead', ok(receipt) && ok(unread) && unread.body.data.unreadList.some(row => String(row.chat_id) === String(shared) && row.unread_count === 1));
  campus = await campusFixture({ account, api, ok, check, until, stomp, a, b, outsider });
  await browser(a, shared, 'browser-' + randomUUID());
  await finishCampusFixture({ fixture: campus, api, ok, check });
  const logout = await api('/api/auth/logout', b.token, 'POST');
  check('logoutRevokesHttp', ok(logout) && (await api('/api/users/me', b.token)).status === 401);
  const after = await api(`/api/chats/${shared}/messages`, a.token, 'POST', { content: 'revoked-' + randomUUID(), clientMessageId: randomUUID() });
  check('messageAfterLogoutPersists', ok(after));
  await delay(1800);
  check('passiveRevokedSocketDenied', !received.some(m => m.messageId === after.body.data.id));
  report.status = 'PASS';
} catch (error) {
  // Error labels only: never expose credentials or HTTP response bodies in public artifacts.
  report.failure = /^[a-zA-Z0-9_]+$/.test(error.message) ? error.message : 'runtime_probe_failed';
  process.exitCode = 1;
} finally {
  if (campus) { try { campus.cleanup(); } catch { report.status = 'FAIL'; report.failure = 'campus_fixture_cleanup'; process.exitCode = 1; } }
  for (const client of clients) await client.deactivate({ force: true }).catch(() => {});
  writeFileSync(resolve(output, 'runtime.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report));
}
