import { test, expect } from '@playwright/test';

// ---------------------------------------------------------------------------
// Shared test data
// ---------------------------------------------------------------------------

const SKILL_ID = 1;

const skillDetailData = {
  id: SKILL_ID,
  name: 'Test Skill',
  categoryName: 'AI',
  authorName: 'testuser',
  latestVersion: '1.0.0',
  status: 'PUBLISHED',
  avgRating: 4.5,
  installCount: 128,
  repoUrl: 'https://github.com/test/skill',
  description: 'A test skill for E2E testing',
};

const pendingSkillData = {
  ...skillDetailData,
  status: 'PENDING_APPROVAL',
};

function makeVersions(count, total) {
  const records = Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    version: `${count - i}.0.0`,
    changelog: `Changelog for version ${count - i}.0.0`,
    createTime: `2024-0${Math.ceil((count - i) / 9)}-${String((count - i) % 28 + 1).padStart(2, '0')}T10:00:00`,
  }));
  return { records, total: total ?? count };
}

function makeRatings(count, total) {
  const records = Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    username: `user_${i + 1}`,
    rating: Math.min(5, 3 + (i % 3)),
    comment: i % 2 === 0 ? `Great skill ${i + 1}` : '',
    createTime: `2024-05-${String((i % 28) + 1).padStart(2, '0')}T10:00:00`,
  }));
  return { records, total: total ?? count };
}

const okBody = { code: 200 };

// ---------------------------------------------------------------------------
// Helpers – route mocks
// ---------------------------------------------------------------------------

/**
 * Register core mocks for a standard skill detail page.
 */
async function mockCoreApis(page, opts = {}) {
  const {
    skill = skillDetailData,
    installed = false,
    versionData = makeVersions(3, 3),
    ratingData = makeRatings(2, 2),
  } = opts;

  // Skill detail
  await page.route(`**/api/v1/skills/${SKILL_ID}`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: skill }),
    });
  });

  // Install status
  await page.route(`**/api/v1/market/skills/${SKILL_ID}/install-status`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: { installed } }),
    });
  });

  // Versions
  await page.route(`**/api/v1/skills/${SKILL_ID}/versions?**`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: versionData }),
    });
  });

  // Ratings (GET)
  await page.route(`**/api/v1/skills/${SKILL_ID}/ratings?**`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, data: ratingData }),
    });
  });
}

/**
 * Set localStorage auth + user data before the page loads.
 */
async function setupAuth(page, user = null) {
  await page.addInitScript((data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
  }, {
    token: 'e2e-test-token',
    user: user ?? { userId: 2, username: 'testuser', roles: ['USER'] },
  });
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe('Skill Detail Page', () => {
  // -----------------------------------------------------------------------
  // 1. Basic display
  // -----------------------------------------------------------------------
  test.describe('Basic display', () => {
    test.beforeEach(async ({ page }) => {
      await setupAuth(page);
      await mockCoreApis(page);
      await page.goto(`/skill/${SKILL_ID}`);
    });

    test('loads skill detail with all info fields', async ({ page }) => {
      // Wait for the main content to render (el-descriptions items)
      await expect(page.getByText('Test Skill')).toBeVisible();
      await expect(page.getByText('AI')).toBeVisible();
      await expect(page.getByText('testuser')).toBeVisible();
      await expect(page.getByText('1.0.0')).toBeVisible();

      // Rating and install count are rendered inside el-descriptions-item
      await expect(page.getByText('⭐ 4.5')).toBeVisible();
      await expect(page.getByText('📥 128')).toBeVisible();

      // Description
      await expect(page.getByText('A test skill for E2E testing')).toBeVisible();

      // Repo URL
      await expect(page.getByText('https://github.com/test/skill')).toBeVisible();

      // Status tag
      await expect(page.getByText('已上架')).toBeVisible();
    });

    test('breadcrumb navigation to market', async ({ page }) => {
      // Breadcrumb shows "市场 > Skill 详情"
      const breadcrumb = page.locator('.el-breadcrumb, .breadcrumb');
      await expect(breadcrumb.getByText('市场')).toBeVisible();
      await expect(breadcrumb.getByText('Skill 详情')).toBeVisible();

      // Mock market list API before clicking breadcrumb
      await page.route('**/api/v1/market/skills?**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: { records: [], total: 0 } }),
        });
      });

      // Click "市场" breadcrumb
      await breadcrumb.getByText('市场').click();

      // Verify navigation to /market
      await expect(page).toHaveURL(/\/market/);
    });
  });

  // -----------------------------------------------------------------------
  // 2. Version list
  // -----------------------------------------------------------------------
  test.describe('Version list', () => {
    test('displays version table with correct columns', async ({ page }) => {
      const versions = makeVersions(3);
      await setupAuth(page);
      await mockCoreApis(page, { versionData: versions });

      await page.goto(`/skill/${SKILL_ID}`);

      // Wait for the version section
      await expect(page.getByText('版本历史')).toBeVisible();

      // Verify table rows
      for (const v of versions.records) {
        await expect(page.getByRole('cell', { name: v.version })).toBeVisible();
        await expect(page.getByRole('cell', { name: v.changelog })).toBeVisible();
        await expect(page.getByRole('cell', { name: v.createTime })).toBeVisible();
      }
    });

    test('handles version pagination', async ({ page }) => {
      const versions = makeVersions(20, 25);
      await setupAuth(page);

      // Register core mocks first (default versions handler)
      await mockCoreApis(page, { versionData: versions });

      // Override the versions route with dynamic pagination (last-registered wins)
      await page.route(`**/api/v1/skills/${SKILL_ID}/versions?**`, async (route) => {
        const url = new URL(route.request().url());
        const p = parseInt(url.searchParams.get('page') || '1');
        const size = parseInt(url.searchParams.get('size') || '20');
        const allRecords = Array.from({ length: 25 }, (_, i) => ({
          id: i + 1,
          version: `v${25 - i}.0.0`,
          changelog: `Changelog ${25 - i}`,
          createTime: `2024-01-${String((i % 28) + 1).padStart(2, '0')}T10:00:00`,
        }));
        const start = (p - 1) * size;
        const records = allRecords.slice(start, start + size);
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ code: 200, data: { records, total: 25 } }),
        });
      });

      await page.goto(`/skill/${SKILL_ID}`);

      // Pagination should exist because total (25) > page size (20)
      const versionSection = page.getByText('版本历史').locator('..');
      const pagination = versionSection.locator('.el-pagination');
      await expect(pagination).toBeVisible();

      // Click page 2
      await pagination.locator('.el-pager li').filter({ hasText: '2' }).click();

      // Wait for the page 2 data to load (v5.0.0 is the first record of page 2)
      await expect(page.getByRole('cell', { name: 'v5.0.0' })).toBeVisible();
    });
  });

  // -----------------------------------------------------------------------
  // 3. Rating list
  // -----------------------------------------------------------------------
  test.describe('Rating list', () => {
    test('displays rating list with user info', async ({ page }) => {
      const ratings = makeRatings(3);
      // 2 ratings with comments, 1 without
      ratings.records[1].comment = '';
      await setupAuth(page);
      await mockCoreApis(page, { ratingData: ratings });

      await page.goto(`/skill/${SKILL_ID}`);

      // Wait for rating section
      await expect(page.getByText('评价列表')).toBeVisible();

      // Verify each rating
      for (const r of ratings.records) {
        await expect(page.getByText(r.username)).toBeVisible();
        await expect(page.getByText(r.createTime)).toBeVisible();
        // Comment should be visible (or placeholder text if empty)
        if (r.comment) {
          await expect(page.getByText(r.comment)).toBeVisible();
        } else {
          await expect(page.getByText('（无文字评价）')).toBeVisible();
        }
      }
    });
  });

  // -----------------------------------------------------------------------
  // 4. Install flow
  // -----------------------------------------------------------------------
  test.describe('Install flow', () => {
    test('shows install button and installs successfully', async ({ page }) => {
      let installCalled = false;
      await setupAuth(page);
      await mockCoreApis(page, { installed: false });

      // Mock install POST
      await page.route(`**/api/v1/market/skills/${SKILL_ID}/install`, async (route) => {
        installCalled = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(okBody),
        });
      });

      await page.goto(`/skill/${SKILL_ID}`);

      // Verify install button visible
      const installBtn = page.getByRole('button', { name: '安装' });
      await expect(installBtn).toBeVisible();

      // Click install
      await installBtn.click();

      // Verify "已安装" tag appears (local state update in SkillInfo)
      await expect(page.getByText('已安装')).toBeVisible();

      // Verify install was not shown as button anymore
      await expect(page.getByRole('button', { name: '安装' })).not.toBeVisible();

      // Verify install API was called
      expect(installCalled).toBe(true);
    });

    test('shows installed state when already installed', async ({ page }) => {
      await setupAuth(page);
      await mockCoreApis(page, { installed: true });

      await page.goto(`/skill/${SKILL_ID}`);

      // Verify "已安装" tag visible
      await expect(page.getByText('已安装')).toBeVisible();

      // Verify install button NOT visible
      await expect(page.getByRole('button', { name: '安装' })).not.toBeVisible();

      // Verify rating form is visible (because installed=true is passed to RatingList)
      await expect(page.getByText('评价列表')).toBeVisible();
      await expect(page.locator('.rate-form')).toBeVisible();
      await expect(page.getByPlaceholder('写下你的评价...')).toBeVisible();
    });
  });

  // -----------------------------------------------------------------------
  // 5. Rating submission
  // -----------------------------------------------------------------------
  test.describe('Rating submission', () => {
    test('submits rating successfully', async ({ page }) => {
      let ratingSubmitted = false;
      let submittedData = null;

      await setupAuth(page);
      await mockCoreApis(page, { installed: true });

      // Mock submit rating POST
      await page.route(`**/api/v1/skills/${SKILL_ID}/ratings`, async (route) => {
        if (route.request().method() === 'POST') {
          ratingSubmitted = true;
          submittedData = JSON.parse(route.request().postData() || '{}');
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(okBody),
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`/skill/${SKILL_ID}`);

      // Wait for the rating form
      await expect(page.locator('.rate-form')).toBeVisible();

      // Click the 4th star (index 3) in the rating form
      await page.locator('.rate-form .el-rate__item').nth(3).click();

      // Fill comment
      await page.getByPlaceholder('写下你的评价...').fill('Amazing skill!');

      // Submit
      await page.locator('.rate-form').getByRole('button', { name: '提交评价' }).click();

      // Verify API was called with correct data
      expect(ratingSubmitted).toBe(true);
      expect(submittedData.rating).toBe(4);
      expect(submittedData.comment).toBe('Amazing skill!');
    });

    test('shows warning when submitting without selecting stars', async ({ page }) => {
      let ratingSubmitted = false;

      await setupAuth(page);
      await mockCoreApis(page, { installed: true });

      // Mock submit rating POST to track calls
      await page.route(`**/api/v1/skills/${SKILL_ID}/ratings`, async (route) => {
        if (route.request().method() === 'POST') {
          ratingSubmitted = true;
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(okBody),
          });
        } else {
          await route.continue();
        }
      });

      await page.goto(`/skill/${SKILL_ID}`);

      // Wait for the rating form
      await expect(page.locator('.rate-form')).toBeVisible();

      // DO NOT select any stars – just fill comment and submit
      await page.getByPlaceholder('写下你的评价...').fill('No rating given');
      await page.locator('.rate-form').getByRole('button', { name: '提交评价' }).click();

      // Verify warning message is shown
      await expect(page.getByText('请选择评分')).toBeVisible();

      // Verify rating API was NOT called
      expect(ratingSubmitted).toBe(false);
    });
  });

  // -----------------------------------------------------------------------
  // 6. Admin actions
  // -----------------------------------------------------------------------
  test.describe('Admin actions', () => {
    const adminUser = { userId: 1, username: 'admin', roles: ['ADMIN'] };

    test('shows approve and reject buttons for PENDING_APPROVAL skill', async ({ page }) => {
      await setupAuth(page, adminUser);
      await mockCoreApis(page, { skill: pendingSkillData });

      await page.goto(`/skill/${SKILL_ID}`);

      // Verify admin buttons are visible
      await expect(page.getByRole('button', { name: '通过' })).toBeVisible();
      await expect(page.getByRole('button', { name: '拒绝' })).toBeVisible();

      // Verify delist button NOT visible (because status is not PUBLISHED)
      await expect(page.getByRole('button', { name: '下架' })).not.toBeVisible();
    });

    test('handles approve with direct API call', async ({ page }) => {
      let approveCalled = false;

      await setupAuth(page, adminUser);
      await mockCoreApis(page, { skill: pendingSkillData });

      // Mock approve API
      await page.route(`**/api/v1/admin/skills/${SKILL_ID}/approve`, async (route) => {
        approveCalled = true;
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(okBody),
        });
      });

      await page.goto(`/skill/${SKILL_ID}`);

      // Click approve
      await page.getByRole('button', { name: '通过' }).click();

      // Wait for success toast to appear (confirms API returned)
      await expect(page.getByText('已通过')).toBeVisible();

      // Verify approve API was called
      expect(approveCalled).toBe(true);
    });

    test('handles reject flow with reason input', async ({ page }) => {
      let rejectCalled = false;
      let rejectData = null;

      await setupAuth(page, adminUser);
      await mockCoreApis(page, { skill: pendingSkillData });

      // Mock reject API
      await page.route(`**/api/v1/admin/skills/${SKILL_ID}/reject`, async (route) => {
        rejectCalled = true;
        rejectData = JSON.parse(route.request().postData() || '{}');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(okBody),
        });
      });

      await page.goto(`/skill/${SKILL_ID}`);

      // Click "拒绝" button
      await page.getByRole('button', { name: '拒绝' }).click();

      // ElMessageBox.prompt dialog should appear
      const messageBox = page.locator('.el-message-box');
      await expect(messageBox).toBeVisible();
      await expect(messageBox.getByText('拒绝上架')).toBeVisible();

      // Fill in the reject reason
      await messageBox.locator('input[type="text"], input.el-input__inner').fill('Not meeting quality standards');

      // Click confirm
      await messageBox.getByRole('button', { name: '确定' }).click();

      // Verify reject API was called with correct reason
      expect(rejectCalled).toBe(true);
      expect(rejectData.reason).toBe('Not meeting quality standards');
    });

    test('handles delist flow for PUBLISHED skill', async ({ page }) => {
      let delistCalled = false;
      let delistData = null;

      await setupAuth(page, adminUser);
      await mockCoreApis(page, { skill: skillDetailData }); // status = PUBLISHED

      // Mock delist API
      await page.route(`**/api/v1/admin/skills/${SKILL_ID}/delist`, async (route) => {
        delistCalled = true;
        delistData = JSON.parse(route.request().postData() || '{}');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(okBody),
        });
      });

      await page.goto(`/skill/${SKILL_ID}`);

      // Verify "下架" button visible (PUBLISHED status)
      await expect(page.getByRole('button', { name: '下架' })).toBeVisible();

      // Verify approve/reject buttons NOT visible (no longer PENDING_APPROVAL)
      await expect(page.getByRole('button', { name: '通过' })).not.toBeVisible();
      await expect(page.getByRole('button', { name: '拒绝' })).not.toBeVisible();

      // Click "下架" button
      await page.getByRole('button', { name: '下架' }).click();

      // ElMessageBox.prompt dialog should appear
      const messageBox = page.locator('.el-message-box');
      await expect(messageBox).toBeVisible();
      await expect(messageBox.getByText('下架Skill')).toBeVisible();

      // Fill in the delist reason
      await messageBox.locator('input[type="text"], input.el-input__inner').fill('Obsolete');

      // Click confirm
      await messageBox.getByRole('button', { name: '确定' }).click();

      // Verify delist API was called with correct reason
      expect(delistCalled).toBe(true);
      expect(delistData.reason).toBe('Obsolete');
    });
  });
});
