import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
import SkillCard from '@/components/skill/SkillCard.vue'
import ElementPlus from 'element-plus'

// Mock router
const mockPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush })
}))

describe('SkillCard', () => {
  const skill = {
    id: 1,
    name: 'Test Skill',
    description: 'A test skill',
    categoryName: '自动化脚本',
    authorName: 'author1',
    avgRating: 4.5,
    installCount: 10,
    latestVersion: '1.0.0'
  }

  function mountCard(props = {}) {
    return mount(SkillCard, {
      props: { skill: { ...skill, ...props } },
      global: {
        components: { ElementPlus },
        stubs: { 'el-icon': true }
      }
    })
  }

  it('renders skill name', () => {
    const wrapper = mountCard()
    expect(wrapper.text()).toContain('Test Skill')
  })

  it('renders author name', () => {
    const wrapper = mountCard()
    expect(wrapper.text()).toContain('author1')
  })

  it('renders rating', () => {
    const wrapper = mountCard()
    expect(wrapper.text()).toContain('4.5')
  })

  it('renders install count', () => {
    const wrapper = mountCard()
    expect(wrapper.text()).toContain('10')
  })

  it('renders version', () => {
    const wrapper = mountCard()
    expect(wrapper.text()).toContain('1.0.0')
  })

  it('renders category tag', () => {
    const wrapper = mountCard()
    expect(wrapper.text()).toContain('自动化脚本')
  })

  it('truncates long descriptions', () => {
    const longDesc = 'a'.repeat(150)
    const wrapper = mountCard({ description: longDesc })
    expect(wrapper.text()).toContain('...')
  })
})
