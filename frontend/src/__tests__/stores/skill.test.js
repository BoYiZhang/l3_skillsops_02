import { setActivePinia, createPinia } from 'pinia'
import { useSkillStore } from '@/stores/skill'
import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock axios
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  }
}))

describe('useSkillStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useSkillStore()
  })

  describe('initial state', () => {
    it('has empty marketSkills array', () => {
      expect(store.marketSkills).toEqual([])
    })

    it('has null currentSkill', () => {
      expect(store.currentSkill).toBeNull()
    })

    it('has loading set to false', () => {
      expect(store.loading).toBe(false)
    })

    it('has total set to 0', () => {
      expect(store.total).toBe(0)
    })
  })

  describe('fetchMarket', () => {
    it('sets loading to true while fetching', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { records: [], total: 0 }
      })
      // loading is set to true synchronously before the first await
      const promise = store.fetchMarket()
      expect(store.loading).toBe(true)
      await promise
      expect(store.loading).toBe(false)
    })

    it('updates marketSkills and total on success', async () => {
      const request = await import('@/utils/request')
      const mockSkills = [
        { id: 1, name: 'Skill A' },
        { id: 2, name: 'Skill B' }
      ]
      request.default.get.mockResolvedValue({
        data: { records: mockSkills, total: 2 }
      })
      await store.fetchMarket({ page: 1, size: 10 })
      expect(store.marketSkills).toEqual(mockSkills)
      expect(store.total).toBe(2)
    })

    it('resets loading on error', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockRejectedValue(new Error('Network error'))
      // fetchMarket uses try/finally, so the error propagates but loading resets
      try {
        await store.fetchMarket()
      } catch (e) {
        // expected — the store rethrows after finally
      }
      expect(store.loading).toBe(false)
    })

    it('passes params to the API call', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { records: [], total: 0 }
      })
      await store.fetchMarket({ categoryId: 1, keyword: 'test', sortBy: 'HOT' })
      expect(request.default.get).toHaveBeenCalledWith('/market/skills', {
        params: { categoryId: 1, keyword: 'test', sortBy: 'HOT' }
      })
    })
  })

  describe('fetchDetail', () => {
    it('fetches skill detail and updates currentSkill', async () => {
      const request = await import('@/utils/request')
      const mockSkill = { id: 1, name: 'Test Skill', description: 'A test skill' }
      request.default.get.mockResolvedValue({
        data: mockSkill
      })
      const result = await store.fetchDetail(1)
      expect(result).toEqual(mockSkill)
      expect(store.currentSkill).toEqual(mockSkill)
      expect(request.default.get).toHaveBeenCalledWith('/skills/1')
    })

    it('returns data from API', async () => {
      const request = await import('@/utils/request')
      const mockSkill = { id: 2, name: 'Another Skill' }
      request.default.get.mockResolvedValue({
        data: mockSkill
      })
      const result = await store.fetchDetail(2)
      expect(result).toEqual(mockSkill)
    })
  })

  describe('install', () => {
    it('calls POST to install endpoint', async () => {
      const request = await import('@/utils/request')
      request.default.post.mockResolvedValue({ data: {} })
      await store.install(42)
      expect(request.default.post).toHaveBeenCalledWith('/market/skills/42/install')
    })
  })

  describe('checkInstallStatus', () => {
    it('returns install status data', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { installed: true, version: '1.0.0' }
      })
      const result = await store.checkInstallStatus(10)
      expect(result).toEqual({ installed: true, version: '1.0.0' })
      expect(request.default.get).toHaveBeenCalledWith('/market/skills/10/install-status')
    })

    it('returns not installed for missing skill', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { installed: false }
      })
      const result = await store.checkInstallStatus(999)
      expect(result.installed).toBe(false)
    })
  })

  describe('clearCurrent', () => {
    it('resets currentSkill to null', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { id: 1, name: 'Skill' }
      })
      await store.fetchDetail(1)
      expect(store.currentSkill).not.toBeNull()
      store.clearCurrent()
      expect(store.currentSkill).toBeNull()
    })
  })
})
