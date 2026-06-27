<template>
  <el-card shadow="never" class="chart-card">
    <template #header>
      <span class="chart-title">📂 Skill 分类分布</span>
    </template>
    <v-chart v-if="data?.length" :option="option" style="height: 280px" autoresize />
    <el-empty v-else description="暂无分类数据" :image-size="80" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const COLORS = ['#1677ff','#52c41a','#fa8c16','#722ed1','#eb2f96','#13c2c2','#f5222d','#faad14']

const option = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)',
  },
  legend: {
    orient: 'vertical',
    right: 10,
    top: 'center',
    textStyle: { fontSize: 12 },
  },
  color: COLORS,
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    center: ['38%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
      scaleSize: 8,
    },
    data: props.data.map(d => ({ name: d.categoryName, value: d.count })),
  }],
}))
</script>

<style scoped>
.chart-card { margin-bottom: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #333; }
</style>
