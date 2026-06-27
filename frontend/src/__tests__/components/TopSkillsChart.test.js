import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import TopSkillsChart from '@/components/workspace/TopSkillsChart.vue'
import ElementPlus from 'element-plus'

describe('TopSkillsChart', () => {
  const sampleSkills = [
    { name: 'Skill A', installCount: 100 },
    { name: 'Skill B', installCount: 80 },
    { name: 'Skill C', installCount: 60 },
  ]

  function mountComponent(props = {}) {
    return mount(TopSkillsChart, {
      props: { skills: [], ...props },
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

  it('renders chart when skills array has items', () => {
    const wrapper = mountComponent({ skills: sampleSkills })
    expect(wrapper.find('.chart-container').exists()).toBe(true)
  })

  it('shows empty state when skills is empty array', () => {
    const wrapper = mountComponent({ skills: [] })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无数据')
  })

  it('shows empty when skills is null', () => {
    const wrapper = mountComponent({ skills: null })
    expect(wrapper.find('.chart-container').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElEmpty' }).exists()).toBe(true)
  })

  it('renders "热门 Skill Top 10" title', () => {
    const wrapper = mountComponent()
    expect(wrapper.text()).toContain('热门 Skill Top 10')
  })

  it('passed option has bar series type with reversed data', () => {
    const wrapper = mountComponent({ skills: sampleSkills })
    const chart = wrapper.find('.chart-container')
    expect(chart.exists()).toBe(true)
    const optionStr = chart.attributes('option')
    if (optionStr) {
      const parsed = JSON.parse(optionStr.replace(/'/g, '"'))
      expect(parsed.series[0].type).toBe('bar')
      // Data is reversed: top 1 at bottom of horizontal bar
      // yAxis names should be reversed with rank numbers
      expect(parsed.yAxis.data).toHaveLength(3)
      // Last element (highest rank #1) should appear at end after reversing:
      // original: [A:100, B:80, C:60] -> reversed: [C:60, B:80, A:100]
      // names: ["3. Skill C", "2. Skill B", "1. Skill A"]
      expect(parsed.yAxis.data[2]).toBe('1. Skill A')
      expect(parsed.yAxis.data[1]).toBe('2. Skill B')
      expect(parsed.yAxis.data[0]).toBe('3. Skill C')
    }
  })
})
