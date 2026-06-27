<template>
  <el-row :gutter="20" class="kpi-cards">
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--blue">
        <div class="kpi-icon kpi-icon--blue"><el-icon size="28"><Document /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.totalSkills ?? 0 }}</div>
          <div class="kpi-label">Skill 总数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--green">
        <div class="kpi-icon kpi-icon--green"><el-icon size="28"><User /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.totalUsers ?? 0 }}</div>
          <div class="kpi-label">用户总数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--orange">
        <div class="kpi-icon kpi-icon--orange"><el-icon size="28"><Download /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.totalInstalls ?? 0 }}</div>
          <div class="kpi-label">安装总数</div>
        </div>
      </el-card>
    </el-col>
    <el-col :xs="12" :sm="12" :md="6">
      <el-card shadow="hover" class="kpi-card kpi-card--purple">
        <div class="kpi-icon kpi-icon--purple"><el-icon size="28"><Star /></el-icon></div>
        <div class="kpi-body">
          <div class="kpi-value">{{ stats?.avgRating?.toFixed(1) ?? '0.0' }}</div>
          <div class="kpi-label">平均评分</div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { Document, User, Download, Star } from '@element-plus/icons-vue'
defineProps({ stats: { type: Object, default: () => ({}) } })
</script>

<style scoped>
.kpi-cards { margin-bottom: 20px; }
.kpi-card :deep(.el-card__body) {
  display: flex; align-items: center; gap: 16px; padding: 20px;
}
.kpi-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.kpi-icon--blue   { background: #e6f4ff; color: #1677ff; }
.kpi-icon--green  { background: #f6ffed; color: #52c41a; }
.kpi-icon--orange { background: #fff7e6; color: #fa8c16; }
.kpi-icon--purple { background: #f9f0ff; color: #722ed1; }
.kpi-value { font-size: 28px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.kpi-label { font-size: 13px; color: #999; margin-top: 2px; }
</style>
