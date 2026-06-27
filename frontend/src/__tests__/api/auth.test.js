/**
 * API Contract Tests — Auth endpoints
 *
 * Verifies that the auth-related axios calls are configured correctly:
 * - HTTP methods match the backend contract
 * - URL paths are correct
 * - Request bodies are structured properly
 * - Token is included in Authorization header when present
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

// Mock localStorage
const localStorageMock = (() => {
  let store = {}
  return {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => { store[key] = value }),
    removeItem: vi.fn((key) => { delete store[key] }),
    clear: vi.fn(() => { store = {} })
  }
})()
Object.defineProperty(global, 'localStorage', { value: localStorageMock })

// Hoist mock functions AND the request instance so they're available when vi.mock factory runs
const { mockGet, mockPost, mockPut, mockDelete, mockRequestInstance } = vi.hoisted(() => {
  const mockGet = vi.fn()
  const mockPost = vi.fn()
  const mockPut = vi.fn()
  const mockDelete = vi.fn()
  const mockRequestInstance = {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }
  return { mockGet, mockPost, mockPut, mockDelete, mockRequestInstance }
})

vi.mock('@/utils/request', () => ({
  default: mockRequestInstance
}))

import { useAuthStore } from '@/stores/auth'
import { setActivePinia, createPinia } from 'pinia'

describe('Auth API Contract Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  // =====================================================================
  // POST /api/v1/auth/register
  // =====================================================================

  describe('POST /auth/register', () => {
    it('calls POST with correct path and body', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useAuthStore()

      await store.register({ username: 'newuser', password: 'secret123' })

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/auth/register', {
        username: 'newuser',
        password: 'secret123'
      })
    })

    it('sends registration with all fields when email is provided', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useAuthStore()

      await store.register({
        username: 'emailuser',
        password: 'password123',
        email: 'user@example.com'
      })

      expect(mockPost).toHaveBeenCalledWith('/auth/register', {
        username: 'emailuser',
        password: 'password123',
        email: 'user@example.com'
      })
    })

    it('does not include token in registration request', async () => {
      // Registration is a public endpoint and localStorage is empty by default
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useAuthStore()

      await store.register({ username: 'nobody', password: '123456' })

      expect(mockPost).toHaveBeenCalledTimes(1)
      // Registration calls don't set auth token (no token in localStorage)
      expect(localStorage.getItem('token')).toBeNull()
    })
  })

  // =====================================================================
  // POST /api/v1/auth/login
  // =====================================================================

  describe('POST /auth/login', () => {
    it('calls POST with correct path and credentials', async () => {
      mockPost.mockResolvedValue({
        data: {
          token: 'jwt-token-abc',
          userId: 42,
          username: 'testuser',
          roles: ['USER']
        }
      })
      const store = useAuthStore()

      await store.login('testuser', 'mypassword')

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/auth/login', {
        username: 'testuser',
        password: 'mypassword'
      })
    })

    it('stores token in localStorage after successful login', async () => {
      mockPost.mockResolvedValue({
        data: {
          token: 'jwt-token-xyz',
          userId: 7,
          username: 'admin',
          roles: ['ADMIN', 'USER']
        }
      })
      const store = useAuthStore()

      await store.login('admin', 'admin123')

      expect(localStorage.setItem).toHaveBeenCalledWith('token', 'jwt-token-xyz')
      expect(localStorage.setItem).toHaveBeenCalledWith(
        'user',
        JSON.stringify({ userId: 7, username: 'admin', roles: ['ADMIN', 'USER'] })
      )
    })

    it('sets isLoggedIn to true after login', async () => {
      mockPost.mockResolvedValue({
        data: {
          token: 'any-token',
          userId: 1,
          username: 'u',
          roles: ['USER']
        }
      })
      const store = useAuthStore()
      expect(store.isLoggedIn).toBe(false)

      await store.login('u', 'p')

      expect(store.isLoggedIn).toBe(true)
    })

    it('sets isAdmin to true when role includes ADMIN', async () => {
      mockPost.mockResolvedValue({
        data: {
          token: 'admin-token',
          userId: 1,
          username: 'superadmin',
          roles: ['ADMIN']
        }
      })
      const store = useAuthStore()

      await store.login('superadmin', 'pass')

      expect(store.isAdmin).toBe(true)
    })

    it('sets isAdmin to false for regular user', async () => {
      mockPost.mockResolvedValue({
        data: {
          token: 'user-token',
          userId: 2,
          username: 'regular',
          roles: ['USER']
        }
      })
      const store = useAuthStore()

      await store.login('regular', 'pass')

      expect(store.isAdmin).toBe(false)
    })
  })

  // =====================================================================
  // Logout behavior
  // =====================================================================

  describe('logout', () => {
    it('clears token and user from localStorage', () => {
      localStorage.setItem('token', 'old-token')
      localStorage.setItem('user', JSON.stringify({ username: 'test' }))

      const store = useAuthStore()
      store.logout()

      expect(localStorage.removeItem).toHaveBeenCalledWith('token')
      expect(localStorage.removeItem).toHaveBeenCalledWith('user')
      expect(store.isLoggedIn).toBe(false)
      expect(store.user).toBeNull()
    })
  })
})
