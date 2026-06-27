import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

const mockPush = vi.fn()
const mockFetchInstalled = vi.fn().mockResolvedValue({ records: [] })
const mockStoreInstalledSkills = []

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ name: 'Market' })
}))

vi.mock('@element-plus/icons-vue', () => ({
  Refresh: { name: 'Refresh', template: '<span>icon</span>' }
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: [] }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

vi.mock('@/stores/workspace', () => ({
  useWorkspaceStore: vi.fn(() => ({
    installedSkills: mockStoreInstalledSkills,
    fetchInstalled: mockFetchInstalled
  }))
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
    ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() }
  }
})

import InstalledTab from '@/components/workspace/InstalledTab.vue'

describe('InstalledTab', () => {
  function mountComponent() {
    return mount(InstalledTab, {
      global: {
        plugins: [ElementPlus],
        stubs: { 'el-icon': true }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockStoreInstalledSkills.length = 0
    mockFetchInstalled.mockResolvedValue({ records: [] })
  })

  it('renders table with correct columns', async () => {
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    const headers = wrapper.findAll('.el-table__header-wrapper th')
    const headerTexts = headers.map(h => h.text().trim())
    expect(headerTexts).toContain('名称')
    expect(headerTexts).toContain('描述')
    expect(headerTexts).toContain('版本')
    expect(headerTexts).toContain('状态')
    expect(headerTexts).toContain('操作')
  })

  it('shows empty text "暂无安装记录" when no data', async () => {
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('暂无安装记录')
  })

  it('shows loading state (v-loading)', () => {
    const wrapper = mountComponent()
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('renders rows with skill name, description, version', async () => {
    const rowData = [{ skillId: 1, skillName: 'TestSkill', skillDescription: 'A test skill', version: '1.0.0', status: 'PUBLISHED' }]
    mockStoreInstalledSkills.push(...rowData)
    mockFetchInstalled.mockResolvedValue({ records: rowData })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const body = wrapper.find('.el-table__body-wrapper tbody')
    expect(body.text()).toContain('TestSkill')
    expect(body.text()).toContain('A test skill')
    expect(body.text()).toContain('1.0.0')
  })

  it('shows "已下架" tag with danger type for DELISTED status', async () => {
    const rowData = [{ skillId: 1, skillName: 'OldSkill', skillDescription: 'Old', version: '1.0.0', status: 'DELISTED' }]
    mockStoreInstalledSkills.push(...rowData)
    mockFetchInstalled.mockResolvedValue({ records: rowData })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const tag = wrapper.find('.el-tag--danger')
    expect(tag.exists()).toBe(true)
    expect(tag.text()).toBe('已下架')
  })

  it('shows "可用" tag with success type for non-DELISTED status', async () => {
    const rowData = [{ skillId: 2, skillName: 'NewSkill', skillDescription: 'New', version: '2.0.0', status: 'PUBLISHED' }]
    mockStoreInstalledSkills.push(...rowData)
    mockFetchInstalled.mockResolvedValue({ records: rowData })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const tag = wrapper.find('.el-tag--success')
    expect(tag.exists()).toBe(true)
    expect(tag.text()).toBe('可用')
  })
})
