<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">🏆 热门 Skill Top 10</span>
    </template>
    <v-chart v-if="skills?.length" :option="option" style="height: 360px" autoresize />
    <el-empty v-else description="暂无数据" :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  skills: { type: Array, default: () => [] },
})

const RANK_COLORS = ['#f53f3f','#ff7d00','#f7ba1e','#1677ff','#1677ff',
                     '#1677ff','#1677ff','#1677ff','#1677ff','#1677ff']

const option = computed(() => {
  const reversed = [...props.skills].reverse()
  const names = reversed.map((s, i) => `${props.skills.length - i}. ${s.name}`)
  const values = reversed.map(s => s.installCount ?? 0)

  return {
    grid: { top: 10, right: 30, bottom: 20, left: 120 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (p) => `${p[0].name}<br/>安装量: <b>${p[0].value}</b>`,
    },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 12 },
    },
    series: [{
      type: 'bar',
      barWidth: '60%',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: {
          color: RANK_COLORS[i],
          borderRadius: [0, 4, 4, 0],
        },
      })),
      label: { show: true, position: 'right', fontSize: 12, fontWeight: 600 },
    }],
  }
})
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
