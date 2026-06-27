<template>
  <el-header class="app-header">
    <div class="header-left">
      <h3 @click="$router.push('/market')" style="cursor:pointer;color:#409EFF">SkillsOps</h3>
      <el-menu mode="horizontal" :default-active="active" @select="nav" class="header-menu">
        <el-menu-item index="/market">市场</el-menu-item>
        <el-menu-item index="/workspace">工作台</el-menu-item>
      </el-menu>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleCommand">
        <span class="user-info">{{ auth.user?.username }} <el-icon><ArrowDown /></el-icon></span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
const active = computed(() => route.path.startsWith('/workspace') ? '/workspace' : '/market')

function nav(index) { router.push(index) }
function handleCommand(cmd) {
  if (cmd === 'logout') { auth.logout(); router.push('/login') }
}
</script>

<style scoped>
.app-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e4e7ed; background: #fff; padding: 0 24px; }
.header-left { display: flex; align-items: center; gap: 24px; }
.header-menu { border-bottom: none !important; }
.user-info { cursor: pointer; }
</style>
