// Campus acceptance uses only newly registered accounts and the explicit loopback audit database.
import { spawnSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';

function fixtureSql(sql) {
  if (!/^jdbc:mysql:\/\/(?:127\.0\.0\.1|localhost):23306\/weeb_audit(?:\?.*)?$/.test(process.env.WEEB_TEST_MYSQL_URL || '')) throw new Error('campus_fixture_scope');
  const found = spawnSync('docker', ['ps', '--filter', 'publish=23306', '--format', '{{.ID}}'], { encoding: 'utf8', windowsHide: true, timeout: 20000 });
  const containers = found.stdout?.trim().split(/\s+/).filter(Boolean) || [];
  if (found.status !== 0 || containers.length !== 1 || !/^[a-f0-9]{12,64}$/.test(containers[0])) throw new Error('campus_fixture_container');
  const container = containers[0];
  const ports = spawnSync('docker', ['inspect', '--format', '{{json .NetworkSettings.Ports}}', container], { encoding: 'utf8', windowsHide: true });
  if (ports.status !== 0 || !Object.values(JSON.parse(ports.stdout)).flat().some(p => p?.HostIp === '127.0.0.1' && p.HostPort === '23306')) throw new Error('campus_fixture_port');
  const result = spawnSync('docker', ['exec', '-i', container, 'sh', '-c',
    'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql --user=root --batch --raw --skip-column-names weeb_audit'],
  { input: sql, encoding: 'utf8', windowsHide: true, timeout: 20000 });
  if (result.status !== 0) throw new Error('campus_fixture_sql');
  return result.stdout.trim();
}

export async function campusFixture({ account, api, ok, check, until, stomp, a, b, outsider }) {
  const admin = await account();
  if (!Number.isSafeInteger(admin.id) || !/^release_[a-f0-9]{10}$/.test(admin.username)) throw new Error('campus_fixture_identity');
  fixtureSql(`UPDATE \`user\` SET type='ADMIN' WHERE id=${admin.id} AND username='${admin.username}' AND type='USER';`);
  const schools = [];
  const fixture = { admin, a, b, outsider, schools, post: null, browserPost: null };
  fixture.cleanup = () => {
    // Exact fixture school IDs are also checked against the generated name prefix before deletion.
    for (const school of schools) {
      if (!Number.isSafeInteger(school.id)) throw new Error('campus_fixture_cleanup_identity');
      const prefix = `SELECT id FROM campus_school WHERE id=${school.id} AND name LIKE 'campus-probe-%' AND created_by=${admin.id}`;
      if (fixtureSql(prefix) !== String(school.id)) throw new Error('campus_fixture_cleanup_owner');
      fixtureSql(`DELETE FROM notifications WHERE entity_type='campus_school' AND entity_id=${school.id};
        DELETE FROM notifications WHERE entity_type='campus_post' AND entity_id IN (SELECT id FROM campus_post WHERE school_id=${school.id});
        DELETE FROM campus_media WHERE school_id=${school.id};
        DELETE FROM campus_post_like WHERE post_id IN (SELECT id FROM campus_post WHERE school_id=${school.id});
        DELETE FROM campus_post_bookmark WHERE post_id IN (SELECT id FROM campus_post WHERE school_id=${school.id});
        UPDATE campus_comment SET reply_to_comment_id=NULL WHERE post_id IN (SELECT id FROM campus_post WHERE school_id=${school.id});
        DELETE FROM campus_comment WHERE post_id IN (SELECT id FROM campus_post WHERE school_id=${school.id});
        DELETE FROM campus_report WHERE school_id=${school.id};
        DELETE FROM campus_post WHERE school_id=${school.id};
        DELETE FROM campus_verification_application WHERE school_id=${school.id};
        DELETE FROM campus_membership WHERE school_id=${school.id};
        DELETE FROM campus_audit WHERE school_id=${school.id};
        DELETE FROM campus_school WHERE id=${school.id};`);
    }
    fixtureSql(`UPDATE \`user\` SET type='USER' WHERE id=${admin.id} AND username='${admin.username}' AND type='ADMIN';`);
  };
  try {
    for (let i = 0; i < 2; i++) {
      const created = await api('/api/campus/schools', admin.token, 'POST', { name: 'campus-probe-' + randomUUID(), description: 'Disposable campus acceptance fixture', preModeration: true });
      check('campusSchoolCreation', ok(created) && created.body.data.capabilities.canModerate);
      schools.push(created.body.data);
    }
    const school = schools[0];
    fixture.school = school;
    for (const [person, destination] of [[a, school], [b, school], [outsider, schools[1]]]) {
      const application = await api(`/api/campus/schools/${destination.id}/applications`, person.token, 'POST', {
        realName: 'Fixture student', studentNumber: 'S' + person.id, department: 'Fixture department', enrollmentYear: 2026, statement: 'Manual audit fixture only' });
      check('campusVerificationApplication', ok(application) && application.body.data.status === 'PENDING');
      const approved = await api(`/api/campus/schools/${destination.id}/applications/${application.body.data.id}/review`, admin.token, 'PUT', {
        decision: 'APPROVE', reason: 'Fixture reviewed', version: application.body.data.version });
      check('campusVerificationReview', ok(approved) && approved.body.data.status === 'APPROVED');
    }
    const unprivileged = await api('/api/campus/schools', a.token, 'POST', { name: 'denied', description: '', preModeration: false });
    check('campusSiteAdminBoundary', unprivileged.status === 403);
    const form = new FormData();
    form.append('file', new Blob([Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aK1sAAAAASUVORK5CYII=', 'base64')], { type: 'image/png' }), 'fixture.png');
    const uploadResponse = await fetch(`http://127.0.0.1:18080/api/campus/schools/${school.id}/media`, { method: 'POST', headers: { Authorization: `Bearer ${a.token}` }, body: form });
    const upload = await uploadResponse.json();
    check('campusProtectedUpload', uploadResponse.ok && upload.code === 0 && upload.data.url.startsWith('/api/campus/media/'));
    const created = await api(`/api/campus/schools/${school.id}/posts`, a.token, 'POST', {
      title: 'Campus private fixture', content: 'Private body %_ literal', category: 'STUDY', mediaIds: [upload.data.id], submit: true });
    check('campusPendingPost', ok(created) && created.body.data.status === 'PENDING');
    let post = created.body.data;
    check('campusPendingVisibility', (await api(`/api/campus/posts/${post.id}`, b.token)).status === 403);
    const published = await api(`/api/campus/posts/${post.id}/review`, admin.token, 'PUT', { decision: 'APPROVE', reason: 'Reviewed', version: post.version });
    check('campusPostReview', ok(published) && published.body.data.status === 'PUBLISHED');
    post = published.body.data;
    fixture.post = post;
    check('campusCrossSchoolDenied', (await api(`/api/campus/posts/${post.id}`, outsider.token)).status === 403
      && (await api(`/api/campus/schools/${school.id}/posts`, outsider.token)).status === 403);
    const privateImage = await fetch('http://127.0.0.1:18080' + upload.data.url, { headers: { Authorization: `Bearer ${b.token}` } });
    check('campusPrivateImageRead', privateImage.ok && privateImage.headers.get('cache-control')?.includes('no-store'));
    const deniedImage = await fetch('http://127.0.0.1:18080' + upload.data.url, { headers: { Authorization: `Bearer ${outsider.token}` } });
    check('campusPrivateImageDenied', deniedImage.status === 403);
    const listener = await stomp(a.token), notices = [];
    listener.subscribe('/user/queue/notifications', frame => notices.push(JSON.parse(frame.body)));
    await new Promise(resolve => setTimeout(resolve, 200));
    await api(`/api/campus/posts/${post.id}/like`, b.token, 'PUT');
    const likeAgain = await api(`/api/campus/posts/${post.id}/like`, b.token, 'PUT');
    check('campusIdempotentLike', ok(likeAgain) && likeAgain.body.data.likeCount === 1);
    await until(() => notices.some(n => n.type === 'CAMPUS_LIKE' && n.entityId === post.id), 'campus_notification_delivery');
    check('campusNotificationDelivery', true);
    await api(`/api/campus/posts/${post.id}/bookmark`, b.token, 'PUT');
    await api(`/api/campus/posts/${post.id}/bookmark`, b.token, 'PUT');
    const bookmarks = await api(`/api/campus/schools/${school.id}/posts?scope=bookmarks`, b.token);
    check('campusIdempotentBookmark', ok(bookmarks) && bookmarks.body.data.total === 1);
    const comment = await api(`/api/campus/posts/${post.id}/comments`, b.token, 'POST', { content: 'Fixture comment', replyToCommentId: null });
    const reply = await api(`/api/campus/posts/${post.id}/comments`, a.token, 'POST', { content: 'Fixture reply', replyToCommentId: comment.body.data.id });
    check('campusCommentReply', ok(comment) && ok(reply));
    check('campusTimestampContract', [school.createdAt, post.createdAt, post.updatedAt, comment.body.data.createdAt]
      .every(value => typeof value === 'string' && value.endsWith('Z') && Math.abs(Date.now() - Date.parse(value)) < 120000));
    await api(`/api/campus/posts/${post.id}/comments/${comment.body.data.id}`, b.token, 'DELETE');
    const comments = await api(`/api/campus/posts/${post.id}/comments`, a.token);
    check('campusCommentTombstone', ok(comments) && comments.body.data.list.some(c => c.id === comment.body.data.id && c.deleted && !c.content));
    const memberSocket = await stomp(b.token), memberNotices = [];
    memberSocket.subscribe('/user/queue/notifications', frame => memberNotices.push(JSON.parse(frame.body)));
    const parent = await api(`/api/campus/posts/${post.id}/comments`, b.token, 'POST', { content: 'Reply delivery fixture', replyToCommentId: null });
    await new Promise(resolve => setTimeout(resolve, 200));
    await api(`/api/campus/posts/${post.id}/comments`, a.token, 'POST', { content: 'Visible reply', replyToCommentId: parent.body.data.id });
    await until(() => memberNotices.some(n => n.type === 'CAMPUS_REPLY' && n.entityId === post.id), 'campus_member_notification_delivery');
    const members = await api(`/api/campus/schools/${school.id}/members`, admin.token);
    let row = members.body.data.list.find(m => m.userId === b.id);
    const suspended = await api(`/api/campus/schools/${school.id}/members/${b.id}`, admin.token, 'PUT', { status: 'SUSPENDED', role: 'MEMBER', version: row.version, reason: 'Fixture suspension' });
    check('campusMemberSuspension', ok(suspended) && (await api(`/api/campus/posts/${post.id}`, b.token)).status === 403);
    const deliveredBefore = memberNotices.length;
    const hiddenReply = await api(`/api/campus/posts/${post.id}/comments`, a.token, 'POST', { content: 'Reply after suspension', replyToCommentId: parent.body.data.id });
    await new Promise(resolve => setTimeout(resolve, 1200));
    check('campusPassiveRevokedSocketDenied', ok(hiddenReply) && memberNotices.length === deliveredBefore);
    const hidden = await api('/api/notifications?page=1&size=50', b.token);
    check('campusRevokedNotificationHidden', ok(hidden) && !hidden.body.data.notifications.some(n => n.entityType === 'campus_post' && n.entityId === post.id));
    const restored = await api(`/api/campus/schools/${school.id}/members/${b.id}`, admin.token, 'PUT', { status: 'VERIFIED', role: 'MEMBER', version: suspended.body.data.version, reason: 'Fixture restored' });
    check('campusMemberRestore', ok(restored));
    const stale = await api(`/api/campus/posts/${post.id}`, a.token, 'PUT', { title: post.title, content: post.content, category: post.category, mediaIds: [upload.data.id], submit: true, version: post.version - 1 });
    check('campusStaleVersionRejected', stale.status === 409);
    const literal = await api(`/api/campus/schools/${school.id}/posts?q=%25_&size=1`, a.token);
    check('campusLiteralSearchPaging', ok(literal) && literal.body.data.total === 1 && literal.body.data.list.length === 1 && literal.body.data.page === 0);
    return fixture;
  } catch (failure) {
    fixture.cleanup();
    throw failure;
  }
}

export async function finishCampusFixture({ fixture, api, ok, check }) {
  const { a, b, admin, school, post } = fixture;
  const report = await api(`/api/campus/posts/${post.id}/reports`, b.token, 'POST', { reason: 'Fixture report for real removal' });
  const queue = await api(`/api/campus/schools/${school.id}/reports`, admin.token);
  const item = queue.body.data.list.find(r => r.postId === post.id);
  const decision = await api(`/api/campus/schools/${school.id}/reports/${item.id}`, admin.token, 'PUT', { decision: 'REMOVE', reason: 'Fixture removal', version: item.version });
  check('campusReportActuallyRemoves', ok(report) && ok(decision) && (await api(`/api/campus/posts/${post.id}`, a.token)).status === 404);
  const audit = await api(`/api/campus/schools/${school.id}/audit`, admin.token);
  check('campusModerationAudit', ok(audit) && audit.body.data.total >= 5);
}
