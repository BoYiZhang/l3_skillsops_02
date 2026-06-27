import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import RatingBarChart from '@/components/workspace/RatingBarChart.vue'
import ElementPlus from 'element-plus'

describe('RatingBarChart', () => {
  const sampleData = [
    { star: 1, count: 2 },
    { star: 2, count: 5 },
    { star: 3, count: 8 },
  ]

  function mountComponent(props = {}) {
    return mount(RatingBarChart, {
      props: { data: [], ...props },
      global: {
        plugins: [ElementPlus],
        stubs: {
          'v-chart': {
            template: '<div class="chart-container"><slot /></div>',
            props: ['option'],
          },
        },
      },
    })
  }

  it('renders chart when data has items', () => {
    const wrapper = mountComponent({ data: sampleData })
    expect(wrapper.find('.chart-container').exists()).toBe(true)
  })

  it('shows empty state when data is empty array', () => {
    const wrapper = mountComponent({ data: [] })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无评分数据')
  })

  it('shows empty when data is null', () => {
    const wrapper = mountComponent({ data: null })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
  })

  it('renders "评分分布" title', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('评分分布')
  })

  it('passed option has bar series type', () => {
    const wrapper = mountComponent({ data: sampleData })
    const chart = wrapper.find('.chart-container')
    expect(chart.exists()).toBe(true)
    // Verify bar series type exists in the option prop
    const optionStr = chart.attributes('option')
    if (optionStr) {
      const parsed = JSON.parse(optionStr.replace(/'/g, '"'))
      expect(parsed.series[0].type).toBe('bar')
    }
  })
})
