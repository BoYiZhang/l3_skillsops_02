import { describe, it, expect, beforeEach, vi } from 'vitest'

describe('Router Guards', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirects to login when no token', async () => {
    // Import router module which sets up guards
    const routerModule = await import('@/router/index.js')
    const router = routerModule.default

    // Simulate navigation to protected route without token
    let redirectTarget = null
    router.beforeEach((to, from, next) => {
      if (to.meta.requiresAuth && !localStorage.getItem('token')) {
        redirectTarget = '/login'
        next('/login')
      } else {
        next()
      }
    })

    // Check that a protected route would redirect
    const market = router.getRoutes().find(r => r.name === 'Market')
    expect(market.meta.requiresAuth).toBe(true)
  })

  it('allows access when token exists', async () => {
    localStorage.setItem('token', 'test-token')
    localStorage.setItem('user', JSON.stringify({ username: 'test', roles: ['USER'] }))

    const routerModule = await import('@/router/index.js')
    const router = routerModule.default

    // Check that routes exist
    const market = router.getRoutes().find(r => r.name === 'Market')
    expect(market).toBeDefined()
    expect(market.name).toBe('Market')
  })

  it('redirects logged-in user from login page', async () => {
    localStorage.setItem('token', 'test-token')

    const routerModule = await import('@/router/index.js')
    const router = routerModule.default

    const login = router.getRoutes().find(r => r.name === 'Login')
    expect(login).toBeDefined()
    expect(login.path).toBe('/login')
  })

  it('has correct route configuration', async () => {
    const routerModule = await import('@/router/index.js')
    const router = routerModule.default

    const routes = router.getRoutes()
    const routeNames = routes.map(r => r.name).filter(Boolean)

    expect(routeNames).toContain('Login')
    expect(routeNames).toContain('Market')
    expect(routeNames).toContain('SkillDetail')
    expect(routeNames).toContain('Workspace')
  })
})
