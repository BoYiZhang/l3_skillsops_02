import { test, expect } from '@playwright/test';

/**
 * Helper: set admin auth into localStorage and navigate.
 */
async function loginAsAdmin(page) {
  await page.goto('/');
  await page.evaluate(() => {
    localStorage.setItem('token', 'mock-admin-token');
    localStorage.setItem(
      'user',
      JSON.stringify({ userId: 1, username: 'admin', roles: ['ADMIN'] })
    );
  });
}

// ── Shared mock data ──────────────────────────────────────────────

const pendingSkills = [
  { id: 1, name: 'Spark ETL', authorName: '张三', categoryName: '大数据', createTime: '2026-06-20 10:00:00' },
  { id: 2, name: 'Flink CEP', authorName: '李四', categoryName: '流处理', createTime: '2026-06-21 14:30:00' },
];

const statsData = {
  totalSkills: 50,
  totalUsers: 120,
  totalInstalls: 300,
  avgRating: 4.2,
  installTrend: [],
  userTrend: [],
  categoryDistribution: [],
  ratingDistribution: [],
  topSkills: [],
  auditSummary: { pending: 5, approved: 40, rejected: 10 },
};

const usersPage1 = {
  records: [
    { id: 1, username: 'admin', email: 'admin@skillsops.com', roles: ['ADMIN'], status: 'ACTIVE', createTime: '2026-01-01' },
    { id: 2, username: 'testuser', email: 'test@skillsops.com', roles: ['USER'], status: 'ACTIVE', createTime: '2026-03-15' },
    { id: 3, username: 'disabled1', email: 'd1@skillsops.com', roles: ['USER'], status: 'DISABLED', createTime: '2026-04-01' },
  ],
  total: 25,
};

const usersPage2 = {
  records: [
    { id: 4, username: 'user4', email: 'u4@skillsops.com', roles: ['USER'], status: 'ACTIVE', createTime: '2026-05-01' },
    { id: 5, username: 'user5', email: 'u5@skillsops.com', roles: ['USER'], status: 'ACTIVE', createTime: '2026-05-02' },
  ],
  total: 25,
};

const categoriesData = [
  { id: 1, name: '大数据', description: '大数据处理相关' },
  { id: 2, name: '流处理', description: '实时流处理引擎' },
];

// ── Tests ─────────────────────────────────────────────────────────

test.describe('Admin - Review Tab (待审核)', () => {

  test('1. Pending skills list renders', async ({ page }) => {
    await page.route('**/api/v1/admin/pending-skills', (route) =>
      route.fulfill({ json: { code: 200, data: { records: pendingSkills } } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=review');

    const tab = page.getByRole('tab', { name: '待审核' });
    await expect(tab).toBeVisible();
    await expect(tab).toHaveClass(/is-active/);

    const table = page.locator('table');
    await expect(table).toBeVisible();

    // Verify row contents
    await expect(page.getByText('Spark ETL')).toBeVisible();
    await expect(page.getByText('张三')).toBeVisible();
    await expect(page.getByText('大数据')).toBeVisible();
    await expect(page.getByText('2026-06-20')).toBeVisible();
    await expect(page.getByText('Flink CEP')).toBeVisible();
    await expect(page.getByText('李四')).toBeVisible();
  });

  test('2. Approve skill - calls approve API and shows success', async ({ page }) => {
    let approveCalled = false;
    let pendingCallCount = 0;

    await page.route('**/api/v1/admin/pending-skills', (route) => {
      pendingCallCount++;
      if (pendingCallCount === 1) {
        return route.fulfill({ json: { code: 200, data: { records: pendingSkills } } });
      }
      // After approve triggers reload
      return route.fulfill({ json: { code: 200, data: { records: [pendingSkills[1]] } } });
    });
    await page.route('**/api/v1/admin/skills/1/approve', (route) => {
      approveCalled = true;
      return route.fulfill({ json: { code: 200 } });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=review');

    // Click 通过 on first row
    const approveBtn = page.getByRole('button', { name: '通过' }).first();
    await approveBtn.click();

    await expect.poll(() => approveCalled).toBe(true);

    // Element Plus success message
    await expect(page.getByText('已通过')).toBeVisible();
  });

  test('3. Reject skill - prompt and calls reject API with reason', async ({ page }) => {
    let rejectBody = null;

    await page.route('**/api/v1/admin/pending-skills', (route) =>
      route.fulfill({ json: { code: 200, data: { records: pendingSkills } } })
    );
    await page.route('**/api/v1/admin/skills/1/reject', (route) => {
      rejectBody = JSON.parse(route.request().postData());
      return route.fulfill({ json: { code: 200 } });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=review');

    const rejectBtn = page.getByRole('button', { name: '拒绝' }).first();
    await rejectBtn.click();

    // ElMessageBox.prompt renders a DOM dialog (.el-message-box)
    const msgBox = page.locator('.el-message-box');
    await expect(msgBox).toBeVisible();
    await expect(msgBox.locator('.el-message-box__title')).toHaveText('拒绝上架');

    // Fill the reason
    await msgBox.locator('.el-message-box__input input').fill('not good');

    // Click confirm
    await msgBox.getByRole('button', { name: '确定' }).click();

    await expect.poll(() => rejectBody).not.toBeNull();
    expect(rejectBody).toEqual({ reason: 'not good' });
    await expect(page.getByText('已拒绝')).toBeVisible();
  });

  test('4. Reject cancel - no API called', async ({ page }) => {
    let rejectCalled = false;

    await page.route('**/api/v1/admin/pending-skills', (route) =>
      route.fulfill({ json: { code: 200, data: { records: pendingSkills } } })
    );
    await page.route('**/api/v1/admin/skills/**', (route) => {
      rejectCalled = true;
      return route.fulfill({ json: { code: 200 } });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=review');

    const rejectBtn = page.getByRole('button', { name: '拒绝' }).first();
    await rejectBtn.click();

    // ElMessageBox.prompt appears — click cancel
    const msgBox = page.locator('.el-message-box');
    await expect(msgBox).toBeVisible();
    await msgBox.getByRole('button', { name: '取消' }).click();

    // Wait a short time then verify no API was called
    await page.waitForTimeout(500);
    expect(rejectCalled).toBe(false);
  });

  test('5. Empty review list shows empty text', async ({ page }) => {
    await page.route('**/api/v1/admin/pending-skills', (route) =>
      route.fulfill({ json: { code: 200, data: { records: [] } } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=review');

    await expect(page.getByText('暂无待审核Skill')).toBeVisible();
  });
});

test.describe('Admin - Stats Tab (运营统计)', () => {

  test('6. Stats dashboard loads with KPI cards', async ({ page }) => {
    await page.route('**/api/v1/admin/stats', (route) =>
      route.fulfill({ json: { code: 200, data: statsData } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=stats');

    const tab = page.getByRole('tab', { name: '运营统计' });
    await expect(tab).toHaveClass(/is-active/);

    // KPI cards
    await expect(page.getByText('Skill 总数')).toBeVisible();
    await expect(page.getByText('50')).toBeVisible();
    await expect(page.getByText('用户总数')).toBeVisible();
    await expect(page.getByText('120')).toBeVisible();
    await expect(page.getByText('安装总数')).toBeVisible();
    await expect(page.getByText('300')).toBeVisible();
  });

  test('7. Range toggle - "近 30 天" becomes active', async ({ page }) => {
    await page.route('**/api/v1/admin/stats', (route) =>
      route.fulfill({ json: { code: 200, data: statsData } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=stats');

    const btn30d = page.getByRole('radio', { name: '近 30 天' });
    await btn30d.click();
    await expect(btn30d).toHaveClass(/is-active/);
  });

  test('8. Chart sections are visible', async ({ page }) => {
    await page.route('**/api/v1/admin/stats', (route) =>
      route.fulfill({ json: { code: 200, data: statsData } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=stats');

    // Trend charts
    await expect(page.getByText('安装趋势')).toBeVisible();
    await expect(page.getByText('用户增长')).toBeVisible();

    // Category & Rating
    await expect(page.getByText('分类分布')).toBeVisible();
    await expect(page.getByText('评分分布')).toBeVisible();

    // Top skills & Audit summary
    await expect(page.getByText('热门 Skill')).toBeVisible();
    await expect(page.getByText('审核概况')).toBeVisible();
  });
});

test.describe('Admin - User Management Tab (用户管理)', () => {

  test('9. User list renders with correct columns', async ({ page }) => {
    await page.route('**/api/v1/admin/users*', (route) =>
      route.fulfill({ json: { code: 200, data: usersPage1 } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=users');

    const tab = page.getByRole('tab', { name: '用户管理' });
    await expect(tab).toHaveClass(/is-active/);

    // Column headers
    await expect(page.getByText('ID')).toBeVisible();
    await expect(page.getByText('用户名')).toBeVisible();
    await expect(page.getByText('邮箱')).toBeVisible();
    await expect(page.getByText('角色')).toBeVisible();
    await expect(page.getByText('状态')).toBeVisible();
    await expect(page.getByText('注册时间')).toBeVisible();

    // Row data
    await expect(page.getByText('admin')).toBeVisible();
    await expect(page.getByText('testuser')).toBeVisible();
    await expect(page.getByText('admin@skillsops.com')).toBeVisible();
  });

  test('10. Toggle user status - disable active user', async ({ page }) => {
    let statusBody = null;

    await page.route('**/api/v1/admin/users*', (route) =>
      route.fulfill({ json: { code: 200, data: usersPage1 } })
    );
    await page.route('**/api/v1/admin/users/*/status', (route) => {
      statusBody = JSON.parse(route.request().postData());
      return route.fulfill({ json: { code: 200 } });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=users');

    // Click 禁用 on testuser (second user, first non-admin)
    const disableBtn = page.getByRole('button', { name: '禁用' }).first();
    await disableBtn.click();

    await expect.poll(() => statusBody).not.toBeNull();
    expect(statusBody).toEqual({ status: 'DISABLED' });
    await expect(page.getByText('已禁用')).toBeVisible();
  });

  test('11. Toggle user status - enable disabled user', async ({ page }) => {
    let statusBody = null;

    const data = {
      records: [{ id: 3, username: 'disabled1', email: 'd1@skillsops.com', roles: ['USER'], status: 'DISABLED', createTime: '2026-04-01' }],
      total: 1,
    };

    await page.route('**/api/v1/admin/users*', (route) =>
      route.fulfill({ json: { code: 200, data } })
    );
    await page.route('**/api/v1/admin/users/*/status', (route) => {
      statusBody = JSON.parse(route.request().postData());
      return route.fulfill({ json: { code: 200 } });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=users');

    const enableBtn = page.getByRole('button', { name: '启用' });
    await enableBtn.click();

    await expect.poll(() => statusBody).not.toBeNull();
    expect(statusBody).toEqual({ status: 'ACTIVE' });
    await expect(page.getByText('已启用')).toBeVisible();
  });

  test('12. Admin user has no toggle button', async ({ page }) => {
    await page.route('**/api/v1/admin/users*', (route) =>
      route.fulfill({ json: { code: 200, data: usersPage1 } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=users');

    // admin row should NOT have 禁用 or 启用 button
    // admin user (id=1, username=admin) is the first row
    // In the table, each row is a <tr>. Navigate from a unique cell.
    const adminRow = page.locator('tr', { has: page.getByText('admin@skillsops.com') });
    await expect(adminRow.getByRole('button', { name: '禁用' })).toHaveCount(0);
    await expect(adminRow.getByRole('button', { name: '启用' })).toHaveCount(0);

    // Verify testuser row DOES have the button
    await expect(page.getByRole('button', { name: '禁用' })).toHaveCount(1);
  });

  test('13. User pagination - page 2 works', async ({ page }) => {
    let requestedPage = null;

    await page.route('**/api/v1/admin/users*', (route) => {
      const url = new URL(route.request().url());
      requestedPage = url.searchParams.get('page');
      if (requestedPage === '2') {
        return route.fulfill({ json: { code: 200, data: usersPage2 } });
      }
      return route.fulfill({ json: { code: 200, data: usersPage1 } });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=users');

    // Pagination should be visible (total > page size)
    await expect(page.getByText('共 25 条')).toBeVisible();

    // Click page 2
    const page2Btn = page.locator('.el-pagination .number').filter({ hasText: '2' });
    await page2Btn.click();

    await expect.poll(() => requestedPage).toBe('2');
    await expect(page.getByText('user4')).toBeVisible();
  });
});

test.describe('Admin - Category Management Tab (分类管理)', () => {

  test('14. Category list renders', async ({ page }) => {
    await page.route('**/api/v1/admin/categories**', (route) =>
      route.fulfill({ json: { code: 200, data: categoriesData } })
    );
    await loginAsAdmin(page);
    await page.goto('/workspace?tab=categories');

    const tab = page.getByRole('tab', { name: '分类管理' });
    await expect(tab).toHaveClass(/is-active/);

    // Column headers
    await expect(page.getByText('名称')).toBeVisible();
    await expect(page.getByText('描述')).toBeVisible();

    // Row data
    await expect(page.getByText('大数据')).toBeVisible();
    await expect(page.getByText('大数据处理相关')).toBeVisible();
    await expect(page.getByText('流处理')).toBeVisible();

    // Action buttons
    await expect(page.getByRole('button', { name: '编辑' }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: '删除' }).first()).toBeVisible();
  });

  test('15. Create category - successful', async ({ page }) => {
    let postBody = null;

    let catCalledCount = 0;
    await page.route('**/api/v1/admin/categories', (route) => {
      catCalledCount++;
      if (route.request().method() === 'POST') {
        postBody = JSON.parse(route.request().postData());
        return route.fulfill({ json: { code: 200 } });
      }
      // GET
      if (catCalledCount <= 1) {
        return route.fulfill({ json: { code: 200, data: categoriesData } });
      }
      // After create, return expanded list
      return route.fulfill({
        json: { code: 200, data: [...categoriesData, { id: 3, name: '机器学习', description: 'ML相关' }] },
      });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=categories');

    // Fill in the category name
    await page.getByPlaceholder('分类名称').fill('机器学习');

    // Click 新增
    const createBtn = page.getByRole('button', { name: '新增' });
    await createBtn.click();

    await expect.poll(() => postBody).not.toBeNull();
    expect(postBody).toEqual({ name: '机器学习', description: '' });
    await expect(page.getByText('已创建')).toBeVisible();
  });

  test('16. Create category with empty name shows warning', async ({ page }) => {
    await page.route('**/api/v1/admin/categories', (route) =>
      route.fulfill({ json: { code: 200, data: categoriesData } })
    );

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=categories');

    // Click 新增 without filling name
    const createBtn = page.getByRole('button', { name: '新增' });
    await createBtn.click();

    await expect(page.getByText('请输入名称')).toBeVisible();
  });

  test('17. Edit category - dialog opens and saves', async ({ page }) => {
    let putBody = null;
    let putCalled = false;

    let routeCalls = 0;
    await page.route('**/api/v1/admin/categories', (route) => {
      routeCalls++;
      if (route.request().method() === 'PUT') {
        putBody = JSON.parse(route.request().postData());
        putCalled = true;
        return route.fulfill({ json: { code: 200 } });
      }
      if (routeCalls <= 1) {
        return route.fulfill({ json: { code: 200, data: categoriesData } });
      }
      return route.fulfill({
        json: { code: 200, data: [{ id: 1, name: '大数据v2', description: '大数据处理相关' }, categoriesData[1]] },
      });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=categories');

    // Click 编辑 on first row
    const editBtn = page.getByRole('button', { name: '编辑' }).first();
    await editBtn.click();

    // Dialog should open
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('编辑分类')).toBeVisible();

    // Change the name
    const nameInput = dialog.getByPlaceholder('分类名称').or(dialog.locator('input').first());
    // The edit dialog has el-input with the current name
    await dialog.locator('.el-form-item').first().locator('input').clear();
    await dialog.locator('.el-form-item').first().locator('input').fill('大数据v2');

    // Save
    const saveBtn = dialog.getByRole('button', { name: '保存' });
    await saveBtn.click();

    await expect.poll(() => putCalled).toBe(true);
    expect(putBody).toEqual({ name: '大数据v2', description: '大数据处理相关' });
    await expect(page.getByText('已更新')).toBeVisible();
  });

  test('18. Delete category - popconfirm and API called', async ({ page }) => {
    let deleteCalled = false;

    let routeCalls = 0;
    await page.route('**/api/v1/admin/categories', (route) => {
      routeCalls++;
      if (route.request().method() === 'DELETE') {
        deleteCalled = true;
        return route.fulfill({ json: { code: 200 } });
      }
      if (routeCalls <= 1) {
        return route.fulfill({ json: { code: 200, data: categoriesData } });
      }
      return route.fulfill({ json: { code: 200, data: [categoriesData[1]] } });
    });

    await loginAsAdmin(page);
    await page.goto('/workspace?tab=categories');

    // Click 删除 on first row
    const delBtn = page.getByRole('button', { name: '删除' }).first();
    await delBtn.click();

    // Popconfirm should appear - click confirm
    const confirmBtn = page.getByText('确定').last();
    await confirmBtn.click();

    await expect.poll(() => deleteCalled).toBe(true);
    await expect(page.getByText('已删除')).toBeVisible();
  });
});
