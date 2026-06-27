import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useSkillStore = defineStore('skill', () => {
  const marketSkills = ref([])
  const currentSkill = ref(null)
  const loading = ref(false)
  const total = ref(0)

  async function fetchMarket(params = {}) {
    loading.value = true
    try {
      const res = await request.get('/market/skills', { params })
      marketSkills.value = res.data.records
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchDetail(id) {
    const res = await request.get(`/skills/${id}`)
    currentSkill.value = res.data
    return res.data
  }

  async function install(id) {
    await request.post(`/market/skills/${id}/install`)
  }

  async function checkInstallStatus(id) {
    const res = await request.get(`/market/skills/${id}/install-status`)
    return res.data
  }

  function clearCurrent() {
    currentSkill.value = null
  }

  return { marketSkills, currentSkill, loading, total, fetchMarket, fetchDetail, install, checkInstallStatus, clearCurrent }
})
