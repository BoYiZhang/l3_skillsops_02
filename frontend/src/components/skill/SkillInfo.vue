<template>
  <div>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="名称">{{ skill.name }}</el-descriptions-item>
      <el-descriptions-item label="分类">{{ skill.categoryName }}</el-descriptions-item>
      <el-descriptions-item label="作者">{{ skill.authorName }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ skill.latestVersion || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="状态"><el-tag :type="statusType">{{ statusLabel }}</el-tag></el-descriptions-item>
      <el-descriptions-item label="评分">⭐ {{ skill.avgRating?.toFixed(1) || '0.0' }}</el-descriptions-item>
      <el-descriptions-item label="安装量">📥 {{ skill.installCount }}</el-descriptions-item>
      <el-descriptions-item label="仓库">{{ skill.repoUrl || '-' }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ skill.description }}</el-descriptions-item>
    </el-descriptions>
    <div class="action-bar">
      <el-button v-if="!installed" type="primary" @click="doInstall" :loading="installing">安装</el-button>
      <el-tag v-else type="success">已安装</el-tag>
      <template v-if="isAdmin">
        <el-button v-if="skill.status === 'PENDING_APPROVAL'" type="success" @click="approve">通过</el-button>
        <el-button v-if="skill.status === 'PENDING_APPROVAL'" type="danger" @click="doReject">拒绝</el-button>
        <el-button v-if="skill.status === 'PUBLISHED'" type="danger" @click="doDelist">下架</el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useSkillStore } from '@/stores/skill'
import request from '@/utils/request'

const props = defineProps({ skill: Object })
const emit = defineEmits(['refresh'])
const auth = useAuthStore()
const store = useSkillStore()
const installing = ref(false)
const installed = ref(false)

const isAdmin = computed(() => auth.isAdmin)
const statusType = computed(() => {
  const map = { DRAFT: 'info', PENDING_APPROVAL: 'warning', PUBLISHED: 'success', DELISTED: 'danger' }
  return map[props.skill.status] || 'info'
})
const statusLabel = computed(() => {
  const map = { DRAFT: '草稿', PENDING_APPROVAL: '审核中', PUBLISHED: '已上架', DELISTED: '已下架' }
  return map[props.skill.status] || props.skill.status
})

async function checkStatus() {
  try {
    const res = await store.checkInstallStatus(props.skill.id)
    installed.value = res.installed
  } catch(e){}
}
checkStatus()

async function doInstall() {
  installing.value = true
  try { await store.install(props.skill.id); ElMessage.success('安装成功'); installed.value = true; emit('refresh') }
  finally { installing.value = false }
}
async function approve() {
  await request.post(`/admin/skills/${props.skill.id}/approve`)
  ElMessage.success('已通过'); emit('refresh')
}
async function doReject() {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝理由', '拒绝上架')
    await request.post(`/admin/skills/${props.skill.id}/reject`, { reason: value })
    ElMessage.success('已拒绝'); emit('refresh')
  } catch(e){}
}
async function doDelist() {
  try {
    const { value } = await ElMessageBox.prompt('请输入下架理由', '下架Skill')
    await request.post(`/admin/skills/${props.skill.id}/delist`, { reason: value })
    ElMessage.success('已下架'); emit('refresh')
  } catch(e){}
}
</script>

<style scoped>
.action-bar { margin-top: 16px; display: flex; gap: 8px; align-items: center; }
</style>
