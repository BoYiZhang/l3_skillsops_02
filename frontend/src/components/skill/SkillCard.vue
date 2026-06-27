<template>
  <el-card class="skill-card" shadow="hover" @click="$router.push(`/skill/${skill.id}`)">
    <div class="card-header">
      <h4>{{ skill.name }}</h4>
      <div class="card-tags">
        <el-tag v-if="installed" size="small" type="success" effect="dark">已安装</el-tag>
        <el-tag size="small" type="info">{{ skill.categoryName }}</el-tag>
      </div>
    </div>
    <p class="card-desc">{{ skill.description?.substring(0, 120) }}{{ skill.description?.length > 120 ? '...' : '' }}</p>
    <div class="card-meta">
      <span>👤 {{ skill.authorName }}</span>
      <span>⭐ {{ skill.avgRating?.toFixed(1) || '0.0' }}</span>
      <span>📥 {{ skill.installCount }}</span>
      <span v-if="skill.latestVersion">v{{ skill.latestVersion }}</span>
    </div>
  </el-card>
</template>

<script setup>
defineProps({ skill: Object, installed: Boolean })
</script>

<style scoped>
.skill-card { cursor: pointer; transition: transform 0.2s; }
.skill-card:hover { transform: translateY(-2px); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h4 { margin: 0; }
.card-tags { display: flex; gap: 6px; }
.card-desc { color: #666; margin: 10px 0; min-height: 40px; }
.card-meta { display: flex; gap: 16px; font-size: 13px; color: #999; }
</style>
