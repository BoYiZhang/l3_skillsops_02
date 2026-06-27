<template>
  <el-header class="app-header">
    <div class="header-left">
      <h3 @click="$router.push('/market')" class="logo">SkillsOps</h3>
      <el-menu mode="horizontal" :default-active="activeMenu" @select="nav" class="header-menu">
        <el-menu-item index="/market">市场</el-menu-item>
      </el-menu>
      <!-- 面包屑：详情页显示 -->
      <el-breadcrumb v-if="route.name === 'SkillDetail'" separator="/" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/market' }">市场</el-breadcrumb-item>
        <el-breadcrumb-item>Skill 详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <span class="user-info">
          <el-avatar size="small" style="margin-right:6px">{{ auth.user?.username?.charAt(0)?.toUpperCase() }}</el-avatar>
          {{ auth.user?.username }}
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="workspace">工作台</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => '/market')

function nav(index) { router.push(index) }
function handleCommand(cmd) {
  if (cmd === 'workspace') router.push('/workspace')
  if (cmd === 'logout') { auth.logout(); router.push('/login') }
}
</script>

<style scoped>
.app-header {
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #e4e7ed; background: #fff; padding: 0 24px;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.logo { cursor: pointer; color: #409EFF; margin: 0; white-space: nowrap; font-size: 18px; }
.header-menu { border-bottom: none !important; }
.breadcrumb { padding-left: 8px; border-left: 1px solid #e4e7ed; }
.header-right { display: flex; align-items: center; }
.user-info { cursor: pointer; display: flex; align-items: center; }
</style>
