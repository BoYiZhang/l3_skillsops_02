import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useWorkspaceStore = defineStore('workspace', () => {
  const mySkills = ref([])
  const installedSkills = ref([])
  const pendingSkills = ref([])
  const stats = ref(null)
  const loading = ref(false)

  async function fetchMySkills(params = {}) {
    loading.value = true
    try {
      const res = await request.get('/workspace/my-skills', { params })
      mySkills.value = res.data.records
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function createSkill(form) {
    const res = await request.post('/skills', form)
    return res.data
  }

  async function submitSkill(id) {
    await request.post(`/skills/${id}/submit`)
  }

  async function publishVersion(id, form) {
    await request.post(`/skills/${id}/versions`, form)
  }

  async function fetchInstalled(params = {}) {
    const res = await request.get('/workspace/installed', { params })
    installedSkills.value = res.data.records
    return res.data
  }

  async function fetchPendingSkills(params = {}) {
    const res = await request.get('/admin/pending-skills', { params })
    pendingSkills.value = res.data.records
    return res.data
  }

  async function approveSkill(id) {
    await request.post(`/admin/skills/${id}/approve`)
  }

  async function rejectSkill(id, reason) {
    await request.post(`/admin/skills/${id}/reject`, { reason })
  }

  async function delistSkill(id, reason) {
    await request.post(`/admin/skills/${id}/delist`, { reason })
  }

  async function fetchStats() {
    const res = await request.get('/admin/stats')
    stats.value = res.data
    return res.data
  }

  return { mySkills, installedSkills, pendingSkills, stats, loading, fetchMySkills, createSkill, submitSkill, publishVersion, fetchInstalled, fetchPendingSkills, approveSkill, rejectSkill, delistSkill, fetchStats }
})
