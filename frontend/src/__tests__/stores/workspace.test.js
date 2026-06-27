import { setActivePinia, createPinia } from 'pinia'
import { useWorkspaceStore } from '@/stores/workspace'
import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock axios
vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn()
  }
}))

describe('useWorkspaceStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useWorkspaceStore()
  })

  describe('initial state', () => {
    it('has empty mySkills array', () => {
      expect(store.mySkills).toEqual([])
    })

    it('has empty installedSkills array', () => {
      expect(store.installedSkills).toEqual([])
    })

    it('has empty pendingSkills array', () => {
      expect(store.pendingSkills).toEqual([])
    })

    it('has null stats', () => {
      expect(store.stats).toBeNull()
    })

    it('has loading set to false', () => {
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchMySkills', () => {
    it('updates mySkills on success', async () => {
      const request = await import('@/utils/request')
      const mockSkills = [
        { id: 1, name: 'My Skill A', status: 'PUBLISHED' },
        { id: 2, name: 'My Skill B', status: 'DRAFT' }
      ]
      request.default.get.mockResolvedValue({
        data: { records: mockSkills, total: 2 }
      })
      const result = await store.fetchMySkills({ page: 1 })
      expect(store.mySkills).toEqual(mockSkills)
      expect(result.records).toEqual(mockSkills)
    })

    it('passes params to API', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { records: [], total: 0 }
      })
      await store.fetchMySkills({ page: 2, size: 10 })
      expect(request.default.get).toHaveBeenCalledWith('/workspace/my-skills', {
        params: { page: 2, size: 10 }
      })
    })

    it('handles error gracefully', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockRejectedValue(new Error('Network error'))
      // fetchMySkills uses try/finally, so the error propagates but loading resets
      try {
        await store.fetchMySkills()
      } catch (e) {
        // expected — the store rethrows after finally
      }
      // State should not crash; loading resets
      expect(store.loading).toBe(false)
    })
  })

  describe('createSkill', () => {
    it('posts skill data and returns result', async () => {
      const request = await import('@/utils/request')
      const form = { name: 'New Skill', categoryId: 1, description: 'Desc' }
      request.default.post.mockResolvedValue({
        data: { id: 10, name: 'New Skill' }
      })
      const result = await store.createSkill(form)
      expect(result).toEqual({ id: 10, name: 'New Skill' })
      expect(request.default.post).toHaveBeenCalledWith('/skills', form)
    })
  })

  describe('submitSkill', () => {
    it('posts submit for the given skill id', async () => {
      const request = await import('@/utils/request')
      request.default.post.mockResolvedValue({ data: {} })
      await store.submitSkill(5)
      expect(request.default.post).toHaveBeenCalledWith('/skills/5/submit')
    })
  })

  describe('publishVersion', () => {
    it('posts version data for the given skill', async () => {
      const request = await import('@/utils/request')
      const form = { version: '1.2.0', changelog: 'Bug fixes' }
      request.default.post.mockResolvedValue({ data: {} })
      await store.publishVersion(7, form)
      expect(request.default.post).toHaveBeenCalledWith('/skills/7/versions', form)
    })
  })

  describe('fetchInstalled', () => {
    it('updates installedSkills on success', async () => {
      const request = await import('@/utils/request')
      const mockInstalled = [{ id: 1, name: 'Installed Skill', installedVersion: '1.0.0' }]
      request.default.get.mockResolvedValue({
        data: { records: mockInstalled, total: 1 }
      })
      await store.fetchInstalled()
      expect(store.installedSkills).toEqual(mockInstalled)
    })

    it('passes params to API', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { records: [], total: 0 }
      })
      await store.fetchInstalled({ page: 1, size: 20 })
      expect(request.default.get).toHaveBeenCalledWith('/workspace/installed', {
        params: { page: 1, size: 20 }
      })
    })
  })

  describe('fetchPendingSkills', () => {
    it('updates pendingSkills on success', async () => {
      const request = await import('@/utils/request')
      const mockPending = [{ id: 10, name: 'Pending Skill', status: 'PENDING' }]
      request.default.get.mockResolvedValue({
        data: { records: mockPending, total: 1 }
      })
      await store.fetchPendingSkills()
      expect(store.pendingSkills).toEqual(mockPending)
    })

    it('passes params to API', async () => {
      const request = await import('@/utils/request')
      request.default.get.mockResolvedValue({
        data: { records: [], total: 0 }
      })
      await store.fetchPendingSkills({ page: 1, size: 10 })
      expect(request.default.get).toHaveBeenCalledWith('/admin/pending-skills', {
        params: { page: 1, size: 10 }
      })
    })
  })

  describe('approveSkill', () => {
    it('posts approve for the given skill id', async () => {
      const request = await import('@/utils/request')
      request.default.post.mockResolvedValue({ data: {} })
      await store.approveSkill(3)
      expect(request.default.post).toHaveBeenCalledWith('/admin/skills/3/approve')
    })
  })

  describe('rejectSkill', () => {
    it('posts reject with reason for the given skill id', async () => {
      const request = await import('@/utils/request')
      request.default.post.mockResolvedValue({ data: {} })
      await store.rejectSkill(4, 'Not up to standard')
      expect(request.default.post).toHaveBeenCalledWith('/admin/skills/4/reject', {
        reason: 'Not up to standard'
      })
    })
  })

  describe('delistSkill', () => {
    it('posts delist with reason for the given skill id', async () => {
      const request = await import('@/utils/request')
      request.default.post.mockResolvedValue({ data: {} })
      await store.delistSkill(6, 'Security issue')
      expect(request.default.post).toHaveBeenCalledWith('/admin/skills/6/delist', {
        reason: 'Security issue'
      })
    })
  })

  describe('fetchStats', () => {
    it('updates stats on success', async () => {
      const request = await import('@/utils/request')
      const mockStats = { totalSkills: 100, totalUsers: 50, totalInstalls: 500 }
      request.default.get.mockResolvedValue({
        data: mockStats
      })
      const result = await store.fetchStats()
      expect(store.stats).toEqual(mockStats)
      expect(result).toEqual(mockStats)
      expect(request.default.get).toHaveBeenCalledWith('/admin/stats')
    })
  })
})
