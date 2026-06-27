<template>
  <div>
    <h4>评价列表</h4>
    <div v-if="installed" class="rate-form">
      <el-rate v-model="myRating.rating" :max="5" show-score />
      <el-input v-model="myRating.comment" type="textarea" :rows="2" placeholder="写下你的评价..." style="margin:8px 0" />
      <el-button type="primary" size="small" @click="submitRating" :loading="submitting">提交评价</el-button>
    </div>
    <div v-for="r in ratings" :key="r.id" class="rating-item">
      <div class="rating-header">
        <strong>{{ r.username }}</strong>
        <el-rate :model-value="r.rating" disabled show-score size="small" />
        <span class="time">{{ r.createTime }}</span>
      </div>
      <p class="rating-comment">{{ r.comment || '（无文字评价）' }}</p>
    </div>
    <el-pagination v-if="total > 0" :total="total" :page-size="10" :current-page="page"
      layout="prev, pager, next" @current-change="onPage" small style="margin-top:10px" />
  </div>
</template>

<script setup>
import { ref, watch, reactive } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const props = defineProps({ skillId: Number, installed: Boolean })
const ratings = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const submitting = ref(false)
const myRating = reactive({ rating: 0, comment: '' })

async function load() {
  loading.value = true
  try {
    const res = await request.get(`/skills/${props.skillId}/ratings`, { params: { page: page.value, size: 10 } })
    ratings.value = res.data.records || []; total.value = res.data.total
  } finally { loading.value = false }
}

async function submitRating() {
  if (myRating.rating === 0) { ElMessage.warning('请选择评分'); return }
  submitting.value = true
  try {
    await request.post(`/skills/${props.skillId}/ratings`, { rating: myRating.rating, comment: myRating.comment })
    ElMessage.success('评价提交成功')
    load()
  } finally { submitting.value = false }
}

watch(() => props.skillId, load, { immediate: true })
function onPage(p) { page.value = p; load() }
</script>

<style scoped>
.rate-form { background: #f5f7fa; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
.rating-item { border-bottom: 1px solid #eee; padding: 12px 0; }
.rating-header { display: flex; align-items: center; gap: 16px; }
.time { color: #999; font-size: 12px; }
.rating-comment { margin: 6px 0 0 0; color: #666; }
</style>
