<template>
  <div class="market-page">
    <AppHeader />
    <div class="market-content">
      <SkillFilter @filter="onFilter" />
      <!-- 已下架 Skill 提示 -->
      <el-alert
        v-if="delistedInstalled.length > 0"
        type="info" show-icon :closable="false"
        style="margin-bottom:16px"
      >
        <template #title>
          你有 <strong>{{ delistedInstalled.length }}</strong> 个已安装的 Skill 被下架，不再显示在市场 —
          <el-button type="primary" link size="small" @click="$router.push('/workspace')">去我的安装查看</el-button>
        </template>
      </el-alert>
      <div class="skill-grid" v-loading="loading">
        <SkillCard v-for="s in skills" :key="s.id" :skill="s" :installed="installedMap[s.id]" />
      </div>
      <el-empty v-if="!loading && skills.length === 0" description="暂无已上架的Skill" />
      <el-pagination v-if="total > 0" :total="total" :page-size="filter.size" :current-page="filter.page"
        layout="prev, pager, next" @current-change="onPage" style="margin-top:20px;justify-content:center" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useSkillStore } from '@/stores/skill'
import { useWorkspaceStore } from '@/stores/workspace'
import AppHeader from '@/components/layout/AppHeader.vue'
import SkillFilter from '@/components/skill/SkillFilter.vue'
import SkillCard from '@/components/skill/SkillCard.vue'

const store = useSkillStore()
const skills = ref([])
const total = ref(0)
const loading = ref(false)
const installedMap = ref({})
const delistedInstalled = ref([])
const filter = reactive({ categoryId: null, keyword: '', sortBy: 'NEWEST', page: 1, size: 12 })

async function load() {
  loading.value = true
  try {
    const [res, installedRes] = await Promise.all([
      store.fetchMarket(filter),
      useWorkspaceStore().fetchInstalled()
    ])
    skills.value = store.marketSkills
    total.value = store.total
    // 已安装但已下架的 Skill
    const ws = useWorkspaceStore()
    delistedInstalled.value = ws.installedSkills.filter(s => s.status === 'DELISTED')
    // 批量检查安装状态
    const results = await Promise.allSettled(
      skills.value.map(s => store.checkInstallStatus(s.id))
    )
    const map = {}
    results.forEach((r, i) => {
      if (r.status === 'fulfilled' && r.value?.installed) map[skills.value[i].id] = true
    })
    installedMap.value = map
  } finally { loading.value = false }
}

function onFilter(f) { Object.assign(filter, f, { page: 1 }); load() }
function onPage(p) { filter.page = p; load() }

load()
</script>

<style scoped>
.market-content { max-width: 1200px; margin: 24px auto; padding: 0 24px; }
.skill-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
</style>
