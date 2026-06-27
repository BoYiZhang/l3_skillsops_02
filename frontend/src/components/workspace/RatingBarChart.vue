<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">⭐ 评分分布</span>
    </template>
    <v-chart v-if="data?.length" :option="option" style="height: 280px" autoresize />
    <el-empty v-else description="暂无评分数据" :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const STAR_COLORS = ['#f5222d','#fa8c16','#faad14','#a0d911','#52c41a']

const option = computed(() => ({
  grid: { top: 20, right: 20, bottom: 30, left: 40 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: props.data.map(d => d.star + ' 星'),
    axisLabel: { fontSize: 13 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f0f0' } },
  },
  series: [{
    type: 'bar',
    barWidth: '50%',
    data: props.data.map((d, i) => ({
      value: d.count,
      itemStyle: { color: STAR_COLORS[i], borderRadius: [4, 4, 0, 0] },
    })),
    label: { show: true, position: 'top', fontSize: 13, fontWeight: 600 },
  }],
}))
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
