import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

const { mockFetchPending, mockApproveSkill, mockRejectSkill, mockPendingSkillsArr } = vi.hoisted(() => {
  const f = vi.fn().mockResolvedValue({ records: [] })
  const a = vi.fn().mockResolvedValue({})
  const r = vi.fn().mockResolvedValue({})
  const arr = []
  return { mockFetchPending: f, mockApproveSkill: a, mockRejectSkill: r, mockPendingSkillsArr: arr }
})

vi.mock('@/stores/workspace', () => ({
  useWorkspaceStore: vi.fn(() => ({
    pendingSkills: mockPendingSkillsArr,
    fetchPendingSkills: mockFetchPending,
    approveSkill: mockApproveSkill,
    rejectSkill: mockRejectSkill
  }))
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
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

import { ElMessageBox } from 'element-plus'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
    ElMessageBox: { prompt: vi.fn(), confirm: vi.fn() }
  }
})

import ReviewTab from '@/components/workspace/ReviewTab.vue'

const mockPendingData = [
  { id: 1, name: 'PendingSkill', authorName: '张三', categoryName: 'AI工具', createTime: '2025-01-01' },
  { id: 2, name: 'AnotherSkill', authorName: '李四', categoryName: '自动化脚本', createTime: '2025-01-02' }
]

describe('ReviewTab', () => {
  function mountComponent() {
    return mount(ReviewTab, {
      global: {
        plugins: [ElementPlus],
        stubs: { 'el-icon': true }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockFetchPending.mockResolvedValue({ records: [] })
    mockPendingSkillsArr.length = 0
  })

  it('renders table with correct columns', async () => {
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    const headers = wrapper.findAll('.el-table__header-wrapper th')
    const headerTexts = headers.map(h => h.text().trim())
    expect(headerTexts).toContain('名称')
    expect(headerTexts).toContain('作者')
    expect(headerTexts).toContain('分类')
    expect(headerTexts).toContain('申请时间')
    expect(headerTexts).toContain('操作')
  })

  it('shows empty text "暂无待审核Skill"', async () => {
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('暂无待审核Skill')
  })

  it('shows loading state', () => {
    const wrapper = mountComponent()
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('renders "通过" button per skill row', async () => {
    const wrapper = mountComponent()
    wrapper.vm.list = mockPendingData
    await wrapper.vm.$nextTick()

    const successBtns = wrapper.findAll('.el-button--success')
    expect(successBtns.length).toBe(2)
    expect(successBtns[0].text()).toBe('通过')
  })

  it('renders "拒绝" button per skill row', async () => {
    const wrapper = mountComponent()
    wrapper.vm.list = mockPendingData
    await wrapper.vm.$nextTick()

    const dangerBtns = wrapper.findAll('.el-button--danger')
    expect(dangerBtns.length).toBe(2)
    expect(dangerBtns[0].text()).toBe('拒绝')
  })

  it('renders pending skills data in table', async () => {
    const wrapper = mountComponent()
    wrapper.vm.list = mockPendingData
    await wrapper.vm.$nextTick()

    const body = wrapper.find('.el-table__body-wrapper tbody')
    expect(body.text()).toContain('PendingSkill')
    expect(body.text()).toContain('张三')
    expect(body.text()).toContain('AI工具')
  })

  it('calls approveSkill when "通过" clicked', async () => {
    const wrapper = mountComponent()
    wrapper.vm.list = [{ id: 1, name: 'PendingSkill', authorName: '张三', categoryName: 'AI工具', createTime: '2025-01-01' }]
    await wrapper.vm.$nextTick()

    const approveBtn = wrapper.find('.el-button--success')
    await approveBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(mockApproveSkill).toHaveBeenCalledWith(1)
  })
})
