<template>
  <el-table :data="list" stripe v-loading="loading" empty-text="暂无安装记录">
    <el-table-column prop="skillName" label="名称" />
    <el-table-column prop="skillDescription" label="描述" :show-overflow-tooltip="true" />
    <el-table-column prop="version" label="版本" width="100" />
    <el-table-column label="状态" width="100">
      <template #default="{ row }">
        <el-tag :type="row.status==='DELISTED'?'danger':'success'" size="small">
          {{ row.status === 'DELISTED' ? '已下架' : '可用' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="100">
      <template #default="{ row }">
        <el-button size="small" @click="$router.push(`/skill/${row.skillId}`)">查看</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useWorkspaceStore } from '@/stores/workspace'

const store = useWorkspaceStore()
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    await store.fetchInstalled(); list.value = store.installedSkills
  } finally { loading.value = false }
}

onMounted(load)
</script>
