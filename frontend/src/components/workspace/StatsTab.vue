<template>
  <div v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="8"><el-statistic title="Skill总数" :value="stats?.totalSkills || 0" /></el-col>
      <el-col :span="8"><el-statistic title="用户总数" :value="stats?.totalUsers || 0" /></el-col>
      <el-col :span="8"><el-statistic title="安装总数" :value="stats?.totalInstalls || 0" /></el-col>
    </el-row>
    <h4 style="margin-top:24px">热门 Skill Top 5</h4>
    <el-table :data="stats?.topSkills || []" stripe>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="authorName" label="作者" />
      <el-table-column prop="installCount" label="安装量" />
      <el-table-column prop="avgRating" label="评分" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useWorkspaceStore } from '@/stores/workspace'

const store = useWorkspaceStore()
const stats = ref(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try { stats.value = await store.fetchStats() } finally { loading.value = false }
})
</script>
