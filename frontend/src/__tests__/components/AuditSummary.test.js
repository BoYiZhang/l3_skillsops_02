import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import AuditSummary from '@/components/workspace/AuditSummary.vue'
import ElementPlus from 'element-plus'

describe('AuditSummary', () => {
  function mountComponent(props = {}) {
    return mount(AuditSummary, {
      props: { summary: {}, ...props },
      global: {
        plugins: [ElementPlus],
      },
    })
  }

  it('renders "审核概况" title', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('审核概况')
  })

  it('renders pending count from summary prop', () => {
    const wrapper = mountComponent({ summary: { pending: 5 } })
    // The value and label are separate elements; check the value appears
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('待审核')
  })

  it('renders published count from summary prop', () => {
    const wrapper = mountComponent({ summary: { published: 12 } })
    expect(wrapper.findAll('.audit-value').length).toBe(4)
    const values = wrapper.findAll('.audit-value')
    expect(values[1].text()).toBe('12')
  })

  it('renders delisted count from summary prop', () => {
    const wrapper = mountComponent({ summary: { delisted: 3 } })
    const values = wrapper.findAll('.audit-value')
    expect(values[2].text()).toBe('3')
  })

  it('renders draft count from summary prop', () => {
    const wrapper = mountComponent({ summary: { draft: 7 } })
    const values = wrapper.findAll('.audit-value')
    expect(values[3].text()).toBe('7')
  })

  it('renders all four labels (待审核, 已发布, 已下架, 草稿)', () => {
    const wrapper = mountComponent({ summary: { pending: 1, published: 2, delisted: 3, draft: 4 } })
    expect(wrapper.text()).toContain('待审核')
    expect(wrapper.text()).toContain('已发布')
    expect(wrapper.text()).toContain('已下架')
    expect(wrapper.text()).toContain('草稿')
  })

  it('defaults to 0 when summary prop is empty object', () => {
    const wrapper = mountComponent({ summary: {} })
    const values = wrapper.findAll('.audit-value')
    expect(values.length).toBe(4)
    values.forEach((el) => {
      expect(el.text()).toBe('0')
    })
  })
})
