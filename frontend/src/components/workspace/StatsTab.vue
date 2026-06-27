<template>
  <div class="stats-dashboard" v-loading="loading">
    <!-- KPI 卡片 -->
    <StatsKpiCards :stats="stats" />

    <!-- 时间范围切换 -->
    <div class="range-bar">
      <el-radio-group v-model="activeRange" size="small">
        <el-radio-button value="7d">近 7 天</el-radio-button>
        <el-radio-button value="30d">近 30 天</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 趋势图行 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <TrendChart
          title="📊 安装趋势"
          :data="stats?.installTrend ?? []"
          :range="activeRange"
          color="#1677ff"
        />
      </el-col>
      <el-col :xs="24" :md="12">
        <TrendChart
          title="👥 用户增长"
          :data="stats?.userTrend ?? []"
          :range="activeRange"
          color="#52c41a"
        />
      </el-col>
    </el-row>

    <!-- 分类 & 评分 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <CategoryPieChart :data="stats?.categoryDistribution ?? []" />
      </el-col>
      <el-col :xs="24" :md="12">
        <RatingBarChart :data="stats?.ratingDistribution ?? []" />
      </el-col>
    </el-row>

    <!-- 热门 Skill & 审核概况 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <TopSkillsChart :skills="stats?.topSkills ?? []" />
      </el-col>
      <el-col :xs="24" :md="8">
        <AuditSummary :summary="stats?.auditSummary" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useWorkspaceStore } from '@/stores/workspace'
import StatsKpiCards from './StatsKpiCards.vue'
import TrendChart from './TrendChart.vue'
import CategoryPieChart from './CategoryPieChart.vue'
import RatingBarChart from './RatingBarChart.vue'
import TopSkillsChart from './TopSkillsChart.vue'
import AuditSummary from './AuditSummary.vue'

const store = useWorkspaceStore()
const stats = ref(null)
const loading = ref(false)
const activeRange = ref('7d')

onMounted(async () => {
  loading.value = true
  try { stats.value = await store.fetchStats() } finally { loading.value = false }
})
</script>

<style scoped>
.stats-dashboard { padding: 4px 0; }
.range-bar { margin-bottom: 16px; text-align: right; }
</style>
