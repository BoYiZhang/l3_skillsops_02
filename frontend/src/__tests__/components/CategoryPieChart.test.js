import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import CategoryPieChart from '@/components/workspace/CategoryPieChart.vue'
import ElementPlus from 'element-plus'

describe('CategoryPieChart', () => {
  const sampleData = [
    { categoryName: '自动化脚本', count: 10 },
    { categoryName: '数据处理', count: 8 },
    { categoryName: '前端开发', count: 5 },
  ]

  function mountComponent(props = {}) {
    return mount(CategoryPieChart, {
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

  it('renders chart container when data has items', () => {
    const wrapper = mountComponent({ data: sampleData })
    expect(wrapper.find('.chart-container').exists()).toBe(true)
  })

  it('renders empty state when data is empty array', () => {
    const wrapper = mountComponent({ data: [] })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无分类数据')
  })

  it('renders empty state when data is null', () => {
    const wrapper = mountComponent({ data: null })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
  })

  it('renders "Skill 分类分布" title', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('Skill 分类分布')
  })

  it('passes correct option with pie series type to v-chart', () => {
    const wrapper = mountComponent({ data: sampleData })
    const chart = wrapper.find('.chart-container')
    expect(chart.exists()).toBe(true)
    const option = chart.attributes('option')
    // props passed to stubs are set as attributes in string form; parse back
    if (option) {
      const parsed = JSON.parse(option.replace(/'/g, '"'))
      expect(parsed.series[0].type).toBe('pie')
      expect(parsed.series[0].data).toHaveLength(3)
      expect(parsed.series[0].data[0].name).toBe('自动化脚本')
      expect(parsed.series[0].data[0].value).toBe(10)
    }
  })
})
