// Real DOM journeys against the built app; no direct Pinia/storage mutation or mocked HTTP.
import { randomUUID } from 'node:crypto';
import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

export async function campusBrowser({ fixture, request, evaluate, until, check, api, ok, output }) {
  const front = 'http://127.0.0.1:18081';
  await request('Emulation.setTimezoneOverride', { timezoneId: 'Asia/Shanghai' });
  const { a, outsider, admin, school } = fixture;
  const navigate = async path => { await request('Page.navigate', { url: front + path }); await until(() => evaluate('!!document.querySelector(".campus")'), 'campus_browser_navigation'); };
  const field = async (label, value) => {
    const result = await evaluate(`(() => { const labels = [...document.querySelectorAll('.campus label')].filter(node => node.getClientRects().length && node.firstChild?.textContent.trim() === ${JSON.stringify(label)});
      if (labels.length !== 1) return false; const input = labels[0].querySelector('input,textarea,select'); if (!input || input.disabled) return false;
      input.value = ${JSON.stringify(String(value))}; input.dispatchEvent(new Event('input', {bubbles:true})); input.dispatchEvent(new Event('change', {bubbles:true})); return true; })()`);
    if (!result) throw new Error('campus_browser_field');
  };
  const button = async label => {
    await until(() => evaluate(`(() => { const rows = [...document.querySelectorAll('.campus button')].filter(node => node.getClientRects().length && node.textContent.trim() === ${JSON.stringify(label)}); return rows.length === 1 && !rows[0].disabled; })()`), 'campus_browser_button_ready');
    await evaluate(`[...document.querySelectorAll('.campus button')].find(node => node.getClientRects().length && node.textContent.trim() === ${JSON.stringify(label)}).click()`);
  };
  const login = async person => {
    await evaluate('document.querySelector(".user-avatar-wrapper").click()');
    await until(() => evaluate('!!document.querySelector(".logout-item")?.getClientRects().length'), 'campus_browser_logout_menu');
    await evaluate('document.querySelector(".logout-item").click()');
    await until(() => evaluate('!!document.querySelector(".el-message-box__btns .el-button--primary")'), 'campus_browser_logout_confirmation');
    await evaluate('document.querySelector(".el-message-box__btns .el-button--primary").click()');
    await until(() => evaluate('location.pathname === "/login" && !!document.querySelector("#username")'), 'campus_browser_logout');
    await evaluate(`(() => { for (const [id,value] of Object.entries(${JSON.stringify({ username: person.username, password: person.password })})) { const input=document.getElementById(id); input.value=value; input.dispatchEvent(new Event('input',{bubbles:true})); } document.querySelector('form').requestSubmit(); })()`);
    await until(() => evaluate('location.pathname !== "/login" && !!document.querySelector(".user-avatar-wrapper")'), 'campus_browser_login');
  };
  await navigate(`/campus/posts/${fixture.post.id}`);
  await until(() => evaluate("[...document.querySelectorAll('.campus img')].some(img => img.src.startsWith('blob:') && img.naturalWidth > 0)"), 'campus_browser_private_image');
  check('campusBrowserPrivateImage', true);
  check('campusBrowserLocalTime', await evaluate(`document.querySelector('.campus').textContent.includes(new Date(${JSON.stringify(fixture.post.createdAt)}).toLocaleString('zh-CN'))`));
  await navigate(`/campus/schools/${school.id}/new`);
  await until(() => evaluate('!!document.querySelector(".campus button[value=draft]")'), 'campus_browser_editor');
  const marker = 'Browser campus ' + randomUUID();
  await field('标题', marker); await field('正文', 'Browser-created private campus draft');
  await button('保存草稿');
  await until(() => evaluate('/^\\/campus\\/posts\\/[0-9]+$/.test(location.pathname)'), 'campus_browser_draft_saved');
  const postId = await evaluate('Number(location.pathname.split("/").at(-1))');
  fixture.browserPost = postId;
  let persisted = await api(`/api/campus/posts/${postId}`, a.token);
  check('campusBrowserDraftPersisted', ok(persisted) && persisted.body.data.status === 'DRAFT' && persisted.body.data.title === marker);
  await navigate(`/campus/posts/${postId}/edit`);
  await until(() => evaluate('!!document.querySelector(".campus button[value=publish]")'), 'campus_browser_edit');
  await field('正文', 'Browser submitted revision'); await button('提交动态');
  await until(() => evaluate(`location.pathname === '/campus/posts/${postId}' && document.querySelector('.campus')?.textContent.includes('待审核')`), 'campus_browser_pending');
  persisted = await api(`/api/campus/posts/${postId}`, a.token);
  check('campusBrowserSubmitPending', ok(persisted) && persisted.body.data.status === 'PENDING');

  await login(outsider);
  await navigate(`/campus/schools/${school.id}`);
  await until(() => evaluate('[...document.querySelectorAll(".campus button")].some(b=>b.textContent.trim()==="提交认证申请")'), 'campus_browser_application');
  await field('姓名', 'Browser applicant'); await field('学号', 'B' + outsider.id); await field('院系', 'Browser department'); await field('入学年份', '2026');
  await button('提交认证申请');
  await until(() => evaluate('document.querySelector(".campus")?.textContent.includes("申请已提交")'), 'campus_browser_application_submitted');
  const applications = await api(`/api/campus/schools/${school.id}/applications/mine`, outsider.token);
  check('campusBrowserApplicationPersisted', ok(applications) && applications.body.data.list[0]?.status === 'PENDING');

  await login(admin);
  await navigate(`/campus/schools/${school.id}/admin`);
  await button('认证审核'); await button('查看并审核申请');
  await until(() => evaluate('document.querySelector("[role=dialog]")?.textContent.includes("Browser applicant")'), 'campus_browser_private_application_review');
  await field('处理原因', 'Reviewed in real browser'); await button('确认处理');
  await until(async () => (await api(`/api/campus/schools/${school.id}`, outsider.token)).body?.data?.capabilities.canRead === true, 'campus_browser_application_approved');
  check('campusBrowserApplicationReview', true);
  await button('内容审核'); await button('审核当前版本');
  await field('处理原因', 'Browser content review'); await button('确认处理');
  await until(async () => (await api(`/api/campus/posts/${postId}`, outsider.token)).body?.data?.status === 'PUBLISHED', 'campus_browser_post_approved');
  check('campusBrowserPostReview', true);
  const adminShot = await request('Page.captureScreenshot', { format: 'png' });
  writeFileSync(resolve(output, 'browser-campus-admin.png'), Buffer.from(adminShot.data, 'base64'));

  await login(outsider);
  await navigate(`/campus/posts/${postId}`);
  await until(() => evaluate('!!document.querySelector(".campus textarea")'), 'campus_browser_published_post');
  await button('点赞 · 0'); await button('收藏');
  await field('评论内容', 'Browser campus comment'); await button('发表评论');
  await until(async () => {
    const result = await api(`/api/campus/posts/${postId}/comments`, a.token);
    return ok(result) && result.body.data.list.some(row => row.content === 'Browser campus comment' && row.author.id === outsider.id);
  }, 'campus_browser_comment_persisted');
  check('campusBrowserInteractions', true);
  await until(() => evaluate('document.querySelector(".campus")?.textContent.includes("Browser campus comment")'), 'campus_browser_comment_rendered');
  const screenshot = await request('Page.captureScreenshot', { format: 'png' });
  writeFileSync(resolve(output, 'browser-campus-post.png'), Buffer.from(screenshot.data, 'base64'));
  await request('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  const mobile = await request('Page.captureScreenshot', { format: 'png' });
  writeFileSync(resolve(output, 'browser-campus-mobile.png'), Buffer.from(mobile.data, 'base64'));
  check('campusBrowserMobileLayout', await evaluate('document.documentElement.scrollWidth <= innerWidth + 1'));
  await evaluate('document.querySelector("button[aria-label=深色模式]").click()');
  // Wait for the actual rendered colors, including the shared theme transition.
  await until(() => evaluate(`(() => {
    if (document.documentElement.getAttribute('data-theme') !== 'dark') return false;
    const style = getComputedStyle(document.querySelector('.campus .campus-card'));
    const rgb = value => (value.match(/[0-9.]+/g) || []).slice(0, 3).map(Number);
    return rgb(style.color).every(value => value >= 240) && rgb(style.backgroundColor).every(value => value <= 40);
  })()`), 'campus_browser_dark_theme');
  check('campusBrowserDarkTheme', await evaluate('document.documentElement.scrollWidth <= innerWidth + 1'));
  const dark = await request('Page.captureScreenshot', { format: 'png' });
  writeFileSync(resolve(output, 'browser-campus-dark.png'), Buffer.from(dark.data, 'base64'));
}
