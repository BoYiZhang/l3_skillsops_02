import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock axios
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  }
}))

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('starts with no auth', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.user).toBeNull()
  })

  it('login sets token and user', async () => {
    const request = await import('@/utils/request')
    request.default.post.mockResolvedValue({
      data: { token: 'test-token', userId: 1, username: 'admin', roles: ['ADMIN'] }
    })
    const store = useAuthStore()
    await store.login('admin', 'admin123')
    expect(store.isLoggedIn).toBe(true)
    expect(store.isAdmin).toBe(true)
    expect(localStorage.getItem('token')).toBe('test-token')
  })

  it('logout clears state', () => {
    const store = useAuthStore()
    store.token = 'test'
    store.user = { username: 'test', roles: ['USER'] }
    store.logout()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })
})
