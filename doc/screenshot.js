const { chromium } = require('playwright');
const path = require('path');

const BASE = 'http://localhost:3000';
const OUT = path.join(__dirname, 'screenshots');

async function loginAs(page, username, pw) {
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.fill('input[placeholder="用户名"]', username);
  await page.fill('input[placeholder="密码"]', pw);
  await page.click('button:has-text("登录")');
  await page.waitForURL('**/market', { timeout: 10000 });
  await page.waitForTimeout(800);
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  // 1. 登录页
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.screenshot({ path: path.join(OUT, '01-login.png'), fullPage: false });
  console.log('01-login');

  // Admin 登录 → 全部页面
  await loginAs(page, 'admin', 'admin123');

  // 2. 市场页
  await page.screenshot({ path: path.join(OUT, '02-market.png'), fullPage: false });
  console.log('02-market');

  // 3. Skill 详情页
  await page.goto(`${BASE}/skill/1`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, '03-skill-detail.png'), fullPage: true });
  console.log('03-skill-detail');

  // 4. 工作台 - 我的发布
  await page.goto(`${BASE}/workspace?tab=published`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, '04-workspace-published.png'), fullPage: false });
  console.log('04-workspace-published');

  // 5. 工作台 - 我的安装
  await page.goto(`${BASE}/workspace?tab=installed`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, '05-workspace-installed.png'), fullPage: false });
  console.log('05-workspace-installed');

  // 6. 工作台 - 待审核
  await page.goto(`${BASE}/workspace?tab=review`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, '06-admin-review.png'), fullPage: false });
  console.log('06-admin-review');

  // 7. 工作台 - 运营统计
  await page.goto(`${BASE}/workspace?tab=stats`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await page.screenshot({ path: path.join(OUT, '07-admin-stats.png'), fullPage: true });
  console.log('07-admin-stats');

  // 8. 工作台 - 用户管理
  await page.goto(`${BASE}/workspace?tab=users`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, '08-admin-users.png'), fullPage: false });
  console.log('08-admin-users');

  // 9. 工作台 - 分类管理
  await page.goto(`${BASE}/workspace?tab=categories`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, '09-admin-categories.png'), fullPage: false });
  console.log('09-admin-categories');

  // Consumer 登录 → 市场（含已安装/已下线标签）
  await loginAs(page, 'consumer1', '123456');
  await page.screenshot({ path: path.join(OUT, '10-consumer-market.png'), fullPage: false });
  console.log('10-consumer-market');

  // Consumer 工作台 - 我的安装
  await page.goto(`${BASE}/workspace?tab=installed`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(OUT, '11-consumer-installed.png'), fullPage: false });
  console.log('11-consumer-installed');

  await browser.close();
  console.log('Done! 11 screenshots');
}

main().catch(e => { console.error(e); process.exit(1); });
