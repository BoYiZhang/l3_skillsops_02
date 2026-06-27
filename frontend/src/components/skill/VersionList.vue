<template>
  <div>
    <h4>版本历史</h4>
    <el-table :data="versions" stripe v-loading="loading" empty-text="暂无版本">
      <el-table-column prop="version" label="版本号" width="120" />
      <el-table-column prop="changelog" label="更新说明" />
      <el-table-column prop="createTime" label="发布时间" width="180" />
    </el-table>
    <el-pagination v-if="total > 0" :total="total" :page-size="20" :current-page="page"
      layout="prev, pager, next" @current-change="onPage" small style="margin-top:10px" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import request from '@/utils/request'

const props = defineProps({ skillId: Number })
const versions = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await request.get(`/skills/${props.skillId}/versions`, { params: { page: page.value, size: 20 } })
    versions.value = res.data.records || []; total.value = res.data.total
  } finally { loading.value = false }
}

watch(() => props.skillId, load, { immediate: true })
function onPage(p) { page.value = p; load() }
</script>
