import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn().mockResolvedValue({ data: [] }),
  mockPost: vi.fn().mockResolvedValue({ data: {} }),
  mockPut: vi.fn().mockResolvedValue({ data: {} }),
  mockDelete: vi.fn().mockResolvedValue({ data: {} }),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ name: 'Market' })
}))

vi.mock('@/stores/workspace', () => ({
  useWorkspaceStore: vi.fn(() => ({}))
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
    ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() }
  }
})

import { ElMessage } from 'element-plus'
import CategoryManageTab from '@/components/workspace/CategoryManageTab.vue'

const mockCategories = [
  { id: 1, name: 'AI工具', description: '人工智能相关工具' },
  { id: 2, name: '自动化脚本', description: '自动化运维脚本' }
]

describe('CategoryManageTab', () => {
  function mountComponent() {
    return mount(CategoryManageTab, {
      global: {
        plugins: [ElementPlus],
        stubs: { 'el-icon': true }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ data: [] })
    mockPost.mockResolvedValue({ data: {} })
    mockPut.mockResolvedValue({ data: {} })
    mockDelete.mockResolvedValue({ data: {} })
  })

  it('renders "新增" button and input fields', () => {
    const wrapper = mountComponent()
    const btn = wrapper.find('.el-button--primary')
    expect(btn.text()).toBe('新增')
    expect(wrapper.findAll('.el-input').length).toBeGreaterThanOrEqual(2)
  })

  it('renders table with correct columns', async () => {
    mockGet.mockResolvedValue({ data: mockCategories })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const headers = wrapper.findAll('.el-table__header-wrapper th')
    const headerTexts = headers.map(h => h.text().trim())
    expect(headerTexts).toContain('ID')
    expect(headerTexts).toContain('名称')
    expect(headerTexts).toContain('描述')
    expect(headerTexts).toContain('操作')
  })

  it('displays loaded categories in table rows', async () => {
    mockGet.mockResolvedValue({ data: mockCategories })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const rows = wrapper.findAll('.el-table__body-wrapper tbody tr')
    expect(rows.length).toBe(2)
    expect(rows[0].text()).toContain('AI工具')
    expect(rows[0].text()).toContain('人工智能相关工具')
    expect(rows[1].text()).toContain('自动化脚本')
  })

  it('shows warning when creating with empty name', async () => {
    const wrapper = mountComponent()
    const button = wrapper.find('.el-button--primary')
    await button.trigger('click')
    expect(ElMessage.warning).toHaveBeenCalledWith('请输入名称')
    expect(mockPost).not.toHaveBeenCalled()
  })

  it('opens edit dialog when edit button clicked', async () => {
    mockGet.mockResolvedValue({ data: mockCategories })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const editButtons = wrapper.findAll('.el-table__body-wrapper .el-button--small')
    await editButtons[0].trigger('click')
    await wrapper.vm.$nextTick()

    const dialog = wrapper.find('.el-dialog')
    expect(dialog.exists()).toBe(true)
    expect(dialog.text()).toContain('编辑分类')
  })

  it('edit dialog has name and desc fields pre-filled', async () => {
    mockGet.mockResolvedValue({ data: mockCategories })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const editButtons = wrapper.findAll('.el-table__body-wrapper .el-button--small')
    await editButtons[0].trigger('click')
    await wrapper.vm.$nextTick()

    const dialog = wrapper.find('.el-dialog')
    expect(dialog.exists()).toBe(true)
    expect(wrapper.vm.editCat.name).toBe('AI工具')
    expect(wrapper.vm.editCat.desc).toBe('人工智能相关工具')
  })

  it('delete confirmation popover renders', async () => {
    mockGet.mockResolvedValue({ data: mockCategories })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const dangerButtons = wrapper.findAll('.el-button--danger')
    expect(dangerButtons.length).toBeGreaterThanOrEqual(1)
  })

  it('cancel button closes edit dialog', async () => {
    mockGet.mockResolvedValue({ data: mockCategories })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const editButtons = wrapper.findAll('.el-table__body-wrapper .el-button--small')
    await editButtons[0].trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.el-dialog').exists()).toBe(true)

    const dialogFooter = wrapper.find('.el-dialog__footer')
    const cancelBtn = dialogFooter.findAll('.el-button')[0]
    await cancelBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.editRow).toBeNull()
  })
})
