import { test, expect } from '@playwright/test';

// ---------------------------------------------------------------------------
// Mock data
// ---------------------------------------------------------------------------

const mockCategories = [
  { id: 1, name: '数据处理' },
  { id: 2, name: '机器学习' },
  { id: 3, name: '可视化' },
];

function buildSkills(count) {
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    name: `Skill-${i + 1}`,
    categoryName: mockCategories[i % 3].name,
    description: `这是Skill-${i + 1}的描述，用于测试市场展示页面。`.repeat(
      1 + (i % 2),
    ),
    authorName: `作者${i + 1}`,
    avgRating: Math.round((3.0 + (i % 3) * 0.9) * 10) / 10,
    installCount: (i + 1) * 128,
    latestVersion: `1.${i}.0`,
  }));
}

const twoSkills = buildSkills(2);
const fifteenSkills = buildSkills(15);

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Set fake auth tokens into localStorage so the route guard lets us through. */
async function setAuth(page) {
  await page.evaluate(() => {
    localStorage.setItem('token', 'fake-jwt-token');
    localStorage.setItem(
      'user',
      JSON.stringify({ userId: 1, username: 'testuser', roles: ['USER'] }),
    );
  });
}

/** Route handler factories that return `route.fulfill` payloads. */
function skillsApiPayload({ records, total }) {
  return { code: 200, data: { records, total } };
}

function categoriesApiPayload() {
  return { code: 200, data: mockCategories };
}

function installStatusPayload(installed) {
  return { code: 200, data: { installed } };
}

/**
 * Install route mocks for the three API endpoints the market page touches.
 *
 * @param {import('@playwright/test').Page} page
 * @param {object} opts
 * @param {Array}   opts.skills      – skills records to return from market API
 * @param {number}  opts.total       – total count
 * @param {boolean|Array} [opts.installed] – true / false / or per-skill array
 */
async function mockMarketApis(page, { skills, total, installed = false }) {
  // Market skills listing
  await page.route('**/api/v1/market/skills**', async (route) => {
    const url = new URL(route.request().url());
    const sortBy = url.searchParams.get('sortBy') || 'NEWEST';
    // Minimal server-side filtering mirror for back-to-back assertions
    let filtered = [...skills];
    const keyword = url.searchParams.get('keyword');
    const categoryId = url.searchParams.get('categoryId');
    if (categoryId) {
      const cat = mockCategories.find((c) => c.id === Number(categoryId));
      if (cat) filtered = filtered.filter((s) => s.categoryName === cat.name);
    }
    if (keyword) {
      filtered = filtered.filter(
        (s) =>
          s.name.includes(keyword) || s.description.includes(keyword),
      );
    }
    // Simulate sort
    if (sortBy === 'RATING') {
      filtered.sort((a, b) => Number(b.avgRating) - Number(a.avgRating));
    } else if (sortBy === 'HOT') {
      filtered.sort((a, b) => b.installCount - a.installCount);
    }
    // Paginate
    const pageNum = Number(url.searchParams.get('page') || '1');
    const size = Number(url.searchParams.get('size') || '12');
    const start = (pageNum - 1) * size;
    const paged = filtered.slice(start, start + size);

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(skillsApiPayload({ records: paged, total: filtered.length })),
    });
  });

  // Categories for filter bar
  await page.route('**/api/v1/admin/categories', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(categoriesApiPayload()),
    });
  });

  // Install status per skill
  await page.route(
    /\/api\/v1\/market\/skills\/\d+\/install-status/,
    async (route) => {
      const url = route.request().url();
      const skillId = Number(url.match(/\/skills\/(\d+)\//)?.[1]);
      const isInstalled = Array.isArray(installed)
        ? !!installed.find((i) => i === skillId)
        : installed;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(installStatusPayload(isInstalled)),
      });
    },
  );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe('Market Page E2E', () => {
  // -----------------------------------------------------------------------
  // 1. Unauthenticated redirect
  // -----------------------------------------------------------------------
  test('1. redirects to /login when no auth token', async ({ page }) => {
    await page.evaluate(() => localStorage.clear());
    await page.goto('/market');
    await page.waitForURL('**/login');
    expect(page.url()).toContain('/login');
    await expect(page.getByText('SkillsOps')).toBeVisible();
    await expect(page.getByText('登录')).toBeVisible();
  });

  // -----------------------------------------------------------------------
  // 2. Market page loads with skill cards
  // -----------------------------------------------------------------------
  test('2. market page renders skill cards from API', async ({ page }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: twoSkills, total: 2 });

    await page.goto('/market');

    // Header logo
    await expect(page.getByText('SkillsOps').first()).toBeVisible();

    // Grid cards – SkillCard root has class .skill-card (el-card)
    await page.waitForSelector('.skill-card');
    const cards = page.locator('.skill-card');
    await expect(cards).toHaveCount(2);
  });

  // -----------------------------------------------------------------------
  // 3. Skill card content
  // -----------------------------------------------------------------------
  test('3. skill card displays name, category, description, author, rating, install count', async ({
    page,
  }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: twoSkills, total: 2 });
    await page.goto('/market');
    await page.waitForSelector('.skill-card');

    const first = page.locator('.skill-card').first();

    // name
    await expect(first.locator('h4')).toHaveText('Skill-1');
    // category tag
    await expect(first.locator('.el-tag--info')).toHaveText('数据处理');
    // description
    await expect(first.locator('.card-desc')).toContainText('Skill-1');
    // author
    await expect(first.getByText('👤 作者1')).toBeVisible();
    // rating
    await expect(first.getByText('⭐ 3.0')).toBeVisible();
    // install count
    await expect(first.getByText('📥 128')).toBeVisible();
  });

  // -----------------------------------------------------------------------
  // 4. Search by keyword
  // -----------------------------------------------------------------------
  test('4. keyword search calls API with keyword param', async ({ page }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: fifteenSkills, total: 15 });
    await page.goto('/market');
    await page.waitForSelector('.skill-card');

    const searchInput = page.getByPlaceholder('搜索Skill...');
    await searchInput.fill('Skill-2');
    await searchInput.press('Enter');

    // After search, only exact-match records render
    // "Skill-2" matches "Skill-2", "Skill-12", "Skill-20" etc. in name
    const cards = page.locator('.skill-card');
    // Skill-2 (id=2) and Skill-12 (id=12) contain "Skill-2" in name
    // Skill-20, Skill-21, Skill-22 etc. are out of range for 15 items
    await expect(cards).toHaveCount(2);
  });

  // -----------------------------------------------------------------------
  // 5. Filter by category
  // -----------------------------------------------------------------------
  test('5. category filter calls API with categoryId', async ({ page }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: fifteenSkills, total: 15 });
    await page.goto('/market');
    await page.waitForSelector('.skill-card');

    // Open the category select and pick "机器学习"
    const categorySelect = page.locator('.filter-bar .el-select').first();
    await categorySelect.click();
    const option = page.getByRole('option', { name: '机器学习' });
    await option.click();

    // Should show only skills with categoryName "机器学习"
    // In fifteenSkills, indices 1, 4, 7, 10, 13 have category "机器学习"
    const cards = page.locator('.skill-card');
    await expect(cards).toHaveCount(5);
  });

  // -----------------------------------------------------------------------
  // 6. Sort change
  // -----------------------------------------------------------------------
  test('6. sort change calls API with sortBy param', async ({ page }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: fifteenSkills, total: 15 });
    await page.goto('/market');
    await page.waitForSelector('.skill-card');

    // The last el-select in the filter bar is the sort dropdown
    const sortSelect = page.locator('.filter-bar .el-select').nth(2);
    await sortSelect.click();
    const option = page.getByRole('option', { name: '最多安装' });
    await option.click();

    // With HOT sort, highest installCount comes first
    const firstCard = page.locator('.skill-card').first();
    // installCount = id * 128, so Skill-15 has 15*128=1920
    await expect(firstCard.locator('h4')).toHaveText('Skill-15');
  });

  // -----------------------------------------------------------------------
  // 7. Pagination
  // -----------------------------------------------------------------------
  test('7. pagination renders when total > page size and navigates', async ({
    page,
  }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: fifteenSkills, total: 15 });
    await page.goto('/market');
    await page.waitForSelector('.skill-card');

    // Pagination should be visible
    const pagination = page.locator('.el-pagination');
    await expect(pagination).toBeVisible();

    // Click page 2
    const page2Btn = pagination.getByText('2', { exact: true });
    await page2Btn.click();

    // Wait for cards to rerender
    await page.waitForSelector('.skill-card');
    const cards = page.locator('.skill-card');
    // page 2 of 15 items with size 12 = 3 items
    await expect(cards).toHaveCount(3);
    // First card on page 2 should be Skill-13
    await expect(cards.first().locator('h4')).toHaveText('Skill-13');
  });

  // -----------------------------------------------------------------------
  // 8. Empty state
  // -----------------------------------------------------------------------
  test('8. empty state renders when no skills', async ({ page }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: [], total: 0 });
    await page.goto('/market');

    const emptyEl = page.locator('.el-empty');
    await expect(emptyEl).toBeVisible();
    await expect(page.getByText('暂无已上架的Skill')).toBeVisible();
  });

  // -----------------------------------------------------------------------
  // 9. Install status badge
  // -----------------------------------------------------------------------
  test('9. installed badge shows when install-status returns true', async ({
    page,
  }) => {
    await setAuth(page);
    // Skill id=1 is installed, id=2 is not
    await mockMarketApis(page, { skills: twoSkills, total: 2, installed: [1] });
    await page.goto('/market');
    await page.waitForSelector('.skill-card');

    const firstCard = page.locator('.skill-card').first();
    // "已安装" tag should be visible on the first card
    await expect(firstCard.locator('.el-tag--success')).toHaveText('已安装');

    const secondCard = page.locator('.skill-card').nth(1);
    // Second card should NOT have the installed tag
    await expect(secondCard.locator('.el-tag--success')).toHaveCount(0);
  });

  // -----------------------------------------------------------------------
  // 10. Card click navigates to skill detail
  // -----------------------------------------------------------------------
  test('10. clicking a skill card navigates to /skill/:id', async ({ page }) => {
    await setAuth(page);
    await mockMarketApis(page, { skills: twoSkills, total: 2 });
    await page.goto('/market');
    await page.waitForSelector('.skill-card');

    const firstCard = page.locator('.skill-card').first();
    await firstCard.click();

    await page.waitForURL('**/skill/1');
    expect(page.url()).toContain('/skill/1');
  });
});
