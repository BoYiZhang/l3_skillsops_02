import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

// Use vi.hoisted so mock variables are available when vi.mock factory is hoisted
const { mockPost, mockAuthStoreState, mockSkillStoreState } = vi.hoisted(() => ({
  mockPost: vi.fn().mockResolvedValue({ data: {} }),
  // Shared state so both component and tests reference the same object
  mockAuthStoreState: { isAdmin: false, user: { username: 'testuser', roles: ['USER'] }, logout: vi.fn() },
  mockSkillStoreState: { install: vi.fn().mockResolvedValue({}), checkInstallStatus: vi.fn().mockResolvedValue({ installed: false }) },
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: mockPost,
    put: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
    ElMessageBox: {
      prompt: vi.fn().mockResolvedValue({ value: 'test reason' }),
      confirm: vi.fn(),
    }
  }
})

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => mockAuthStoreState)
}))

vi.mock('@/stores/skill', () => ({
  useSkillStore: vi.fn(() => mockSkillStoreState)
}))

import SkillInfo from '@/components/skill/SkillInfo.vue'

describe('SkillInfo', () => {
  const baseSkill = {
    id: 1,
    name: 'Test Skill',
    categoryName: '自动化脚本',
    authorName: 'author1',
    latestVersion: '1.0.0',
    status: 'PUBLISHED',
    avgRating: 4.5,
    installCount: 100,
    repoUrl: 'https://github.com/test/skill',
    description: 'A great skill for testing',
  }

  function mountComponent(props = {}) {
    return mount(SkillInfo, {
      props: {
        skill: { ...baseSkill, ...props },
      },
      global: {
        plugins: [ElementPlus],
        stubs: {
          'el-icon': true,
          'el-avatar': true,
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockPost.mockResolvedValue({ data: {} })
    mockAuthStoreState.isAdmin = false
    mockSkillStoreState.checkInstallStatus.mockResolvedValue({ installed: false })
  })

  it('renders skill name from props', () => {
    const wrapper = mountComponent({ name: 'My Custom Skill' })
    expect(wrapper.text()).toContain('My Custom Skill')
  })

  it('renders category name', () => {
    const wrapper = mountComponent({ categoryName: '数据处理' })
    expect(wrapper.text()).toContain('数据处理')
  })

  it('renders author name', () => {
    const wrapper = mountComponent({ authorName: 'zhangsan' })
    expect(wrapper.text()).toContain('zhangsan')
  })

  it('renders version or "暂无" when empty', () => {
    // With version
    const wrapper = mountComponent({ latestVersion: '2.0.0' })
    expect(wrapper.text()).toContain('2.0.0')

    // Without version
    const wrapper2 = mountComponent({ latestVersion: '' })
    expect(wrapper2.text()).toContain('暂无')
  })

  it('renders status tag with correct type', () => {
    const wrapper = mountComponent({ status: 'DRAFT' })
    expect(wrapper.text()).toContain('草稿')

    const wrapper2 = mountComponent({ status: 'PUBLISHED' })
    expect(wrapper2.text()).toContain('已上架')

    const wrapper3 = mountComponent({ status: 'DELISTED' })
    expect(wrapper3.text()).toContain('已下架')
  })

  it('shows install button when not installed', async () => {
    mockSkillStoreState.checkInstallStatus.mockResolvedValue({ installed: false })

    const wrapper = mountComponent()
    // checkStatus() is called synchronously in setup, but it's async
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    // The install button should be visible since installed is false
    const btns = wrapper.findAllComponents({ name: 'ElButton' })
    const installBtn = btns.find(btn => btn.text().includes('安装'))
    expect(installBtn).toBeTruthy()
  })

  it('does NOT show admin buttons when user is not admin', async () => {
    mockAuthStoreState.isAdmin = false

    const wrapper = mountComponent({ status: 'PENDING_APPROVAL' })
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const approveBtn = buttons.find(btn => btn.text().includes('通过'))
    const rejectBtn = buttons.find(btn => btn.text().includes('拒绝'))

    expect(approveBtn).toBeFalsy()
    expect(rejectBtn).toBeFalsy()
  })

  it('shows admin action buttons when user is admin and status is PENDING_APPROVAL', async () => {
    mockAuthStoreState.isAdmin = true
    mockSkillStoreState.checkInstallStatus.mockResolvedValue({ installed: true })

    const wrapper = mountComponent({ status: 'PENDING_APPROVAL' })
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const approveBtn = buttons.find(btn => btn.text().includes('通过'))
    const rejectBtn = buttons.find(btn => btn.text().includes('拒绝'))

    expect(approveBtn).toBeTruthy()
    expect(rejectBtn).toBeTruthy()
  })
})
