import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'

// Use vi.hoisted so mock variables are available when vi.mock factory is hoisted
const { mockGet, mockPost, elMessageMock } = vi.hoisted(() => ({
  mockGet: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
  mockPost: vi.fn().mockResolvedValue({ data: {} }),
  elMessageMock: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: elMessageMock,
  }
})

import RatingList from '@/components/skill/RatingList.vue'

describe('RatingList', () => {
  function mountComponent(props = {}) {
    return mount(RatingList, {
      props: {
        skillId: 1,
        installed: false,
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
    mockPost.mockResolvedValue({ data: {} })
  })

  it('renders "评价列表" heading', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('评价列表')
  })

  it('does NOT show rating form when installed prop is false', () => {
    const wrapper = mountComponent({ installed: false })
    expect(wrapper.find('.rate-form').exists()).toBe(false)
  })

  it('shows rating form when installed prop is true (el-rate, textarea, submit button)', () => {
    const wrapper = mountComponent({ installed: true })
    expect(wrapper.find('.rate-form').exists()).toBe(true)

    // Should contain el-rate component
    expect(wrapper.findComponent({ name: 'ElRate' }).exists()).toBe(true)
    // Should contain textarea
    const textarea = wrapper.find('textarea')
    expect(textarea.exists()).toBe(true)
    // Should contain submit button
    const button = wrapper.find('.rate-form').findComponent({ name: 'ElButton' })
    expect(button.exists()).toBe(true)
    expect(button.text()).toContain('提交评价')
  })

  it('renders rating items when data loaded', async () => {
    mockGet.mockResolvedValue({
      data: {
        records: [
          { id: 1, username: 'user1', rating: 5, comment: '很好用', createTime: '2024-01-01' },
          { id: 2, username: 'user2', rating: 4, comment: '不错', createTime: '2024-01-02' },
        ],
        total: 2,
      }
    })
    const wrapper = mountComponent({ skillId: 1 })

    // Flush pending promises to let the watch(immediate) -> load() complete
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    const items = wrapper.findAll('.rating-item')
    expect(items).toHaveLength(2)
    expect(items[0].text()).toContain('user1')
    expect(items[0].text()).toContain('很好用')
    expect(items[1].text()).toContain('user2')
    expect(items[1].text()).toContain('不错')
  })

  it('shows pagination when total > 0', async () => {
    mockGet.mockResolvedValue({
      data: {
        records: [{ id: 1, username: 'user1', rating: 5, comment: 'test', createTime: '2024-01-01' }],
        total: 25,
      }
    })
    const wrapper = mountComponent({ skillId: 1 })

    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    expect(wrapper.findComponent({ name: 'ElPagination' }).exists()).toBe(true)
  })

  it('shows "（无文字评价）" for ratings without comment', async () => {
    mockGet.mockResolvedValue({
      data: {
        records: [
          { id: 1, username: 'user1', rating: 5, comment: '', createTime: '2024-01-01' },
          { id: 2, username: 'user2', rating: 4, comment: null, createTime: '2024-01-02' },
        ],
        total: 2,
      }
    })
    const wrapper = mountComponent({ skillId: 1 })

    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    const comments = wrapper.findAll('.rating-comment')
    expect(comments).toHaveLength(2)
    expect(comments[0].text()).toContain('（无文字评价）')
    expect(comments[1].text()).toContain('（无文字评价）')
  })

  it('submits rating and shows success message', async () => {
    mockGet.mockResolvedValue({ data: { records: [], total: 0 } })
    mockPost.mockResolvedValue({ data: {} })

    const wrapper = mountComponent({ skillId: 1, installed: true })
    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    // Set rating and comment via component's reactive state
    wrapper.vm.myRating.rating = 5
    wrapper.vm.myRating.comment = '测试评价'
    await wrapper.vm.$nextTick()

    // Click submit
    const button = wrapper.find('.rate-form').findComponent({ name: 'ElButton' })
    await button.trigger('click')

    await new Promise(resolve => setTimeout(resolve, 10))
    await wrapper.vm.$nextTick()

    expect(mockPost).toHaveBeenCalledWith('/skills/1/ratings', {
      rating: 5,
      comment: '测试评价',
    })
    expect(elMessageMock.success).toHaveBeenCalledWith('评价提交成功')
  })
})
