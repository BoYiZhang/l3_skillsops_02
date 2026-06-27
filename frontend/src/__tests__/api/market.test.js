/**
 * API Contract Tests — Market endpoints
 *
 * Verifies that the market-related API calls use correct:
 * - HTTP methods (GET for query, POST for install)
 * - URL paths with proper parameter interpolation
 * - Query parameter format for paginated list endpoints
 * - Request body structure for install operations
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

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

const { mockGet, mockPost, mockRequestInstance } = vi.hoisted(() => {
  const mockGet = vi.fn()
  const mockPost = vi.fn()
  const mockRequestInstance = {
    get: mockGet,
    post: mockPost,
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }
  return { mockGet, mockPost, mockRequestInstance }
})

vi.mock('@/utils/request', () => ({
  default: mockRequestInstance
}))

import { useSkillStore } from '@/stores/skill'
import { setActivePinia, createPinia } from 'pinia'

describe('Market API Contract Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  // =====================================================================
  // GET /api/v1/market/skills — Market listing (paginated)
  // =====================================================================

  describe('GET /market/skills — fetchMarket', () => {
    it('calls GET with correct path', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 12, current: 1 }
      })
      const store = useSkillStore()

      await store.fetchMarket()

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/market/skills', { params: {} })
    })

    it('passes categoryId as query parameter', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 12, current: 1 }
      })
      const store = useSkillStore()

      await store.fetchMarket({ categoryId: 3 })

      expect(mockGet).toHaveBeenCalledWith('/market/skills', {
        params: { categoryId: 3 }
      })
    })

    it('passes keyword as query parameter', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 12, current: 1 }
      })
      const store = useSkillStore()

      await store.fetchMarket({ keyword: 'python' })

      expect(mockGet).toHaveBeenCalledWith('/market/skills', {
        params: { keyword: 'python' }
      })
    })

    it('passes sortBy as query parameter', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 12, current: 1 }
      })
      const store = useSkillStore()

      await store.fetchMarket({ sortBy: 'rating' })

      expect(mockGet).toHaveBeenCalledWith('/market/skills', {
        params: { sortBy: 'rating' }
      })
    })

    it('passes page and size as query parameters', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 50, size: 12, current: 3 }
      })
      const store = useSkillStore()

      await store.fetchMarket({ page: 3, size: 12 })

      expect(mockGet).toHaveBeenCalledWith('/market/skills', {
        params: { page: 3, size: 12 }
      })
    })

    it('passes all filters together', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 5, size: 10, current: 1 }
      })
      const store = useSkillStore()

      await store.fetchMarket({
        categoryId: 2,
        keyword: 'data',
        sortBy: 'newest',
        page: 1,
        size: 10
      })

      expect(mockGet).toHaveBeenCalledWith('/market/skills', {
        params: {
          categoryId: 2,
          keyword: 'data',
          sortBy: 'newest',
          page: 1,
          size: 10
        }
      })
    })

    it('updates marketSkills and total after successful fetch', async () => {
      const mockRecords = [
        { id: 1, name: 'Skill A', status: 'PUBLISHED' },
        { id: 2, name: 'Skill B', status: 'PUBLISHED' },
      ]
      mockGet.mockResolvedValue({
        data: { records: mockRecords, total: 2, size: 12, current: 1 }
      })
      const store = useSkillStore()

      await store.fetchMarket()

      expect(store.marketSkills).toEqual(mockRecords)
      expect(store.marketSkills).toHaveLength(2)
      expect(store.total).toBe(2)
    })
  })

  // =====================================================================
  // GET /api/v1/skills/:id — Skill detail
  // =====================================================================

  describe('GET /skills/:id — fetchDetail', () => {
    it('calls GET with correct skill id in path', async () => {
      const mockSkill = {
        id: 42,
        name: 'Test Skill',
        description: 'A test',
        status: 'PUBLISHED',
        authorName: 'author1',
        categoryName: 'Tools',
      }
      mockGet.mockResolvedValue({ data: mockSkill })
      const store = useSkillStore()

      const result = await store.fetchDetail(42)

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/skills/42')
      expect(result).toEqual(mockSkill)
      expect(store.currentSkill).toEqual(mockSkill)
    })

    it('interpolates the skill id correctly for different ids', async () => {
      mockGet.mockResolvedValue({ data: { id: 99, name: 'Skill 99' } })
      const store = useSkillStore()

      await store.fetchDetail(99)
      expect(mockGet).toHaveBeenCalledWith('/skills/99')
    })
  })

  // =====================================================================
  // POST /api/v1/market/skills/:id/install — Install skill
  // =====================================================================

  describe('POST /market/skills/:id/install — install', () => {
    it('calls POST with correct path for given skill id', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useSkillStore()

      await store.install(7)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/market/skills/7/install')
    })

    it('does not send a request body for install', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useSkillStore()

      await store.install(10)

      // The store calls request.post(path) without a second argument,
      // so axios receives only 1 argument
      expect(mockPost).toHaveBeenCalledWith('/market/skills/10/install')
    })
  })

  // =====================================================================
  // GET /api/v1/market/skills/:id/install-status — Check install status
  // =====================================================================

  describe('GET /market/skills/:id/install-status — checkInstallStatus', () => {
    it('calls GET with correct path', async () => {
      mockGet.mockResolvedValue({
        data: { installed: true, version: '1.0.0' }
      })
      const store = useSkillStore()

      const result = await store.checkInstallStatus(15)

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/market/skills/15/install-status')
      expect(result).toEqual({ installed: true, version: '1.0.0' })
    })

    it('returns install status data correctly', async () => {
      mockGet.mockResolvedValue({
        data: { installed: false }
      })
      const store = useSkillStore()

      const result = await store.checkInstallStatus(1)

      expect(result).toEqual({ installed: false })
    })
  })
})
