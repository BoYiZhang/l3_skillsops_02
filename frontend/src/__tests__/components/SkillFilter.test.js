import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

// Use vi.hoisted so mock variables are available when vi.mock factory is hoisted
const { mockGet } = vi.hoisted(() => ({
  mockGet: vi.fn().mockResolvedValue({ data: [] }),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

vi.mock('@element-plus/icons-vue', () => ({
  Refresh: { name: 'Refresh', template: '<span>icon</span>' },
}))

import SkillFilter from '@/components/skill/SkillFilter.vue'

describe('SkillFilter', () => {
  function mountComponent() {
    return mount(SkillFilter, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          'el-icon': true,
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ data: [] })
  })

  it('renders all filter controls (category select, keyword input, sort select, refresh button)', async () => {
    mockGet.mockResolvedValue({ data: [{ id: 1, name: '自动化脚本' }, { id: 2, name: '数据处理' }] })
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()

    // 2 selects (category + sort) + 1 input + 1 button
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    expect(selects).toHaveLength(2)

    const inputs = wrapper.findAllComponents({ name: 'ElInput' })
    expect(inputs).toHaveLength(1)

    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    expect(buttons).toHaveLength(1)
  })

  it('emits filter when keyword changes (enter key)', async () => {
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()

    // Set keyword on the component's reactive state and call search directly
    wrapper.vm.localFilter.keyword = 'test keyword'
    wrapper.vm.search()
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('filter')).toBeTruthy()
    expect(wrapper.emitted('filter')[0][0]).toMatchObject({
      categoryId: null,
      keyword: 'test keyword',
      sortBy: 'NEWEST',
    })
  })

  it('emits filter when category select changes', async () => {
    mockGet.mockResolvedValue({ data: [{ id: 1, name: '自动化脚本' }] })
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()

    // First select is category — update modelValue and emit change
    const categorySelect = wrapper.findAllComponents({ name: 'ElSelect' })[0]
    const option = categorySelect.findComponent({ name: 'ElOption' })
    if (option.exists()) {
      await option.trigger('click')
    }
    // Directly emit change via component instance
    wrapper.vm.localFilter.categoryId = 1
    categorySelect.vm.$emit('change', 1)
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('filter')).toBeTruthy()
    expect(wrapper.emitted('filter')[0][0]).toMatchObject({
      categoryId: 1,
      keyword: '',
      sortBy: 'NEWEST',
    })
  })

  it('emits filter when sort select changes', async () => {
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()

    // Last select is sort select
    const sortSelect = wrapper.findAllComponents({ name: 'ElSelect' })[1]
    wrapper.vm.localFilter.sortBy = 'HOT'
    sortSelect.vm.$emit('change', 'HOT')
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('filter')).toBeTruthy()
    expect(wrapper.emitted('filter')[0][0]).toMatchObject({
      categoryId: null,
      keyword: '',
      sortBy: 'HOT',
    })
  })

  it('emits refresh when refresh button clicked', async () => {
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()

    const button = wrapper.findComponent({ name: 'ElButton' })
    await button.trigger('click')

    expect(wrapper.emitted('refresh')).toBeTruthy()
  })

  it('has correct default sort value (NEWEST)', async () => {
    const wrapper = mountComponent()
    await wrapper.vm.$nextTick()

    // The default sortBy in localFilter is 'NEWEST'
    expect(wrapper.vm.localFilter.sortBy).toBe('NEWEST')
  })

  it('fetches categories on mount from /admin/categories', () => {
    mountComponent()
    expect(mockGet).toHaveBeenCalledWith('/admin/categories')
  })
})
