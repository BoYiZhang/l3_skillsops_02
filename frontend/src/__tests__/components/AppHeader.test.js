import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AppHeader from '@/components/layout/AppHeader.vue'
import ElementPlus from 'element-plus'

const mockPush = vi.fn()
const mockLogout = vi.fn()
const mockRoute = { name: 'Market' }

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => mockRoute,
}))

vi.mock('@element-plus/icons-vue', () => ({
  ArrowDown: { name: 'ArrowDown', template: '<span>icon</span>' },
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => ({
    user: { username: 'testuser', roles: ['USER'] },
    logout: mockLogout,
  })),
}))

describe('AppHeader', () => {
  function mountHeader() {
    return mount(AppHeader, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          'el-icon': true,
          'el-avatar': { template: '<span class="avatar-stub"><slot /></span>' },
        },
      },
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockRoute.name = 'Market'
  })

  it('renders logo "SkillsOps"', () => {
    const wrapper = mountHeader()
    expect(wrapper.text()).toContain('SkillsOps')
  })

  it('renders "市场" menu item', () => {
    const wrapper = mountHeader()
    expect(wrapper.text()).toContain('市场')
  })

  it('does NOT show breadcrumb when route name is not SkillDetail', () => {
    mockRoute.name = 'Market'
    const wrapper = mountHeader()
    expect(wrapper.find('.breadcrumb').exists()).toBe(false)
  })

  it('shows breadcrumb when route name is SkillDetail', () => {
    mockRoute.name = 'SkillDetail'
    const wrapper = mountHeader()
    expect(wrapper.find('.breadcrumb').exists()).toBe(true)
    expect(wrapper.text()).toContain('Skill 详情')
  })

  it('renders username from auth store', () => {
    const wrapper = mountHeader()
    expect(wrapper.text()).toContain('testuser')
  })

  it('renders first letter of username in avatar', () => {
    const wrapper = mountHeader()
    // The component uses auth.user?.username?.charAt(0)?.toUpperCase()
    // The first letter 'T' should be inside the user-info span
    expect(wrapper.text()).toContain('T')
  })

  it('nav() pushes to the given route index', async () => {
    const wrapper = mountHeader()
    const menu = wrapper.findComponent({ name: 'ElMenu' })
    await menu.vm.$emit('select', '/market')
    expect(mockPush).toHaveBeenCalledWith('/market')
  })

  it('handleCommand("workspace") pushes to /workspace', async () => {
    const wrapper = mountHeader()
    const dropdown = wrapper.findComponent({ name: 'ElDropdown' })
    await dropdown.vm.$emit('command', 'workspace')
    expect(mockPush).toHaveBeenCalledWith('/workspace')
  })

  it('handleCommand("logout") calls auth.logout() and pushes /login', async () => {
    const wrapper = mountHeader()
    const dropdown = wrapper.findComponent({ name: 'ElDropdown' })
    await dropdown.vm.$emit('command', 'logout')
    expect(mockLogout).toHaveBeenCalled()
    expect(mockPush).toHaveBeenCalledWith('/login')
  })
})
