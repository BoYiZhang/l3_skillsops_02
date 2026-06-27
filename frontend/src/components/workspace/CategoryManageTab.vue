<template>
  <div>
    <div style="display:flex;gap:8px;margin-bottom:16px">
      <el-input v-model="newCat.name" placeholder="分类名称" style="width:200px" />
      <el-input v-model="newCat.desc" placeholder="描述（选填）" style="width:300px" />
      <el-button type="primary" @click="createCat" :loading="creating">新增</el-button>
    </div>
    <el-table :data="categories" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="editRow=row; editCat.name=row.name; editCat.desc=row.description">编辑</el-button>
          <el-popconfirm title="删除后关联 Skill 将不可用，确定？" @confirm="deleteCat(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <!-- Edit Dialog -->
    <el-dialog v-model="!!editRow" title="编辑分类" width="400px">
      <el-form v-if="editRow">
        <el-form-item label="名称"><el-input v-model="editCat.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="editCat.desc" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editRow=null">取消</el-button>
        <el-button type="primary" @click="saveEdit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const categories = ref([])
const loading = ref(false)
const creating = ref(false)
const saving = ref(false)
const editRow = ref(null)
const newCat = reactive({ name: '', desc: '' })
const editCat = reactive({ name: '', desc: '' })

async function load() {
  loading.value = true
  try { const res = await request.get('/admin/categories'); categories.value = res.data || [] }
  finally { loading.value = false }
}

async function createCat() {
  if (!newCat.name) return ElMessage.warning('请输入名称')
  creating.value = true
  try {
    await request.post('/admin/categories', { name: newCat.name, description: newCat.desc })
    ElMessage.success('已创建'); newCat.name = ''; newCat.desc = ''; load()
  } finally { creating.value = false }
}

async function saveEdit() {
  saving.value = true
  try {
    await request.put(`/admin/categories/${editRow.value.id}`, { name: editCat.name, description: editCat.desc })
    ElMessage.success('已更新'); editRow.value = null; load()
  } finally { saving.value = false }
}

async function deleteCat(id) {
  try { await request.delete(`/admin/categories/${id}`); ElMessage.success('已删除'); load() }
  catch(e) {}
}

onMounted(load)
</script>
