<template>
  <div class="detail-page">
    <AppHeader />
    <div class="detail-content" v-loading="loading">
      <SkillInfo v-if="skill" :skill="skill" @refresh="loadSkill" />
      <VersionList v-if="skill" :skillId="skill.id" style="margin-top:24px" />
      <RatingList v-if="skill" :skillId="skill.id" :installed="installed" style="margin-top:24px" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillStore } from '@/stores/skill'
import AppHeader from '@/components/layout/AppHeader.vue'
import SkillInfo from '@/components/skill/SkillInfo.vue'
import VersionList from '@/components/skill/VersionList.vue'
import RatingList from '@/components/skill/RatingList.vue'

const route = useRoute()
const store = useSkillStore()
const skill = ref(null)
const loading = ref(false)
const installed = ref(false)

async function loadSkill() {
  loading.value = true
  try {
    skill.value = await store.fetchDetail(route.params.id)
    const res = await store.checkInstallStatus(skill.value.id)
    installed.value = res.installed
  } finally { loading.value = false }
}

onMounted(loadSkill)
</script>

<style scoped>
.detail-content { max-width: 900px; margin: 24px auto; padding: 0 24px; }
</style>
