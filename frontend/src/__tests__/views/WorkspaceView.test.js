import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import WorkspaceView from '@/views/WorkspaceView.vue'

// Mock router
vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: { tab: 'published' }
  })
}))

// Use vi.hoisted for mutable mock state (vi.mock factory is hoisted)
const { useAuthStoreMock } = vi.hoisted(() => ({
  useAuthStoreMock: vi.fn(() => ({ isAdmin: false }))
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: useAuthStoreMock
}))

describe('WorkspaceView', () => {
  function mountView() {
    return mount(WorkspaceView, {
      global: {
        stubs: {
          AppHeader: { template: '<div class="app-header-stub">AppHeader</div>' },
          MySkillsTab: { template: '<div class="my-skills-tab-stub">MySkills</div>' },
          InstalledTab: { template: '<div class="installed-tab-stub">Installed</div>' },
          ReviewTab: { template: '<div class="review-tab-stub">Review</div>' },
          StatsTab: { template: '<div class="stats-tab-stub">Stats</div>' },
          UserManageTab: { template: '<div class="user-manage-tab-stub">Users</div>' },
          CategoryManageTab: { template: '<div class="category-manage-tab-stub">Categories</div>' },
          'el-tabs': { template: '<div class="el-tabs-stub"><slot /></div>' },
          'el-tab-pane': { template: '<div class="el-tab-pane-stub" :data-name="name"><slot /></div>', props: ['label', 'name'] }
        }
      }
    })
  }

  // Helper: find all tab pane names rendered (from el-tab-pane's name attribute)
  function getTabPaneNames(wrapper) {
    return wrapper.findAll('.el-tab-pane-stub').map(el => el.attributes('data-name'))
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    useAuthStoreMock.mockReturnValue({ isAdmin: false })
  })

  describe('component mount', () => {
    it('mounts and renders the page', () => {
      const wrapper = mountView()
      expect(wrapper.find('.workspace-page').exists()).toBe(true)
    })

    it('renders AppHeader component', () => {
      const wrapper = mountView()
      expect(wrapper.find('.app-header-stub').exists()).toBe(true)
    })

    it('renders workspace content area', () => {
      const wrapper = mountView()
      expect(wrapper.find('.workspace-content').exists()).toBe(true)
    })

    it('shows published tab content by default', () => {
      const wrapper = mountView()
      // activeTab defaults to 'published', so MySkillsTab should be visible
      expect(wrapper.find('.my-skills-tab-stub').exists()).toBe(true)
    })

    it('does not show installed tab content when on published tab', () => {
      const wrapper = mountView()
      // Only one tab content is visible at a time (v-if)
      expect(wrapper.find('.installed-tab-stub').exists()).toBe(false)
    })
  })

  describe('tab pane visibility for regular user', () => {
    it('shows published and installed tab labels', () => {
      const wrapper = mountView()
      const names = getTabPaneNames(wrapper)
      expect(names).toContain('published')
      expect(names).toContain('installed')
    })

    it('hides admin-only tab labels', () => {
      const wrapper = mountView()
      const names = getTabPaneNames(wrapper)
      expect(names).not.toContain('review')
      expect(names).not.toContain('stats')
      expect(names).not.toContain('users')
      expect(names).not.toContain('categories')
    })

    it('does not render admin tab content stubs', () => {
      const wrapper = mountView()
      expect(wrapper.find('.review-tab-stub').exists()).toBe(false)
      expect(wrapper.find('.stats-tab-stub').exists()).toBe(false)
      expect(wrapper.find('.user-manage-tab-stub').exists()).toBe(false)
      expect(wrapper.find('.category-manage-tab-stub').exists()).toBe(false)
    })
  })

  describe('tab visibility for admin user', () => {
    it('shows all 6 tab labels', () => {
      useAuthStoreMock.mockReturnValue({ isAdmin: true })
      const wrapper = mountView()
      const names = getTabPaneNames(wrapper)
      expect(names).toContain('published')
      expect(names).toContain('installed')
      expect(names).toContain('review')
      expect(names).toContain('stats')
      expect(names).toContain('users')
      expect(names).toContain('categories')
    })

    it('shows published tab content by default', () => {
      useAuthStoreMock.mockReturnValue({ isAdmin: true })
      const wrapper = mountView()
      expect(wrapper.find('.my-skills-tab-stub').exists()).toBe(true)
    })
  })
})
