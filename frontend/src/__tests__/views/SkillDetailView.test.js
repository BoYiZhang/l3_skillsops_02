import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import SkillDetailView from '@/views/SkillDetailView.vue'

// Mock router
vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { id: '1' }
  })
}))

// Mock skill store
const mockFetchDetail = vi.fn()
const mockCheckInstallStatus = vi.fn()

vi.mock('@/stores/skill', () => ({
  useSkillStore: () => ({
    fetchDetail: mockFetchDetail,
    checkInstallStatus: mockCheckInstallStatus
  })
}))

describe('SkillDetailView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockFetchDetail.mockReset()
    mockCheckInstallStatus.mockReset()
  })

  // Helper to flush all pending microtasks
  async function flush() {
    await new Promise(resolve => setTimeout(resolve, 0))
  }

  function mountView() {
    return mount(SkillDetailView, {
      global: {
        stubs: {
          AppHeader: { template: '<div class="app-header-stub">AppHeader</div>' },
          SkillInfo: { template: '<div class="skill-info-stub"><slot /></div>', props: ['skill'] },
          VersionList: { template: '<div class="version-list-stub" />', props: ['skillId'] },
          RatingList: { template: '<div class="rating-list-stub" />', props: ['skillId', 'installed'] }
        },
        directives: {
          loading: {
            mounted() {},
            updated() {}
          }
        }
      }
    })
  }

  it('mounts and renders the page', async () => {
    mockFetchDetail.mockResolvedValue({ id: 1, name: 'Test Skill', description: 'A test' })
    mockCheckInstallStatus.mockResolvedValue({ installed: false })

    const wrapper = mountView()
    await flush()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.detail-page').exists()).toBe(true)
  })

  it('renders AppHeader component', async () => {
    mockFetchDetail.mockResolvedValue({ id: 1, name: 'Test' })
    mockCheckInstallStatus.mockResolvedValue({ installed: false })

    const wrapper = mountView()
    await flush()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.app-header-stub').exists()).toBe(true)
  })

  it('calls fetchDetail with route param id on mount', async () => {
    mockFetchDetail.mockResolvedValue({ id: 1, name: 'Test' })
    mockCheckInstallStatus.mockResolvedValue({ installed: false })

    mountView()
    await flush()
    expect(mockFetchDetail).toHaveBeenCalledWith('1')
  })

  it('calls checkInstallStatus after loading skill', async () => {
    mockFetchDetail.mockResolvedValue({ id: 1, name: 'Test' })
    mockCheckInstallStatus.mockResolvedValue({ installed: false })

    mountView()
    await flush()
    expect(mockCheckInstallStatus).toHaveBeenCalledWith(1)
  })

  it('shows SkillInfo when skill is loaded', async () => {
    const mockSkill = { id: 1, name: 'Test Skill', description: 'A test skill' }
    mockFetchDetail.mockResolvedValue(mockSkill)
    mockCheckInstallStatus.mockResolvedValue({ installed: true })

    const wrapper = mountView()
    await flush()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.skill-info-stub').exists()).toBe(true)
  })

  it('shows VersionList when skill is loaded', async () => {
    mockFetchDetail.mockResolvedValue({ id: 2, name: 'Another Skill' })
    mockCheckInstallStatus.mockResolvedValue({ installed: false })

    const wrapper = mountView()
    await flush()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.version-list-stub').exists()).toBe(true)
  })

  it('shows RatingList when skill is loaded', async () => {
    mockFetchDetail.mockResolvedValue({ id: 3, name: 'Third Skill' })
    mockCheckInstallStatus.mockResolvedValue({ installed: true })

    const wrapper = mountView()
    await flush()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.rating-list-stub').exists()).toBe(true)
  })

  it('does not show SkillInfo before skill loads', () => {
    // Keep fetchDetail pending so skill stays null
    mockFetchDetail.mockReturnValue(new Promise(() => {}))

    const wrapper = mountView()
    expect(wrapper.find('.skill-info-stub').exists()).toBe(false)
  })
})
