<template>
  <div>
    <el-button type="primary" @click="showCreate = true" style="margin-bottom:16px">创建 Skill</el-button>

    <el-table :data="skills" stripe v-loading="loading" empty-text="暂无发布的Skill">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="categoryName" label="分类" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="latestVersion" label="版本" width="100" />
      <el-table-column prop="installCount" label="安装量" width="80" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="editSkill(row)">编辑</el-button>
          <el-button v-if="row.status==='DRAFT'" size="small" type="warning" @click="doSubmit(row.id)">提交审核</el-button>
          <el-button v-if="row.status==='PUBLISHED'" size="small" type="success" @click="showVersion(row)">发新版</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog :title="editing ? '编辑Skill' : '创建Skill'" v-model="showCreate" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库地址"><el-input v-model="form.repoUrl" /></el-form-item>
        <el-form-item label="文档链接"><el-input v-model="form.docUrl" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="saveSkill" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- Version Dialog -->
    <el-dialog title="发布新版本" v-model="showVer" width="400px">
      <el-form :model="verForm" label-width="80px">
        <el-form-item label="版本号"><el-input v-model="verForm.version" placeholder="如 1.0.0" /></el-form-item>
        <el-form-item label="更新说明"><el-input v-model="verForm.changelog" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showVer = false">取消</el-button>
        <el-button type="primary" @click="publishVer" :loading="pubLoading">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useWorkspaceStore } from '@/stores/workspace'
import request from '@/utils/request'

const store = useWorkspaceStore()
const skills = ref([])
const loading = ref(false)
const showCreate = ref(false)
const showVer = ref(false)
const editing = ref(null)
const saving = ref(false)
const pubLoading = ref(false)
const categories = ref([])
const curSkillId = ref(null)

const form = reactive({ name: '', description: '', categoryId: null, repoUrl: '', docUrl: '' })
const verForm = reactive({ version: '', changelog: '' })

const statusType = s => ({ DRAFT:'info', PENDING_APPROVAL:'warning', PUBLISHED:'success', DELISTED:'danger' }[s]||'info')
const statusLabel = s => ({ DRAFT:'草稿', PENDING_APPROVAL:'审核中', PUBLISHED:'已上架', DELISTED:'已下架' }[s]||s)

async function load() {
  loading.value = true
  try {
    const res = await store.fetchMySkills()
    skills.value = store.mySkills
  } finally { loading.value = false }
}

async function loadCategories() {
  try { const res = await request.get('/admin/categories'); categories.value = res.data || [] } catch(e){}
}

function editSkill(row) {
  editing.value = row
  form.name = row.name; form.description = row.description
  form.categoryId = row.categoryId; form.repoUrl = row.repoUrl; form.docUrl = row.docUrl
  showCreate.value = true
}

async function saveSkill() {
  saving.value = true
  try {
    if (editing.value) {
      await request.put(`/skills/${editing.value.id}`, { ...form })
    } else {
      await store.createSkill({ ...form })
    }
    ElMessage.success(editing.value ? '已更新' : '已创建')
    showCreate.value = false; editing.value = null
    form.name = ''; form.description = ''; form.categoryId = null; form.repoUrl = ''; form.docUrl = ''
    load()
  } finally { saving.value = false }
}

async function doSubmit(id) {
  await store.submitSkill(id); ElMessage.success('已提交审核'); load()
}

function showVersion(row) { curSkillId.value = row.id; showVer.value = true }

async function publishVer() {
  pubLoading.value = true
  try {
    await store.publishVersion(curSkillId.value, { ...verForm })
    ElMessage.success('版本发布成功')
    showVer.value = false; verForm.version = ''; verForm.changelog = ''; load()
  } finally { pubLoading.value = false }
}

onMounted(() => { load(); loadCategories() })
</script>
