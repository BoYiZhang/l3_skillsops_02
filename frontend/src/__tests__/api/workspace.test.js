/**
 * API Contract Tests — Workspace endpoints
 *
 * Verifies that the workspace-related API calls use correct:
 * - HTTP methods
 * - URL paths with proper parameter interpolation
 * - Request bodies for create/update/admin operations
 * - Query parameters for paginated endpoints
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

import { useWorkspaceStore } from '@/stores/workspace'
import { setActivePinia, createPinia } from 'pinia'

describe('Workspace API Contract Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  // =====================================================================
  // GET /api/v1/workspace/my-skills — My skills (paginated)
  // =====================================================================

  describe('GET /workspace/my-skills — fetchMySkills', () => {
    it('calls GET with correct path', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 20, current: 1 }
      })
      const store = useWorkspaceStore()

      await store.fetchMySkills()

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/workspace/my-skills', { params: {} })
    })

    it('passes pagination params', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 2 }
      })
      const store = useWorkspaceStore()

      await store.fetchMySkills({ page: 2, size: 10 })

      expect(mockGet).toHaveBeenCalledWith('/workspace/my-skills', {
        params: { page: 2, size: 10 }
      })
    })

    it('updates mySkills after successful fetch', async () => {
      const mockRecords = [
        { id: 1, name: 'My Skill A', status: 'DRAFT' },
        { id: 2, name: 'My Skill B', status: 'PUBLISHED' },
      ]
      mockGet.mockResolvedValue({
        data: { records: mockRecords, total: 2, size: 20, current: 1 }
      })
      const store = useWorkspaceStore()

      const result = await store.fetchMySkills()

      expect(store.mySkills).toEqual(mockRecords)
      expect(store.mySkills).toHaveLength(2)
      expect(result.total).toBe(2)
    })
  })

  // =====================================================================
  // GET /api/v1/workspace/installed — Installed skills (paginated)
  // =====================================================================

  describe('GET /workspace/installed — fetchInstalled', () => {
    it('calls GET with correct path', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 20, current: 1 }
      })
      const store = useWorkspaceStore()

      await store.fetchInstalled()

      expect(mockGet).toHaveBeenCalledWith('/workspace/installed', { params: {} })
    })

    it('passes pagination params', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 5, current: 3 }
      })
      const store = useWorkspaceStore()

      await store.fetchInstalled({ page: 3, size: 5 })

      expect(mockGet).toHaveBeenCalledWith('/workspace/installed', {
        params: { page: 3, size: 5 }
      })
    })

    it('updates installedSkills after successful fetch', async () => {
      const mockRecords = [
        { id: 1, skillId: 10, skillName: 'Installed A', version: '1.0.0', status: 'INSTALLED' },
      ]
      mockGet.mockResolvedValue({
        data: { records: mockRecords, total: 1, size: 20, current: 1 }
      })
      const store = useWorkspaceStore()

      await store.fetchInstalled()

      expect(store.installedSkills).toEqual(mockRecords)
      expect(store.installedSkills).toHaveLength(1)
    })
  })

  // =====================================================================
  // POST /api/v1/skills — Create skill
  // =====================================================================

  describe('POST /skills — createSkill', () => {
    it('calls POST with correct path and skill data', async () => {
      const createdSkill = {
        id: 100,
        name: 'New Skill',
        description: 'A new test skill',
        categoryId: 2,
        status: 'DRAFT',
      }
      mockPost.mockResolvedValue({ data: createdSkill })
      const store = useWorkspaceStore()

      const result = await store.createSkill({
        name: 'New Skill',
        description: 'A new test skill',
        categoryId: 2,
        repoUrl: 'https://github.com/test/repo',
        docUrl: 'https://docs.example.com',
      })

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/skills', {
        name: 'New Skill',
        description: 'A new test skill',
        categoryId: 2,
        repoUrl: 'https://github.com/test/repo',
        docUrl: 'https://docs.example.com',
      })
      expect(result).toEqual(createdSkill)
    })

    it('sends required fields only when optional fields are absent', async () => {
      mockPost.mockResolvedValue({ data: { id: 101, name: 'Minimal', status: 'DRAFT' } })
      const store = useWorkspaceStore()

      await store.createSkill({
        name: 'Minimal',
        description: 'Just enough',
        categoryId: 1,
      })

      expect(mockPost).toHaveBeenCalledWith('/skills', {
        name: 'Minimal',
        description: 'Just enough',
        categoryId: 1,
      })
    })
  })

  // =====================================================================
  // POST /api/v1/skills/:id/submit — Submit for approval
  // =====================================================================

  describe('POST /skills/:id/submit — submitSkill', () => {
    it('calls POST with correct path', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useWorkspaceStore()

      await store.submitSkill(50)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/skills/50/submit')
    })

    it('does not send a request body', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useWorkspaceStore()

      await store.submitSkill(77)

      expect(mockPost).toHaveBeenCalledWith('/skills/77/submit')
    })
  })

  // =====================================================================
  // POST /api/v1/skills/:id/versions — Publish version
  // =====================================================================

  describe('POST /skills/:id/versions — publishVersion', () => {
    it('calls POST with correct path and version data', async () => {
      mockPost.mockResolvedValue({
        data: { id: 1, skillId: 60, version: '1.0.0', changelog: 'First release' }
      })
      const store = useWorkspaceStore()

      await store.publishVersion(60, {
        version: '1.0.0',
        changelog: 'First release',
      })

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/skills/60/versions', {
        version: '1.0.0',
        changelog: 'First release',
      })
    })

    it('sends version without changelog when changelog is empty', async () => {
      mockPost.mockResolvedValue({ data: { id: 2, skillId: 61, version: '2.0.0' } })
      const store = useWorkspaceStore()

      await store.publishVersion(61, { version: '2.0.0' })

      expect(mockPost).toHaveBeenCalledWith('/skills/61/versions', {
        version: '2.0.0',
      })
    })
  })

  // =====================================================================
  // GET /api/v1/admin/pending-skills — Admin: pending skills (paginated)
  // =====================================================================

  describe('GET /admin/pending-skills — fetchPendingSkills', () => {
    it('calls GET with correct admin path', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 20, current: 1 }
      })
      const store = useWorkspaceStore()

      await store.fetchPendingSkills()

      expect(mockGet).toHaveBeenCalledWith('/admin/pending-skills', { params: {} })
    })

    it('passes pagination params', async () => {
      mockGet.mockResolvedValue({
        data: { records: [], total: 0, size: 20, current: 1 }
      })
      const store = useWorkspaceStore()

      await store.fetchPendingSkills({ page: 1, size: 50 })

      expect(mockGet).toHaveBeenCalledWith('/admin/pending-skills', {
        params: { page: 1, size: 50 }
      })
    })

    it('updates pendingSkills after successful fetch', async () => {
      const pendingRecords = [
        { id: 5, name: 'Pending Skill', status: 'PENDING_APPROVAL', authorName: 'user1' },
      ]
      mockGet.mockResolvedValue({
        data: { records: pendingRecords, total: 1, size: 20, current: 1 }
      })
      const store = useWorkspaceStore()

      await store.fetchPendingSkills()

      expect(store.pendingSkills).toEqual(pendingRecords)
    })
  })

  // =====================================================================
  // POST /api/v1/admin/skills/:id/approve — Admin: approve skill
  // =====================================================================

  describe('POST /admin/skills/:id/approve — approveSkill', () => {
    it('calls POST with correct admin path', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useWorkspaceStore()

      await store.approveSkill(33)

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/admin/skills/33/approve')
    })
  })

  // =====================================================================
  // POST /api/v1/admin/skills/:id/reject — Admin: reject skill
  // =====================================================================

  describe('POST /admin/skills/:id/reject — rejectSkill', () => {
    it('calls POST with correct path and reason body', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useWorkspaceStore()

      await store.rejectSkill(44, 'Does not meet quality standards')

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/admin/skills/44/reject', {
        reason: 'Does not meet quality standards',
      })
    })

    it('sends empty reason correctly', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useWorkspaceStore()

      await store.rejectSkill(45, '')

      expect(mockPost).toHaveBeenCalledWith('/admin/skills/45/reject', {
        reason: '',
      })
    })
  })

  // =====================================================================
  // POST /api/v1/admin/skills/:id/delist — Admin: delist skill
  // =====================================================================

  describe('POST /admin/skills/:id/delist — delistSkill', () => {
    it('calls POST with correct path and reason body', async () => {
      mockPost.mockResolvedValue({ data: { code: 200 } })
      const store = useWorkspaceStore()

      await store.delistSkill(55, 'Violates policy')

      expect(mockPost).toHaveBeenCalledTimes(1)
      expect(mockPost).toHaveBeenCalledWith('/admin/skills/55/delist', {
        reason: 'Violates policy',
      })
    })
  })

  // =====================================================================
  // GET /api/v1/admin/stats — Admin: dashboard stats
  // =====================================================================

  describe('GET /admin/stats — fetchStats', () => {
    it('calls GET with correct admin path', async () => {
      const mockStats = {
        totalSkills: 10,
        totalUsers: 5,
        totalInstalls: 20,
        avgRating: 4.2,
      }
      mockGet.mockResolvedValue({ data: mockStats })
      const store = useWorkspaceStore()

      const result = await store.fetchStats()

      expect(mockGet).toHaveBeenCalledTimes(1)
      expect(mockGet).toHaveBeenCalledWith('/admin/stats')
      expect(result).toEqual(mockStats)
      expect(store.stats).toEqual(mockStats)
    })
  })
})
