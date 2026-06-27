import { test, expect } from '@playwright/test'

/**
 * Auth E2E tests — all API calls are mocked via page.route().
 * The real Vite dev server (port 3000) serves the SPA, but the
 * backend is never called — every /api/v1 request is intercepted.
 */

const API_BASE = '/api/v1'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Set up localStorage auth state before navigating. */
async function setLocalAuth(page, token, user) {
  await page.evaluate(({ t, u }) => {
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(u))
  }, { t: token, u: user })
}

/** Clear localStorage auth state. */
async function clearLocalAuth(page) {
  await page.evaluate(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  })
}

/** Default mock: POST /auth/login returns a 200 with a valid token. */
function mockLoginSuccess(route, username, token) {
  const u = username || 'testuser'
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200,
      data: { token: token || 'jwt-mock-token', userId: 1, username: u, roles: ['USER'] },
      message: 'ok'
    })
  })
}

/** Mock POST /auth/register returns 200. */
function mockRegisterSuccess(route) {
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 200, data: {}, message: 'ok' })
  })
}

/** Mock GET /market/skills returns empty list (no skills). */
function mockMarketEmpty(route) {
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 200,
      data: { records: [], total: 0, page: 1, size: 12, pages: 0 },
      message: 'ok'
    })
  })
}

/** Return a 401 JSON response. */
function mockUnauthorized(route) {
  return route.fulfill({
    status: 401,
    contentType: 'application/json',
    body: JSON.stringify({ code: 401, message: 'Unauthorized' })
  })
}

// ---------------------------------------------------------------------------
// Test group: Login flow
// ---------------------------------------------------------------------------

test.describe('Login flow', () => {
  test.beforeEach(async ({ page }) => {
    await clearLocalAuth(page)
  })

  test('successful login stores token and redirects to /market', async ({ page }) => {
    // Mock all API traffic — market is loaded on redirect
    await page.route('**/api/v1/**', (route) => {
      const method = route.request().method()
      const url = route.request().url()

      if (method === 'POST' && url.includes('/auth/login')) {
        return mockLoginSuccess(route, 'alice', 'token-alice')
      }
      if (method === 'GET' && url.includes('/market/skills')) {
        return mockMarketEmpty(route)
      }
      return route.continue()
    })

    await page.goto('/login')
    await expect(page.getByPlaceholder('用户名')).toBeVisible()
    await expect(page.getByPlaceholder('密码')).toBeVisible()

    // Fill form
    await page.getByPlaceholder('用户名').fill('alice')
    await page.getByPlaceholder('密码').fill('password123')

    // Submit
    await page.getByRole('button', { name: '登录' }).click()

    // After login the app pushes to /market
    await page.waitForURL('**/market')
    await expect(page).toHaveURL(/\/market$/)

    // Token persisted
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBe('token-alice')

    // User info persisted
    const user = await page.evaluate(() => JSON.parse(localStorage.getItem('user')))
    expect(user).toMatchObject({ username: 'alice', userId: 1 })
  })
})

// ---------------------------------------------------------------------------
// Test group: Register flow
// ---------------------------------------------------------------------------

test.describe('Register flow', () => {
  test.beforeEach(async ({ page }) => {
    await clearLocalAuth(page)
  })

  test('register tab adds email field and submits register + login', async ({ page }) => {
    let registerCalled = false

    await page.route('**/api/v1/**', (route) => {
      const method = route.request().method()
      const url = route.request().url()

      if (method === 'POST' && url.includes('/auth/register')) {
        registerCalled = true
        return mockRegisterSuccess(route)
      }
      if (method === 'POST' && url.includes('/auth/login')) {
        return mockLoginSuccess(route, 'newuser', 'token-newuser')
      }
      if (method === 'GET' && url.includes('/market/skills')) {
        return mockMarketEmpty(route)
      }
      return route.continue()
    })

    await page.goto('/login')

    // Switch to register tab
    await page.getByRole('tab', { name: '注册' }).click()

    // Email field should appear
    await expect(page.getByPlaceholder('邮箱（选填）')).toBeVisible()

    // Fill form
    await page.getByPlaceholder('用户名').fill('newuser')
    await page.getByPlaceholder('密码').fill('pass123456')
    await page.getByPlaceholder('邮箱（选填）').fill('new@example.com')

    // Submit — button text changes to "注册"
    await page.getByRole('button', { name: '注册' }).click()

    // Wait for landing on /market (register calls login internally)
    await page.waitForURL('**/market')
    await expect(page).toHaveURL(/\/market$/)

    // Verify register was actually called
    expect(registerCalled).toBe(true)

    // Token stored from the subsequent login
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBe('token-newuser')
  })
})

// ---------------------------------------------------------------------------
// Test group: Login failure
// ---------------------------------------------------------------------------

test.describe('Login failure', () => {
  test.beforeEach(async ({ page }) => {
    await clearLocalAuth(page)
  })

  test('wrong credentials — 401 clears token and redirects to /login', async ({ page }) => {
    await page.route('**/api/v1/auth/login', (route) => mockUnauthorized(route))

    await page.goto('/login')

    await page.getByPlaceholder('用户名').fill('baduser')
    await page.getByPlaceholder('密码').fill('wrongpass')

    await page.getByRole('button', { name: '登录' }).click()

    // The axios 401 interceptor does window.location.href = '/login',
    // which triggers a full page reload.
    await page.waitForLoadState('domcontentloaded')

    // Should still be on /login (interceptor forcibly reloads it)
    await expect(page).toHaveURL(/\/login$/)

    // Token must be cleared by the interceptor
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBeFalsy()
  })

  test('empty fields — shows validation messages and does not call API', async ({ page }) => {
    // We track whether ANY /api/v1 call was made
    let apiCalled = false

    await page.route('**/api/v1/**', (route) => {
      apiCalled = true
      return route.continue()
    })

    await page.goto('/login')

    // Submit without filling anything
    await page.getByRole('button', { name: '登录' }).click()

    // Element Plus shows inline validation errors
    await expect(page.getByText('请输入用户名')).toBeVisible()
    await expect(page.getByText('请输入密码')).toBeVisible()

    // No API request should have been sent
    expect(apiCalled).toBe(false)
  })
})

// ---------------------------------------------------------------------------
// Test group: Token lifecycle
// ---------------------------------------------------------------------------

test.describe('Token lifecycle', () => {
  test('expired token on protected page — receives 401 and redirects to login', async ({ page }) => {
    await setLocalAuth(page, 'expired-jwt', { username: 'bob', userId: 2, roles: ['USER'] })

    // Mock all API endpoints to return 401 — simulates expired token
    await page.route('**/api/v1/**', (route) => mockUnauthorized(route))

    await page.goto('/market')

    // The /market page calls market APIs which 401,
    // the interceptor clears localStorage + hard-redirects to /login
    await page.waitForLoadState('domcontentloaded')

    await expect(page).toHaveURL(/\/login$/)

    // Token should have been cleared by the interceptor
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBeFalsy()
  })

  test('already logged in — /login redirects to /market via router guard', async ({ page }) => {
    await setLocalAuth(page, 'valid-jwt', { username: 'carol', userId: 3, roles: ['USER'] })

    // Mock market API so the redirected-to page loads cleanly
    await page.route('**/api/v1/market/skills*', (route) => mockMarketEmpty(route))

    await page.goto('/login')

    // The router beforeEach guard detects the token and redirects to /market
    await page.waitForURL('**/market')
    await expect(page).toHaveURL(/\/market$/)

    // Token is still intact
    const token = await page.evaluate(() => localStorage.getItem('token'))
    expect(token).toBe('valid-jwt')
  })
})

// ---------------------------------------------------------------------------
// Test group: UI state
// ---------------------------------------------------------------------------

test.describe('Login form UI', () => {
  test.beforeEach(async ({ page }) => {
    await clearLocalAuth(page)
  })

  test('displays login tab by default with correct button text', async ({ page }) => {
    await page.goto('/login')

    await expect(page.getByRole('tab', { name: '登录' })).toBeVisible()
    await expect(page.getByRole('tab', { name: '注册' })).toBeVisible()
    await expect(page.getByRole('button', { name: '登录' })).toBeVisible()
  })

  test('switching to register tab changes button text', async ({ page }) => {
    await page.goto('/login')

    await page.getByRole('tab', { name: '注册' }).click()

    // Button text should change to "注册"
    await expect(page.getByRole('button', { name: '注册' })).toBeVisible()
    // Email field should be visible in register mode
    await expect(page.getByPlaceholder('邮箱（选填）')).toBeVisible()
  })
})
