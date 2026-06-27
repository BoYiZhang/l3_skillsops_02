import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import StatsKpiCards from '@/components/workspace/StatsKpiCards.vue'
import ElementPlus from 'element-plus'

vi.mock('@element-plus/icons-vue', () => ({
  Document: { name: 'Document', template: '<span>icon</span>' },
  User: { name: 'User', template: '<span>icon</span>' },
  Download: { name: 'Download', template: '<span>icon</span>' },
  Star: { name: 'Star', template: '<span>icon</span>' },
}))

describe('StatsKpiCards', () => {
  function mountComponent(props = {}) {
    return mount(StatsKpiCards, {
      props: { stats: {}, ...props },
      global: {
        plugins: [ElementPlus],
        stubs: { 'el-icon': true },
      },
    })
  }

  it('renders totalSkills count', () => {
    const wrapper = mountComponent({ stats: { totalSkills: 42 } })
    const values = wrapper.findAll('.kpi-value')
    expect(values[0].text()).toBe('42')
  })

  it('renders totalUsers count', () => {
    const wrapper = mountComponent({ stats: { totalUsers: 150 } })
    const values = wrapper.findAll('.kpi-value')
    expect(values[1].text()).toBe('150')
  })

  it('renders totalInstalls count', () => {
    const wrapper = mountComponent({ stats: { totalInstalls: 888 } })
    const values = wrapper.findAll('.kpi-value')
    expect(values[2].text()).toBe('888')
  })

  it('renders avgRating with 1 decimal place', () => {
    const wrapper = mountComponent({ stats: { avgRating: 4.567 } })
    const values = wrapper.findAll('.kpi-value')
    expect(values[3].text()).toBe('4.6')
  })

  it('shows "0.0" when avgRating is missing', () => {
    const wrapper = mountComponent({ stats: {} })
    const values = wrapper.findAll('.kpi-value')
    expect(values[3].text()).toBe('0.0')
  })

  it('renders all KPI labels (Skill 总数, 用户总数, 安装总数, 平均评分)', () => {
    const wrapper = mountComponent({ stats: { totalSkills: 1, totalUsers: 1, totalInstalls: 1, avgRating: 1 } })
    expect(wrapper.text()).toContain('Skill 总数')
    expect(wrapper.text()).toContain('用户总数')
    expect(wrapper.text()).toContain('安装总数')
    expect(wrapper.text()).toContain('平均评分')
  })

  it('defaults to 0 when stats prop is empty', () => {
    const wrapper = mountComponent({ stats: {} })
    const values = wrapper.findAll('.kpi-value')
    expect(values.length).toBe(4)
    expect(values[0].text()).toBe('0')
    expect(values[1].text()).toBe('0')
    expect(values[2].text()).toBe('0')
    expect(values[3].text()).toBe('0.0')
  })
})
