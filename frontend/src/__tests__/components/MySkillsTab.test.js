import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

const {
  mockGet,
  mockPut,
  mockFetchMySkills,
  mockCreateSkill,
  mockSubmitSkill,
  mockPublishVersion,
  mockMySkillsArr,
} = vi.hoisted(() => {
  const mockGet = vi.fn().mockResolvedValue({ data: [] })
  const mockPut = vi.fn().mockResolvedValue({ data: {} })
  const mockFetchMySkills = vi.fn().mockResolvedValue({ records: [] })
  const mockCreateSkill = vi.fn().mockResolvedValue({})
  const mockSubmitSkill = vi.fn().mockResolvedValue({})
  const mockPublishVersion = vi.fn().mockResolvedValue({})
  const mockMySkillsArr = []
  return {
    mockGet,
    mockPut,
    mockFetchMySkills,
    mockCreateSkill,
    mockSubmitSkill,
    mockPublishVersion,
    mockMySkillsArr,
  }
})

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: mockPut,
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ name: 'Market' })
}))

vi.mock('@element-plus/icons-vue', () => ({
  Refresh: { name: 'Refresh', template: '<span>icon</span>' }
}))

vi.mock('@/stores/workspace', () => ({
  useWorkspaceStore: vi.fn(() => ({
    mySkills: mockMySkillsArr,
    fetchMySkills: mockFetchMySkills,
    createSkill: mockCreateSkill,
    submitSkill: mockSubmitSkill,
    publishVersion: mockPublishVersion
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

import MySkillsTab from '@/components/workspace/MySkillsTab.vue'

const mockSkillDraft = {
  id: 1, name: 'MySkill', description: 'A skill description',
  categoryId: 1, categoryName: 'AI工具',
  repoUrl: 'https://github.com/test', docUrl: 'https://docs.test',
  status: 'DRAFT', latestVersion: '0.1.0', installCount: 5
}
const mockSkillPublished = {
  id: 2, name: 'PublishedSkill', description: 'Published desc',
  categoryId: 2, categoryName: '自动化脚本',
  repoUrl: '', docUrl: '',
  status: 'PUBLISHED', latestVersion: '1.0.0', installCount: 100
}

describe('MySkillsTab', () => {
  function mountComponent() {
    return mount(MySkillsTab, {
      global: {
        plugins: [ElementPlus],
        stubs: { 'el-icon': true }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ data: [] })
    mockFetchMySkills.mockResolvedValue({ records: [] })
    mockMySkillsArr.length = 0
  })

  it('renders "创建 Skill" button', () => {
    const wrapper = mountComponent()
    const btn = wrapper.find('.el-button--primary')
    expect(btn.text()).toBe('创建 Skill')
  })

  it('renders table with correct columns', async () => {
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    const headers = wrapper.findAll('.el-table__header-wrapper th')
    const headerTexts = headers.map(h => h.text().trim())
    expect(headerTexts).toContain('名称')
    expect(headerTexts).toContain('分类')
    expect(headerTexts).toContain('状态')
    expect(headerTexts).toContain('版本')
    expect(headerTexts).toContain('安装量')
    expect(headerTexts).toContain('操作')
  })

  it('shows "暂无发布的Skill" empty text', async () => {
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('暂无发布的Skill')
  })

  it('shows "提交审核" button for DRAFT status skills', async () => {
    mockMySkillsArr.push(mockSkillDraft)
    mockFetchMySkills.mockResolvedValue({ records: [mockSkillDraft] })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const warningBtns = wrapper.findAll('.el-button--warning')
    expect(warningBtns.length).toBeGreaterThanOrEqual(1)
    const submitBtn = warningBtns.find(b => b.text() === '提交审核')
    expect(submitBtn.exists()).toBe(true)
  })

  it('shows "发新版" button for PUBLISHED status skills', async () => {
    mockMySkillsArr.push(mockSkillPublished)
    mockFetchMySkills.mockResolvedValue({ records: [mockSkillPublished] })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const successBtns = wrapper.findAll('.el-button--success')
    const versionBtn = successBtns.find(b => b.text() === '发新版')
    expect(versionBtn.exists()).toBe(true)
  })

  it('status tag shows correct type and label for each status', async () => {
    const skills = [
      { ...mockSkillDraft, status: 'DRAFT' },
      { ...mockSkillDraft, id: 3, status: 'PENDING_APPROVAL' },
      { ...mockSkillPublished, status: 'PUBLISHED' },
      { ...mockSkillDraft, id: 4, status: 'DELISTED' }
    ]
    mockMySkillsArr.push(...skills)
    mockFetchMySkills.mockResolvedValue({ records: skills })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const tags = wrapper.findAll('.el-table__body-wrapper .el-tag')
    expect(tags.length).toBe(4)

    // DRAFT -> info type, 草稿 label
    expect(tags[0].classes()).toContain('el-tag--info')
    expect(tags[0].text()).toBe('草稿')

    // PENDING_APPROVAL -> warning type, 审核中 label
    expect(tags[1].classes()).toContain('el-tag--warning')
    expect(tags[1].text()).toBe('审核中')

    // PUBLISHED -> success type, 已上架 label
    expect(tags[2].classes()).toContain('el-tag--success')
    expect(tags[2].text()).toBe('已上架')

    // DELISTED -> danger type, 已下架 label
    expect(tags[3].classes()).toContain('el-tag--danger')
    expect(tags[3].text()).toBe('已下架')
  })

  it('clicking "创建 Skill" opens create dialog', async () => {
    const wrapper = mountComponent()
    const createBtn = wrapper.find('.el-button--primary')
    await createBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.showCreate).toBe(true)
    expect(wrapper.find('.el-dialog').exists()).toBe(true)
    expect(wrapper.find('.el-dialog__title').text()).toBe('创建Skill')
  })

  it('create dialog has form fields (name, description, category select, repoUrl, docUrl)', async () => {
    const wrapper = mountComponent()
    wrapper.vm.showCreate = true
    await wrapper.vm.$nextTick()

    const dialog = wrapper.find('.el-dialog')
    const formLabels = dialog.findAll('.el-form-item__label')
    const labelTexts = formLabels.map(l => l.text().trim())
    expect(labelTexts).toContain('名称')
    expect(labelTexts).toContain('描述')
    expect(labelTexts).toContain('分类')
    expect(labelTexts).toContain('仓库地址')
    expect(labelTexts).toContain('文档链接')
  })

  it('clicking "发新版" opens version dialog', async () => {
    mockMySkillsArr.push(mockSkillPublished)
    mockFetchMySkills.mockResolvedValue({ records: [mockSkillPublished] })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const versionBtn = wrapper.find('.el-button--success')
    await versionBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.showVer).toBe(true)
  })

  it('version dialog has version and changelog fields', async () => {
    const wrapper = mountComponent()
    wrapper.vm.showVer = true
    wrapper.vm.curSkillId = 2
    await wrapper.vm.$nextTick()

    const dialogs = wrapper.findAll('.el-dialog')
    expect(dialogs.length).toBeGreaterThanOrEqual(1)
    const lastDialog = dialogs[dialogs.length - 1]
    const labels = lastDialog.findAll('.el-form-item__label')
    const labelTexts = labels.map(l => l.text().trim())
    expect(labelTexts).toContain('版本号')
    expect(labelTexts).toContain('更新说明')
  })

  it('clicking cancel closes dialog', async () => {
    const wrapper = mountComponent()
    wrapper.vm.showCreate = true
    await wrapper.vm.$nextTick()

    const dialog = wrapper.find('.el-dialog')
    const footer = dialog.find('.el-dialog__footer')
    const cancelBtn = footer.findAll('.el-button')[0]
    await cancelBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.showCreate).toBe(false)
  })

  it('编辑 button opens dialog in edit mode with pre-filled form', async () => {
    mockGet.mockResolvedValue({ data: [{ id: 1, name: 'AI工具' }] })
    mockMySkillsArr.push(mockSkillDraft)
    mockFetchMySkills.mockResolvedValue({ records: [mockSkillDraft] })
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    const editBtns = wrapper.findAll('.el-table__body-wrapper .el-button--small')
    const editBtn = editBtns.find(b => b.text() === '编辑')
    expect(editBtn.exists()).toBe(true)
    await editBtn.trigger('click')
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.showCreate).toBe(true)
    expect(wrapper.vm.editing).toEqual(mockSkillDraft)
    expect(wrapper.vm.form.name).toBe('MySkill')
    expect(wrapper.vm.form.description).toBe('A skill description')
    expect(wrapper.vm.form.categoryId).toBe(1)
    expect(wrapper.vm.form.repoUrl).toBe('https://github.com/test')
    expect(wrapper.vm.form.docUrl).toBe('https://docs.test')
    expect(wrapper.find('.el-dialog__title').text()).toBe('编辑Skill')
  })
})
