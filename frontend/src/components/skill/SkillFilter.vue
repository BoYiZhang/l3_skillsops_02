<template>
  <div class="filter-bar">
    <el-select v-model="localFilter.categoryId" placeholder="全部分类" clearable @change="search" style="width:160px">
      <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
    </el-select>
    <el-input v-model="localFilter.keyword" placeholder="搜索Skill..." clearable @keyup.enter="search" @clear="search" style="width:260px" />
    <el-select v-model="localFilter.sortBy" @change="search" style="width:130px">
      <el-option label="最新发布" value="NEWEST" />
      <el-option label="最多安装" value="HOT" />
      <el-option label="最高评分" value="RATING" />
    </el-select>
    <el-button :icon="Refresh" circle @click="$emit('refresh')" title="刷新列表" />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const emit = defineEmits(['filter', 'refresh'])
const localFilter = reactive({ categoryId: null, keyword: '', sortBy: 'NEWEST' })
const categories = ref([])

onMounted(async () => {
  try { const res = await request.get('/admin/categories'); categories.value = res.data || [] } catch(e){}
})

function search() { emit('filter', { ...localFilter }) }
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; align-items: center; }
</style>
