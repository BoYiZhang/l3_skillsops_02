<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">📋 审核概况</span>
    </template>
    <div class="audit-grid">
      <div class="audit-item">
        <div class="audit-dot audit-dot--warning"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.pending ?? 0 }}</div>
          <div class="audit-label">待审核</div>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-dot audit-dot--success"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.published ?? 0 }}</div>
          <div class="audit-label">已发布</div>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-dot audit-dot--danger"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.delisted ?? 0 }}</div>
          <div class="audit-label">已下架</div>
        </div>
      </div>
      <div class="audit-item">
        <div class="audit-dot audit-dot--default"></div>
        <div class="audit-body">
          <div class="audit-value">{{ summary?.draft ?? 0 }}</div>
          <div class="audit-label">草稿</div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({ summary: { type: Object, default: () => ({}) } })
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
.audit-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.audit-item { display: flex; align-items: center; gap: 12px; padding: 16px; background: #f7f8fa; border-radius: 8px; }
.audit-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.audit-dot--warning { background: #faad14; }
.audit-dot--success { background: #52c41a; }
.audit-dot--danger  { background: #ff4d4f; }
.audit-dot--default { background: #999; }
.audit-value { font-size: 24px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.audit-label { font-size: 13px; color: #999; }

@media (max-width: 768px) {
  .audit-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
