<template>
  <div class="stats-container" v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card skill-card">
          <div class="stat-icon"><el-icon size="32"><Document /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats?.totalSkills || 0 }}</div>
            <div class="stat-label">Skill 总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card user-card">
          <div class="stat-icon"><el-icon size="32"><User /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats?.totalUsers || 0 }}</div>
            <div class="stat-label">用户总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card install-card">
          <div class="stat-icon"><el-icon size="32"><Download /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats?.totalInstalls || 0 }}</div>
            <div class="stat-label">安装总数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门 Skill 排行 -->
    <el-card shadow="never" class="rank-card">
      <template #header><span class="card-title">🏆 热门 Skill Top 5</span></template>
      <div v-if="stats?.topSkills?.length" class="rank-list">
        <div v-for="(s, i) in stats.topSkills" :key="s.id" class="rank-item">
          <div class="rank-num" :class="'rank-' + (i + 1)">{{ i + 1 }}</div>
          <div class="rank-info">
            <div class="rank-name">{{ s.name }}</div>
            <div class="rank-meta">{{ s.authorName }} · ⭐{{ s.avgRating?.toFixed(1) || '0.0' }}</div>
          </div>
          <div class="rank-bar-wrap">
            <div class="rank-bar" :style="{ width: barWidth(s.installCount) }"></div>
          </div>
          <div class="rank-count">{{ s.installCount }} 安装</div>
        </div>
      </div>
      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-card>

    <!-- 安装趋势 -->
    <el-card shadow="never" class="trend-card" v-if="stats?.installTrend?.length">
      <template #header><span class="card-title">📊 安装趋势</span></template>
      <div class="trend-list">
        <div v-for="t in stats.installTrend" :key="t.date" class="trend-item">
          <span class="trend-date">{{ t.date }}</span>
          <span class="trend-count">{{ t.count }} 次安装</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Document, User, Download } from '@element-plus/icons-vue'
import { useWorkspaceStore } from '@/stores/workspace'

const store = useWorkspaceStore()
const stats = ref(null)
const loading = ref(false)

function barWidth(count) {
  if (!stats.value?.topSkills?.length) return '0%'
  const max = Math.max(...stats.value.topSkills.map(s => s.installCount), 1)
  return ((count / max) * 100).toFixed(0) + '%'
}

onMounted(async () => {
  loading.value = true
  try { stats.value = await store.fetchStats() } finally { loading.value = false }
})
</script>

<style scoped>
.stats-container { padding: 4px 0; }
.stat-cards { margin-bottom: 20px; }
.stat-card { display: flex; align-items: center; padding: 8px 16px; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; width: 100%; padding: 20px; }
.stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.skill-card .stat-icon { background: #e6f4ff; color: #1677ff; }
.user-card .stat-icon { background: #f6ffed; color: #52c41a; }
.install-card .stat-icon { background: #fff7e6; color: #fa8c16; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }

.card-title { font-size: 15px; font-weight: 600; color: #333; }
.rank-card, .trend-card { margin-bottom: 20px; }
.rank-list { display: flex; flex-direction: column; gap: 12px; }
.rank-item { display: flex; align-items: center; gap: 12px; }
.rank-num { width: 24px; height: 24px; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; color: #fff; background: #999; }
.rank-1 { background: #f53f3f; }
.rank-2 { background: #ff7d00; }
.rank-3 { background: #f7ba1e; }
.rank-info { flex: 0 0 180px; }
.rank-name { font-weight: 600; font-size: 14px; }
.rank-meta { font-size: 12px; color: #999; }
.rank-bar-wrap { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.rank-bar { height: 100%; background: linear-gradient(90deg, #1677ff, #69b1ff); border-radius: 4px; transition: width 0.6s; }
.rank-count { font-size: 13px; color: #666; min-width: 70px; text-align: right; }

.trend-list { display: flex; gap: 24px; }
.trend-item { display: flex; flex-direction: column; align-items: center; padding: 12px 20px; background: #f7f8fa; border-radius: 8px; }
.trend-date { font-size: 13px; color: #666; }
.trend-count { font-size: 18px; font-weight: 700; color: #1677ff; margin-top: 4px; }
</style>
