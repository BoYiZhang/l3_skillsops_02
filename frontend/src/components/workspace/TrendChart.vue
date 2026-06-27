<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">{{ title }}</span>
    </template>
    <v-chart v-if="slicedData.length > 0" :option="option" style="height: 280px" autoresize />
    <el-empty v-else description="暂无趋势数据" :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  data: { type: Array, default: () => [] },
  color: { type: String, default: '#1677ff' },
  range: { type: String, default: '7d' },
})

const slicedData = computed(() => {
  const limit = props.range === '7d' ? 7 : 30
  return props.data.slice(-limit)
})

const option = computed(() => {
  const dates = slicedData.value.map(d => d.date)
  const values = slicedData.value.map(d => d.count)

  return {
    grid: { top: 20, right: 20, bottom: 30, left: 50 },
    tooltip: {
      trigger: 'axis',
      formatter: (p) => `${p[0].axisValue}<br/><b>${p[0].value}</b> 次`,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: props.range === '30d' ? 45 : 0, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: props.color, width: 2 },
      itemStyle: { color: props.color },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: props.color + '40' },
            { offset: 1, color: props.color + '05' },
          ],
        },
      },
    }],
  }
})
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
