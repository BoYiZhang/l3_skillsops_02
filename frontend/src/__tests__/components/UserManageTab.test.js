import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'
import UserManageTab from '@/components/workspace/UserManageTab.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ name: 'Market' })
}))

vi.mock('@element-plus/icons-vue', () => ({
  Refresh: { name: 'Refresh', template: '<span>icon</span>' }
}))

const mockGet = vi.fn().mockResolvedValue({
  data: { records: [], total: 0 }
})
const mockPut = vi.fn().mockResolvedValue({ data: {} })

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: mockPut,
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

vi.mock('@/stores/workspace', () => ({
  useWorkspaceStore: vi.fn(() => ({}))
}))

import { ElMessage } from 'element-plus'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
    ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() }
  }
})

const mockUsers = [
  { id: 1, username: 'admin', email: 'admin@test.com', roles: ['ADMIN'], status: 'ACTIVE', createTime: '2025-01-01' },
  { id: 2, username: 'zhangsan', email: 'zhangsan@test.com', roles: ['USER'], status: 'ACTIVE', createTime: '2025-01-02' },
  { id: 3, username: 'lisi', email: 'lisi@test.com', roles: ['USER'], status: 'DISABLED', createTime: '2025-01-03' }
]

describe('UserManageTab', () => {
  function mountComponent() {
    return mount(UserManageTab, {
      global: {
        plugins: [ElementPlus],
        stubs: { 'el-icon': true }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ data: { records: [], total: 0 } })
  })

  it('renders table with correct columns', async () => {
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()
    const headers = wrapper.findAll('.el-table__header-wrapper th')
    const headerTexts = headers.map(h => h.text().trim())
    expect(headerTexts).toContain('ID')
    expect(headerTexts).toContain('用户名')
    expect(headerTexts).toContain('邮箱')
    expect(headerTexts).toContain('角色')
    expect(headerTexts).toContain('状态')
    expect(headerTexts).toContain('操作')
  })

  it('shows empty text "暂无用户"', async () => {
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('暂无用户')
  })

  it('shows loading state', () => {
    const wrapper = mountComponent()
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('renders user data in table', async () => {
    mockGet.mockResolvedValue({ data: { records: mockUsers, total: 3 } })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const body = wrapper.find('.el-table__body-wrapper tbody')
    expect(body.text()).toContain('admin')
    expect(body.text()).toContain('zhangsan')
    expect(body.text()).toContain('lisi')
  })

  it('shows role tags with ADMIN having danger type', async () => {
    mockGet.mockResolvedValue({ data: { records: mockUsers, total: 3 } })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const tags = wrapper.findAll('.el-table__body-wrapper .el-tag')
    // First row has ADMIN tag which should be danger type
    const adminTag = tags.find(t => t.text() === 'ADMIN')
    expect(adminTag.exists()).toBe(true)
    expect(adminTag.classes()).toContain('el-tag--danger')
  })

  it('shows status tags (ACTIVE -> success / "正常", else -> info / "禁用")', async () => {
    mockGet.mockResolvedValue({ data: { records: mockUsers, total: 3 } })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const tags = wrapper.findAll('.el-table__body-wrapper .el-tag')
    // Find "正常" tag
    const activeTag = tags.find(t => t.text() === '正常')
    expect(activeTag.exists()).toBe(true)
    expect(activeTag.classes()).toContain('el-tag--success')

    // Find "禁用" tag
    const disabledTag = tags.find(t => t.text() === '禁用')
    expect(disabledTag.exists()).toBe(true)
    expect(disabledTag.classes()).toContain('el-tag--info')
  })

  it('does NOT show toggle button for admin user', async () => {
    mockGet.mockResolvedValue({ data: { records: mockUsers, total: 3 } })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // In the admin row (first row), there should NOT be a toggle button
    const rows = wrapper.findAll('.el-table__body-wrapper tbody tr')
    // First row is admin, should not have warning or success button
    const adminRow = rows[0]
    expect(adminRow.find('.el-button--warning').exists()).toBe(false)
    expect(adminRow.find('.el-button--success').exists()).toBe(false)
  })

  it('shows toggle button for non-admin users', async () => {
    mockGet.mockResolvedValue({ data: { records: mockUsers, total: 3 } })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const rows = wrapper.findAll('.el-table__body-wrapper tbody tr')
    // Second row is an active user - should have "禁用" button (warning type)
    const zhangsanRow = rows[1]
    expect(zhangsanRow.text()).toContain('禁用')

    // Third row is disabled - should have "启用" button (success type)
    const lisiRow = rows[2]
    expect(lisiRow.text()).toContain('启用')
  })

  it('shows pagination when total > 0', async () => {
    mockGet.mockResolvedValue({ data: { records: mockUsers, total: 30 } })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.el-pagination').exists()).toBe(true)
  })
})
