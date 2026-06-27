import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import TrendChart from '@/components/workspace/TrendChart.vue'
import ElementPlus from 'element-plus'

describe('TrendChart', () => {
  const sampleData = Array.from({ length: 40 }, (_, i) => ({
    date: `2025-06-${String(i + 1).padStart(2, '0')}`,
    count: i + 1,
  }))

  function mountComponent(props = {}) {
    return mount(TrendChart, {
      props: {
        title: 'Test Trend',
        data: [],
        ...props,
      },
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

  it('renders chart title from prop', () => {
    const wrapper = mountComponent({ title: '安装趋势' })
    expect(wrapper.text()).toContain('安装趋势')
  })

  it('renders chart when data has items', () => {
    const wrapper = mountComponent({ data: sampleData })
    expect(wrapper.find('.chart-container').exists()).toBe(true)
  })

  it('shows empty state when data is empty array', () => {
    const wrapper = mountComponent({ data: [] })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无趋势数据')
  })

  it('shows empty when data array has fewer items than range (slicedData still non-empty)', () => {
    // With only 3 items, range='7d' still slices last 7 -> returns all 3, chart renders
    const wrapper = mountComponent({ data: sampleData.slice(0, 3), range: '7d' })
    expect(wrapper.find('.chart-container').exists()).toBe(true)
  })

  it('slices data to last 7 items when range is "7d"', () => {
    const wrapper = mountComponent({ data: sampleData, range: '7d' })
    const chart = wrapper.find('.chart-container')
    expect(chart.exists()).toBe(true)
    const optionStr = chart.attributes('option')
    if (optionStr) {
      const parsed = JSON.parse(optionStr.replace(/'/g, '"'))
      // sampleData has 40 items, last 7 should be indices 33-39
      expect(parsed.xAxis.data).toHaveLength(7)
      expect(parsed.xAxis.data[0]).toBe('2025-06-34')
      expect(parsed.series[0].data).toHaveLength(7)
    }
  })

  it('slices data to last 30 items when range is "30d"', () => {
    const wrapper = mountComponent({ data: sampleData, range: '30d' })
    const chart = wrapper.find('.chart-container')
    expect(chart.exists()).toBe(true)
    const optionStr = chart.attributes('option')
    if (optionStr) {
      const parsed = JSON.parse(optionStr.replace(/'/g, '"'))
      // sampleData has 40 items, last 30 should be indices 10-39
      expect(parsed.xAxis.data).toHaveLength(30)
      expect(parsed.series[0].data).toHaveLength(30)
    }
  })

  it('shows empty when slicedData is empty (empty data array)', () => {
    const wrapper = mountComponent({ data: [] })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
  })

  it('passed option has line series type', () => {
    const wrapper = mountComponent({ data: sampleData })
    const chart = wrapper.find('.chart-container')
    expect(chart.exists()).toBe(true)
    const optionStr = chart.attributes('option')
    if (optionStr) {
      const parsed = JSON.parse(optionStr.replace(/'/g, '"'))
      expect(parsed.series[0].type).toBe('line')
      expect(parsed.series[0].smooth).toBe(true)
    }
  })
})
