<template>
  <div>
    <el-table :data="users" stripe v-loading="loading" empty-text="暂无用户">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" :type="r==='ADMIN'?'danger':''" style="margin-right:4px">{{ r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status==='ACTIVE'?'success':'info'" size="small">{{ row.status==='ACTIVE'?'正常':'禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="row.username!=='admin'" size="small" :type="row.status==='ACTIVE'?'warning':'success'"
            @click="toggleStatus(row)">{{ row.status==='ACTIVE'?'禁用':'启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total>0" :total="total" :page-size="20" :current-page="page"
      layout="prev,pager,next" @current-change="onPage" style="margin-top:16px;justify-content:center" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const users = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)

async function load() {
  loading.value = true
  try {
    const res = await request.get('/admin/users', { params: { page: page.value, size: 20 } })
    users.value = res.data.records; total.value = res.data.total
  } finally { loading.value = false }
}

async function toggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await request.put(`/admin/users/${row.id}/status`, { status: newStatus })
  ElMessage.success(newStatus === 'ACTIVE' ? '已启用' : '已禁用')
  load()
}

function onPage(p) { page.value = p; load() }
onMounted(load)
</script>
