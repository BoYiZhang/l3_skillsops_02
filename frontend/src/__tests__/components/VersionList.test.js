import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

// Use vi.hoisted so mock variables are available when vi.mock factory is hoisted
const { mockGet } = vi.hoisted(() => ({
  mockGet: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

import VersionList from '@/components/skill/VersionList.vue'

describe('VersionList', () => {
  function mountComponent(props = {}) {
    return mount(VersionList, {
      props: {
        skillId: 1,
        ...props,
      },
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
    mockGet.mockResolvedValue({ data: { records: [], total: 0 } })
  })

  it('renders "版本历史" heading', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('版本历史')
  })

  it('renders table with correct columns (版本号, 更新说明, 发布时间)', () => {
    const wrapper = mountComponent()

    const table = wrapper.findComponent({ name: 'ElTable' })
    expect(table.exists()).toBe(true)

    const columns = wrapper.findAllComponents({ name: 'ElTableColumn' })
    const labels = columns.map(c => c.props('label'))

    expect(labels).toContain('版本号')
    expect(labels).toContain('更新说明')
    expect(labels).toContain('发布时间')
  })

  it('shows empty text "暂无版本" when no data', () => {
    const wrapper = mountComponent()

    const table = wrapper.findComponent({ name: 'ElTable' })
    expect(table.props('emptyText')).toBe('暂无版本')
  })

  it('shows pagination when total > 0', async () => {
    mockGet.mockResolvedValue({
      data: {
        records: [
          { version: '1.0.0', changelog: 'Initial release', createTime: '2024-01-01' },
        ],
        total: 25,
      }
    })
    const wrapper = mountComponent({ skillId: 1 })

    // Flush pending promises to let the watch(immediate) -> load() complete
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    expect(wrapper.findComponent({ name: 'ElPagination' }).exists()).toBe(true)
  })

  it('shows loading state', () => {
    // Loading should be true while request is pending
    // Use a never-resolving promise so loading stays true
    mockGet.mockReturnValue(new Promise(() => {}))

    const wrapper = mountComponent()

    // v-loading directive adds the el-loading-mask div
    const loadingMask = wrapper.find('.el-loading-mask')
    expect(loadingMask.exists()).toBe(true)
  })

  it('renders version data in table rows', async () => {
    mockGet.mockResolvedValue({
      data: {
        records: [
          { version: '2.0.0', changelog: 'Big update', createTime: '2024-06-01' },
          { version: '1.0.0', changelog: 'First release', createTime: '2024-01-01' },
        ],
        total: 2,
      }
    })
    const wrapper = mountComponent({ skillId: 1 })

    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    const text = wrapper.text()
    expect(text).toContain('2.0.0')
    expect(text).toContain('Big update')
    expect(text).toContain('1.0.0')
    expect(text).toContain('First release')
  })

  it('watches skillId changes and reloads', async () => {
    mockGet.mockResolvedValue({ data: { records: [], total: 0 } })
    const wrapper = mountComponent({ skillId: 1 })

    // First call on mount (immediate watch)
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    expect(mockGet).toHaveBeenCalledWith('/skills/1/versions', { params: { page: 1, size: 20 } })

    // Change skillId
    vi.clearAllMocks()
    mockGet.mockResolvedValue({ data: { records: [], total: 0 } })
    await wrapper.setProps({ skillId: 2 })
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    expect(mockGet).toHaveBeenCalledWith('/skills/2/versions', { params: { page: 1, size: 20 } })
  })
})
