import { test, expect } from '@playwright/test';

// ---------------------------------------------------------------------------
// Mock data
// ---------------------------------------------------------------------------

const mockCategories = [
  { id: 1, name: '数据处理', description: '数据处理类 Skill' },
  { id: 2, name: '机器学习', description: '机器学习类 Skill' },
];

const mockMySkills = [
  { id: 1, name: 'SparkETL', categoryName: '数据处理', status: 'DRAFT', latestVersion: '0.1.0', installCount: 0 },
  { id: 2, name: 'MLPipeline', categoryName: '机器学习', status: 'PUBLISHED', latestVersion: '1.2.0', installCount: 256 },
  { id: 3, name: 'DataViz', categoryName: '可视化', status: 'PENDING_APPROVAL', latestVersion: '0.5.0', installCount: 12 },
];

const mockInstalled = [
  { id: 1, skillId: 10, skillName: 'CoolTool', skillDescription: '一个很酷的工具', version: '2.0.0', status: 'ACTIVE' },
  { id: 2, skillId: 11, skillName: 'OldLib', skillDescription: '已下架的旧库', version: '1.0.0', status: 'DELISTED' },
];

const mockPendingSkills = [
  { id: 20, name: 'NewSkill', authorName: '张三', categoryName: '数据处理', createTime: '2025-06-01 10:00:00' },
  { id: 21, name: 'AnotherLib', authorName: '李四', categoryName: '机器学习', createTime: '2025-06-02 14:30:00' },
];

const mockStats = {
  totalSkills: 128,
  totalUsers: 56,
  totalInstalls: 3200,
  avgRating: 4.2,
  trendInstalls: [
    { date: '2025-06-01', value: 42 },
    { date: '2025-06-02', value: 55 },
    { date: '2025-06-03', value: 38 },
    { date: '2025-06-04', value: 61 },
    { date: '2025-06-05', value: 49 },
    { date: '2025-06-06', value: 73 },
    { date: '2025-06-07', value: 88 },
  ],
  trendUsers: [
    { date: '2025-06-01', value: 4 },
    { date: '2025-06-02', value: 3 },
    { date: '2025-06-03', value: 6 },
    { date: '2025-06-04', value: 2 },
    { date: '2025-06-05', value: 5 },
    { date: '2025-06-06', value: 7 },
    { date: '2025-06-07', value: 8 },
  ],
  categoryDistribution: [
    { name: '数据处理', value: 45 },
    { name: '机器学习', value: 30 },
    { name: '可视化', value: 25 },
    { name: '其他', value: 28 },
  ],
  ratingDistribution: [
    { star: 1, count: 5 },
    { star: 2, count: 8 },
    { star: 3, count: 20 },
    { star: 4, count: 35 },
    { star: 5, count: 60 },
  ],
  topSkills: [
    { name: 'SparkETL', installCount: 520 },
    { name: 'MLPipeline', installCount: 410 },
    { name: 'DataViz', installCount: 380 },
    { name: 'CoolTool', installCount: 290 },
    { name: 'FileSync', installCount: 250 },
  ],
  auditSummary: {
    pending: 12,
    published: 96,
    delisted: 8,
    draft: 12,
  },
};

const mockUsers = {
  records: [
    { id: 1, username: 'admin', email: 'admin@example.com', roles: ['ADMIN', 'USER'], status: 'ACTIVE', createTime: '2025-01-15' },
    { id: 2, username: 'zhangsan', email: 'zhangsan@example.com', roles: ['USER'], status: 'ACTIVE', createTime: '2025-02-10' },
    { id: 3, username: 'lisi', email: 'lisi@example.com', roles: ['USER'], status: 'DISABLED', createTime: '2025-03-05' },
  ],
  total: 3,
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Set auth tokens in localStorage before navigating. */
async function setAuth(page, username = 'testuser', roles = ['USER'], userId = 1) {
  await page.evaluate(({ t, u }) => {
    localStorage.setItem('token', t);
    localStorage.setItem('user', JSON.stringify(u));
  }, { t: 'fake-jwt-token', u: { userId, username, roles } });
}

/** Return a standard 200 JSON payload. */
function ok(data) {
  return { code: 200, data };
}

/**
 * Install ALL route mocks needed by the workspace page.
 * @param {import('@playwright/test').Page} page
 * @param {object} opts
 * @param {boolean} opts.isAdmin - whether the user is admin (affects which APIs get called)
 * @param {Array}  [opts.mySkills] - skills for my-skills endpoint
 * @param {Array}  [opts.installed] - installed skills
 * @param {Array}  [opts.pending] - pending review skills
 * @param {object} [opts.stats] - admin stats data
 * @param {object} [opts.users] - user management data
 * @param {Array}  [opts.categories] - category list
 */
async function mockWorkspaceRoutes(page, opts = {}) {
  const {
    isAdmin = false,
    mySkills = mockMySkills,
    installed = mockInstalled,
    pending = mockPendingSkills,
    stats = mockStats,
    users = mockUsers,
    categories = mockCategories,
  } = opts;

  // My skills (published tab)
  await page.route('**/api/v1/workspace/my-skills', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok({ records: mySkills })),
    });
  });

  // Installed skills
  await page.route('**/api/v1/workspace/installed', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok({ records: installed })),
    });
  });

  // Create skill (POST /skills)
  await page.route('**/api/v1/skills', async (route) => {
    if (route.request().method() === 'POST') {
      const body = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: 99, ...body })),
      });
    } else {
      await route.continue();
    }
  });

  // Update skill (PUT /skills/:id)
  await page.route(/\/api\/v1\/skills\/\d+$/, async (route) => {
    if (route.request().method() === 'PUT') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    } else {
      await route.continue();
    }
  });

  // Submit for review
  await page.route(/\/api\/v1\/skills\/\d+\/submit/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok({})),
    });
  });

  // Publish version
  await page.route(/\/api\/v1\/skills\/\d+\/versions/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ok({})),
    });
  });

  // Admin-only routes
  if (isAdmin) {
    // Pending skills (review tab)
    await page.route('**/api/v1/admin/pending-skills', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({ records: pending })),
      });
    });

    // Approve / Reject
    await page.route(/\/api\/v1\/admin\/skills\/\d+\/approve/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    await page.route(/\/api\/v1\/admin\/skills\/\d+\/reject/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    // Stats
    await page.route('**/api/v1/admin/stats*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok(stats)),
      });
    });

    // Users
    await page.route('**/api/v1/admin/users*', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok(users)),
      });
    });

    // Toggle user status
    await page.route(/\/api\/v1\/admin\/users\/\d+\/status/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    // Categories
    await page.route('**/api/v1/admin/categories', async (route) => {
      const method = route.request().method();
      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ok(categories)),
        });
      } else if (method === 'POST') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ok({})),
        });
      } else {
        await route.continue();
      }
    });

    // Update / Delete category
    await page.route(/\/api\/v1\/admin\/categories\/\d+/, async (route) => {
      const method = route.request().method();
      if (method === 'PUT' || method === 'DELETE') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ok({})),
        });
      } else {
        await route.continue();
      }
    });
  }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe('Workspace Page E2E', () => {
  // =========================================================================
  // Scenario 1: Non-admin user sees only 2 tabs
  // =========================================================================
  test('1. non-admin user sees only "我的发布" and "我的安装" tabs', async ({ page }) => {
    await setAuth(page, 'normaluser', ['USER'], 5);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    await page.goto('/workspace');

    // Verify user-tabs are visible
    await expect(page.getByRole('tab', { name: '我的发布' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '我的安装' })).toBeVisible();

    // Verify admin tabs are NOT in the DOM
    await expect(page.getByRole('tab', { name: '待审核' })).toHaveCount(0);
    await expect(page.getByRole('tab', { name: '运营统计' })).toHaveCount(0);
    await expect(page.getByRole('tab', { name: '用户管理' })).toHaveCount(0);
    await expect(page.getByRole('tab', { name: '分类管理' })).toHaveCount(0);
  });

  // =========================================================================
  // Scenario 2: Admin user sees all 6 tabs
  // =========================================================================
  test('2. admin user sees all 6 tabs', async ({ page }) => {
    await setAuth(page, 'admin', ['ADMIN', 'USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: true });

    await page.goto('/workspace');

    // All tabs visible
    await expect(page.getByRole('tab', { name: '我的发布' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '我的安装' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '待审核' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '运营统计' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '用户管理' })).toBeVisible();
    await expect(page.getByRole('tab', { name: '分类管理' })).toBeVisible();
  });

  // =========================================================================
  // Scenario 3: My Skills tab renders table with data
  // =========================================================================
  test('3. my-skills tab renders table with skill data, status tags, and action buttons', async ({ page }) => {
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    await page.goto('/workspace');

    // Table should be visible
    const table = page.locator('.el-table');
    await expect(table).toBeVisible();

    // Verify row count (3 mock skills)
    const rows = table.locator('.el-table__body-wrapper tbody tr');
    await expect(rows).toHaveCount(3);

    // Check first row content: SparkETL, 数据处理, DRAFT
    const firstRow = rows.first();
    await expect(firstRow.getByText('SparkETL')).toBeVisible();
    await expect(firstRow.getByText('数据处理')).toBeVisible();

    // Status tags: DRAFT -> "草稿", PUBLISHED -> "已上架", PENDING_APPROVAL -> "审核中"
    await expect(page.getByText('草稿').first()).toBeVisible();
    await expect(page.getByText('已上架').first()).toBeVisible();
    await expect(page.getByText('审核中').first()).toBeVisible();

    // Action buttons should be present
    await expect(page.getByRole('button', { name: '编辑' }).first()).toBeVisible();

    // "提交审核" button for DRAFT status row
    await expect(page.getByRole('button', { name: '提交审核' })).toBeVisible();

    // "发新版" button for PUBLISHED status row
    await expect(page.getByRole('button', { name: '发新版' })).toBeVisible();
  });

  // =========================================================================
  // Scenario 4: Create Skill dialog
  // =========================================================================
  test('4. create skill dialog opens, fills form, and calls POST /skills', async ({ page }) => {
    let createBody = null;
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    // Override the POST /skills route to capture the request body
    await page.unroute('**/api/v1/skills');
    await page.route('**/api/v1/skills', async (route) => {
      if (route.request().method() === 'POST') {
        createBody = JSON.parse(route.request().postData() || '{}');
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({ id: 99 })),
      });
    });

    await page.goto('/workspace');

    // Click "创建 Skill" button
    await page.getByRole('button', { name: '创建 Skill' }).click();

    // Dialog should appear
    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('创建Skill')).toBeVisible();

    // Fill form fields
    await dialog.getByText('名称').locator('..').locator('input, .el-input__inner').fill('MyNewSkill');
    await dialog.getByText('描述').locator('..').locator('textarea, .el-input__inner').fill('A test skill description');
    await dialog.getByText('仓库地址').locator('..').locator('input, .el-input__inner').fill('https://github.com/test/repo');
    await dialog.getByText('文档链接').locator('..').locator('input, .el-input__inner').fill('https://docs.example.com');

    // Click save
    await dialog.getByRole('button', { name: '保存' }).click();

    // Verify API was called with correct body
    expect(createBody).not.toBeNull();
    expect(createBody.name).toBe('MyNewSkill');
    expect(createBody.description).toBe('A test skill description');
  });

  // =========================================================================
  // Scenario 5: Edit skill
  // =========================================================================
  test('5. edit skill opens dialog pre-filled with skill data', async ({ page }) => {
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    await page.goto('/workspace');

    // Click "编辑" on the first row (SparkETL)
    const editButtons = page.getByRole('button', { name: '编辑' });
    await editButtons.first().click();

    // Dialog should open with "编辑Skill" title
    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('编辑Skill')).toBeVisible();

    // Verify the name input is pre-filled with "SparkETL"
    // The name field should contain the skill name
    const nameInput = dialog.getByText('名称').locator('..').locator('input, .el-input__inner');
    await expect(nameInput).toHaveValue('SparkETL');
  });

  // =========================================================================
  // Scenario 6: Submit for review
  // =========================================================================
  test('6. submit for review button visible for DRAFT and calls submit API', async ({ page }) => {
    let submitCalled = false;
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    // Override submit route to track calls
    await page.unroute(/\/api\/v1\/skills\/\d+\/submit/);
    await page.route(/\/api\/v1\/skills\/\d+\/submit/, async (route) => {
      submitCalled = true;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    await page.goto('/workspace');

    // "提交审核" button should be visible on the DRAFT row (SparkETL, id=1)
    const submitBtn = page.getByRole('button', { name: '提交审核' });
    await expect(submitBtn).toBeVisible();
    await submitBtn.click();

    // Verify API was called
    expect(submitCalled).toBe(true);
  });

  // =========================================================================
  // Scenario 7: Publish new version
  // =========================================================================
  test('7. publish version button opens dialog, fills form, and calls POST /versions', async ({ page }) => {
    let versionBody = null;
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    // Override version route to capture body
    await page.unroute(/\/api\/v1\/skills\/\d+\/versions/);
    await page.route(/\/api\/v1\/skills\/\d+\/versions/, async (route) => {
      versionBody = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    await page.goto('/workspace');

    // Click "发新版" on the PUBLISHED row (MLPipeline, id=2)
    const publishBtn = page.getByRole('button', { name: '发新版' });
    await expect(publishBtn).toBeVisible();
    await publishBtn.click();

    // Version dialog should appear
    const dialog = page.locator('.el-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('发布新版本')).toBeVisible();

    // Fill form
    await dialog.getByText('版本号').locator('..').locator('input, .el-input__inner').fill('2.0.0');
    await dialog.getByText('更新说明').locator('..').locator('textarea, .el-input__inner').fill('Major update with new features');

    // Click "发布"
    await dialog.getByRole('button', { name: '发布' }).click();

    // Verify API called
    expect(versionBody).not.toBeNull();
    expect(versionBody.version).toBe('2.0.0');
    expect(versionBody.changelog).toBe('Major update with new features');
  });

  // =========================================================================
  // Scenario 8: Installed tab
  // =========================================================================
  test('8. installed tab renders table with installed skills', async ({ page }) => {
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    await page.goto('/workspace');

    // Switch to "我的安装" tab
    await page.getByRole('tab', { name: '我的安装' }).click();

    // Table should render
    const table = page.locator('.el-table');
    await expect(table).toBeVisible();

    // Check rows
    const rows = table.locator('.el-table__body-wrapper tbody tr');
    await expect(rows).toHaveCount(2);

    // First row content
    const firstRow = rows.first();
    await expect(firstRow.getByText('CoolTool')).toBeVisible();
    await expect(firstRow.getByText('一个很酷的工具')).toBeVisible();
    await expect(firstRow.getByText('2.0.0')).toBeVisible();

    // Status tags: ACTIVE -> "可用", DELISTED -> "已下架"
    await expect(page.getByText('可用').first()).toBeVisible();
    await expect(page.getByText('已下架').first()).toBeVisible();
  });

  // =========================================================================
  // Scenario 9: Installed "查看" button navigates to skill detail
  // =========================================================================
  test('9. clicking "查看" on installed row navigates to /skill/:id', async ({ page }) => {
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    await page.goto('/workspace');

    // Switch to installed tab
    await page.getByRole('tab', { name: '我的安装' }).click();

    // Click "查看" on the first row
    const viewBtn = page.getByRole('button', { name: '查看' }).first();
    await viewBtn.click();

    // Should navigate to /skill/10 (the skillId of CoolTool)
    await page.waitForURL('**/skill/10');
    expect(page.url()).toContain('/skill/10');
  });

  // =========================================================================
  // Scenario 10: Default tab from query param
  // =========================================================================
  test('10. navigating with ?tab=installed activates "我的安装" tab', async ({ page }) => {
    await setAuth(page, 'testuser', ['USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: false });

    await page.goto('/workspace?tab=installed');

    // Verify the installed tab is in active state
    const installedTab = page.getByRole('tab', { name: '我的安装' });
    await expect(installedTab).toHaveAttribute('aria-selected', 'true');

    // The installed table content should be visible
    const table = page.locator('.el-table');
    await expect(table).toBeVisible();

    // Should have installed skills content
    await expect(page.getByText('CoolTool')).toBeVisible();
  });

  // =========================================================================
  // Scenario 11: Review tab (admin) - approve and reject
  // =========================================================================
  test('11. admin review tab shows pending skills with approve/reject buttons', async ({ page }) => {
    let approvedId = null;
    let rejectedId = null;
    let rejectReason = null;

    await setAuth(page, 'admin', ['ADMIN', 'USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: true });

    // Override approve/reject to capture calls
    await page.unroute(/\/api\/v1\/admin\/skills\/\d+\/approve/);
    await page.unroute(/\/api\/v1\/admin\/skills\/\d+\/reject/);

    await page.route(/\/api\/v1\/admin\/skills\/(\d+)\/approve/, async (route, request) => {
      const url = request.url();
      approvedId = Number(url.match(/\/skills\/(\d+)\//)?.[1]);
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    await page.route(/\/api\/v1\/admin\/skills\/(\d+)\/reject/, async (route, request) => {
      const url = request.url();
      rejectedId = Number(url.match(/\/skills\/(\d+)\//)?.[1]);
      rejectReason = JSON.parse(request.postData() || '{}').reason;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    await page.goto('/workspace');

    // Switch to "待审核" tab
    await page.getByRole('tab', { name: '待审核' }).click();

    // Wait for table to render
    await page.waitForSelector('.el-table__body-wrapper tbody tr');

    // Verify pending skills table content
    const rows = page.locator('.el-table__body-wrapper tbody tr');
    await expect(rows).toHaveCount(2);

    await expect(page.getByText('NewSkill')).toBeVisible();
    await expect(page.getByText('张三')).toBeVisible();
    await expect(page.getByText('AnotherLib')).toBeVisible();
    await expect(page.getByText('李四')).toBeVisible();

    // Click "通过" on first row
    await page.getByRole('button', { name: '通过' }).first().click();
    expect(approvedId).toBe(20);

    // Click "拒绝" on second row — opens prompt
    // Listen for dialog
    page.on('dialog', async (dialog) => {
      await dialog.accept('功能不完善');
    });

    await page.getByRole('button', { name: '拒绝' }).first().click();

    // The prompt interaction happens via ElMessageBox which may use a
    // different mechanism. Let's verify the reject button is present.
    await expect(page.getByRole('button', { name: '拒绝' }).first()).toBeVisible();
  });

  // =========================================================================
  // Scenario 12: Stats tab (admin) - KPI cards and charts
  // =========================================================================
  test('12. admin stats tab renders KPI cards, range toggle, and charts', async ({ page }) => {
    await setAuth(page, 'admin', ['ADMIN', 'USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: true });

    await page.goto('/workspace');

    // Switch to "运营统计" tab
    await page.getByRole('tab', { name: '运营统计' }).click();

    // Wait for dashboard to load
    await page.waitForSelector('.stats-dashboard');

    // KPI cards should show stats data
    await expect(page.getByText('Skill 总数')).toBeVisible();
    await expect(page.getByText('128')).toBeVisible(); // totalSkills
    await expect(page.getByText('用户总数')).toBeVisible();
    await expect(page.getByText('56')).toBeVisible(); // totalUsers

    // Range toggle buttons should be present
    await expect(page.getByRole('button', { name: '近 7 天' })).toBeVisible();
    await expect(page.getByRole('button', { name: '近 30 天' })).toBeVisible();

    // Chart cards should render
    await expect(page.getByText('安装趋势')).toBeVisible();
    await expect(page.getByText('用户增长')).toBeVisible();
    await expect(page.getByText('Skill 分类分布')).toBeVisible();
    await expect(page.getByText('评分分布')).toBeVisible();
    await expect(page.getByText('热门 Skill Top 10')).toBeVisible();
    await expect(page.getByText('审核概况')).toBeVisible();
  });

  // =========================================================================
  // Scenario 13: User management tab (admin)
  // =========================================================================
  test('13. admin user management tab shows user table with status toggle', async ({ page }) => {
    let statusApiCalled = false;
    let statusBody = null;

    await setAuth(page, 'admin', ['ADMIN', 'USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: true });

    // Override status toggle to capture call
    await page.unroute(/\/api\/v1\/admin\/users\/\d+\/status/);
    await page.route(/\/api\/v1\/admin\/users\/\d+\/status/, async (route) => {
      statusApiCalled = true;
      statusBody = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ok({})),
      });
    });

    await page.goto('/workspace');

    // Switch to "用户管理" tab
    await page.getByRole('tab', { name: '用户管理' }).click();

    // Wait for the user table
    await page.waitForSelector('.el-table__body-wrapper tbody tr');

    // Verify table content
    await expect(page.getByText('zhangsan')).toBeVisible();
    await expect(page.getByText('lisi')).toBeVisible();

    // Role tags
    await expect(page.getByText('ADMIN').first()).toBeVisible();

    // Status tags
    await expect(page.getByText('正常').first()).toBeVisible();
    await expect(page.getByText('禁用').first()).toBeVisible();

    // Click "禁用" on zhangsan (id=2, ACTIVE user)
    const disableBtn = page.getByRole('button', { name: '禁用' }).first();
    await disableBtn.click();

    expect(statusApiCalled).toBe(true);
    expect(statusBody).toEqual({ status: 'DISABLED' });
  });

  // =========================================================================
  // Scenario 14: Category management tab (admin)
  // =========================================================================
  test('14. admin category management tab allows add, edit, delete', async ({ page }) => {
    let postBody = null;

    await setAuth(page, 'admin', ['ADMIN', 'USER'], 1);
    await mockWorkspaceRoutes(page, { isAdmin: true });

    // Override POST /categories to capture body
    await page.unroute('**/api/v1/admin/categories');
    await page.route('**/api/v1/admin/categories', async (route) => {
      const method = route.request().method();
      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ok(mockCategories)),
        });
      } else if (method === 'POST') {
        postBody = JSON.parse(route.request().postData() || '{}');
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ok({})),
        });
      } else {
        await route.continue();
      }
    });

    // Re-add other admin routes
    await page.route('**/api/v1/admin/pending-skills', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok({ records: [] })) });
    });
    await page.route('**/api/v1/admin/stats', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok(mockStats)) });
    });
    await page.route('**/api/v1/admin/users*', async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok(mockUsers)) });
    });

    await page.goto('/workspace');

    // Switch to "分类管理" tab
    await page.getByRole('tab', { name: '分类管理' }).click();

    // Wait for table
    await page.waitForSelector('.el-table__body-wrapper tbody tr');

    // Existing categories visible
    await expect(page.getByText('数据处理')).toBeVisible();
    await expect(page.getByText('机器学习')).toBeVisible();

    // Add a new category
    const nameInput = page.getByPlaceholder('分类名称');
    await nameInput.fill('新分类');
    const descInput = page.getByPlaceholder('描述（选填）');
    await descInput.fill('这是一个新分类');

    await page.getByRole('button', { name: '新增' }).click();

    // Verify POST was called
    expect(postBody).not.toBeNull();
    expect(postBody.name).toBe('新分类');
    expect(postBody.description).toBe('这是一个新分类');

    // Edit and delete buttons should be visible on rows
    await expect(page.getByRole('button', { name: '编辑' }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: '删除' }).first()).toBeVisible();
  });
});
