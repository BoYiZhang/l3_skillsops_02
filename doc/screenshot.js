const { chromium } = require('playwright');
const path = require('path');

const BASE = 'http://localhost:3000';
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // 1. Login page
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.screenshot({ path: path.join(SCREENSHOT_DIR, '01-login.png'), fullPage: true });
  console.log('✅ login page');

  // Login as admin
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登录")');
  await page.waitForURL('**/market', { timeout: 10000 });
  await page.waitForTimeout(1000);

  // 2. Market page
  await page.screenshot({ path: path.join(SCREENSHOT_DIR, '02-market.png'), fullPage: true });
  console.log('✅ market page');

  // 3. Workspace — navigate directly
  await page.goto(`${BASE}/workspace`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOT_DIR, '03-workspace-my-skills.png'), fullPage: true });
  console.log('✅ workspace — my skills');

  // 4. Admin stats tab
  const statsTab = page.locator('#tab-stats');
  if (await statsTab.isVisible({ timeout: 3000 }).catch(() => false)) {
    await statsTab.click();
    await page.waitForTimeout(500);
  }
  await page.screenshot({ path: path.join(SCREENSHOT_DIR, '04-admin-stats.png'), fullPage: true });
  console.log('✅ admin stats');

  // 5. Skill detail — create a skill first and go to its detail
  // Navigate to a skill via market
  await page.goto(`${BASE}/market`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  // Click first skill card if exists
  const card = page.locator('.skill-card').first();
  if (await card.isVisible({ timeout: 3000 }).catch(() => false)) {
    await card.click();
    await page.waitForTimeout(1000);
    await page.screenshot({ path: path.join(SCREENSHOT_DIR, '05-skill-detail.png'), fullPage: true });
    console.log('✅ skill detail');
  }

  await browser.close();
  console.log('Done!');
}

main().catch(e => { console.error(e); process.exit(1); });
