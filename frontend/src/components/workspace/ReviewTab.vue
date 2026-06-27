<template>
  <el-table :data="list" stripe v-loading="loading" empty-text="暂无待审核Skill">
    <el-table-column prop="name" label="名称" />
    <el-table-column prop="authorName" label="作者" />
    <el-table-column prop="categoryName" label="分类" />
    <el-table-column prop="createTime" label="申请时间" width="180" />
    <el-table-column label="操作" width="200">
      <template #default="{ row }">
        <el-button size="small" type="success" @click="doApprove(row.id)">通过</el-button>
        <el-button size="small" type="danger" @click="doReject(row.id)">拒绝</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useWorkspaceStore } from '@/stores/workspace'

const store = useWorkspaceStore()
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    await store.fetchPendingSkills(); list.value = store.pendingSkills
  } finally { loading.value = false }
}

async function doApprove(id) {
  await store.approveSkill(id); ElMessage.success('已通过'); load()
}
async function doReject(id) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝理由', '拒绝上架')
    await store.rejectSkill(id, value); ElMessage.success('已拒绝'); load()
  } catch(e){}
}

onMounted(load)
</script>
