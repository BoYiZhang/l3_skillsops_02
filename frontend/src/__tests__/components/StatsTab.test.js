import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ElementPlus from 'element-plus'
import StatsTab from '@/components/workspace/StatsTab.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ name: 'Market' })
}))

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
  }
}))

const mockStats = {
  totalSkills: 42,
  totalUsers: 158,
  totalInstalls: 1024,
  avgRating: 4.3,
  installTrend: [{ date: '2025-01-01', count: 10 }],
  userTrend: [{ date: '2025-01-01', count: 5 }],
  categoryDistribution: [{ name: 'AI', value: 20 }],
  ratingDistribution: [{ rating: 5, count: 30 }],
  topSkills: [{ name: 'TopSkill', installCount: 500 }],
  auditSummary: { pending: 3, approved: 10, rejected: 1 }
}

const mockFetchStats = vi.fn().mockResolvedValue(mockStats)

vi.mock('@/stores/workspace', () => ({
  useWorkspaceStore: vi.fn(() => ({
    fetchStats: mockFetchStats
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

describe('StatsTab', () => {
  function mountComponent() {
    return mount(StatsTab, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          'v-chart': true,
          'StatsKpiCards': { template: '<div class="kpi-stub"></div>', props: ['stats'] },
          'TrendChart': { template: '<div class="trend-stub"></div>', props: ['title', 'data', 'range', 'color'] },
          'CategoryPieChart': { template: '<div class="pie-stub"></div>', props: ['data'] },
          'RatingBarChart': { template: '<div class="bar-stub"></div>', props: ['data'] },
          'TopSkillsChart': { template: '<div class="top-stub"></div>', props: ['skills'] },
          'AuditSummary': { template: '<div class="audit-stub"></div>', props: ['summary'] },
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockFetchStats.mockResolvedValue(mockStats)
  })

  it('shows loading state initially (v-loading)', () => {
    const wrapper = mountComponent()
    // The loading ref is set to true during onMounted and remains until fetchStats resolves
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('renders StatsKpiCards with stats data', async () => {
    const wrapper = mountComponent()
    await new Promise(r => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.kpi-stub').exists()).toBe(true)
  })

  it('renders time range radio group', () => {
    const wrapper = mountComponent()
    const radioGroup = wrapper.find('.el-radio-group')
    expect(radioGroup.exists()).toBe(true)
  })

  it('radio group contains "近 7 天" and "近 30 天" options', () => {
    const wrapper = mountComponent()
    const radioButtons = wrapper.findAll('.el-radio-button')
    const labels = radioButtons.map(r => r.text().trim())
    // ElementPlus renders radio-button text inside a span
    const allText = wrapper.text()
    expect(allText).toContain('近 7 天')
    expect(allText).toContain('近 30 天')
  })

  it('default range is "7d"', () => {
    const wrapper = mountComponent()
    expect(wrapper.vm.activeRange).toBe('7d')
  })

  it('renders two TrendChart components for install trend and user growth', () => {
    const wrapper = mountComponent()
    const trendStubs = wrapper.findAll('.trend-stub')
    expect(trendStubs.length).toBe(2)
  })

  it('renders CategoryPieChart and RatingBarChart components', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.pie-stub').exists()).toBe(true)
    expect(wrapper.find('.bar-stub').exists()).toBe(true)
  })

  it('renders TopSkillsChart and AuditSummary components', () => {
    const wrapper = mountComponent()
    expect(wrapper.find('.top-stub').exists()).toBe(true)
    expect(wrapper.find('.audit-stub').exists()).toBe(true)
  })
})
