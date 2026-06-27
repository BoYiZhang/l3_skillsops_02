<template>
  <div class="workspace-page">
    <AppHeader />
    <div class="workspace-content">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的发布" name="published" />
        <el-tab-pane label="我的安装" name="installed" />
        <el-tab-pane v-if="isAdmin" label="待审核" name="review" />
        <el-tab-pane v-if="isAdmin" label="运营统计" name="stats" />
      </el-tabs>

      <MySkillsTab v-if="activeTab === 'published'" />
      <InstalledTab v-if="activeTab === 'installed'" />
      <ReviewTab v-if="activeTab === 'review'" />
      <StatsTab v-if="activeTab === 'stats'" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/layout/AppHeader.vue'
import MySkillsTab from '@/components/workspace/MySkillsTab.vue'
import InstalledTab from '@/components/workspace/InstalledTab.vue'
import ReviewTab from '@/components/workspace/ReviewTab.vue'
import StatsTab from '@/components/workspace/StatsTab.vue'

const route = useRoute()
const auth = useAuthStore()
const isAdmin = computed(() => auth.isAdmin)
const activeTab = ref(route.query.tab || 'published')
</script>

<style scoped>
.workspace-content { max-width: 1100px; margin: 24px auto; padding: 0 24px; }
</style>
