<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const destroyChart = () => {
  if (resizeObserver && chartRef.value) resizeObserver.unobserve(chartRef.value)
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)

  const data = dashboardStore.orderVolumeData
  if (!data || !data.length) {
    chartInstance.setOption({ title: { text: '暂无订单量数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14 } } })
    return
  }

  const labels = data.map(d => {
    const dt = new Date(d.date)
    return isNaN(dt) ? d.date : `${dt.getMonth() + 1}/${dt.getDate()}`
  })

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        const i = params[0].dataIndex
        const dt = new Date(data[i].date)
        const ds = isNaN(dt) ? data[i].date : dt.toLocaleDateString('zh-CN')
        return `${ds}<br/>订单数: ${params[0].value}`
      }
    },
    grid: { left: 50, right: 20, top: 20, bottom: 60, containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '订单数' },
    series: [{
      name: '订单量',
      type: 'line',
      data: data.map(d => d.count),
      smooth: true,
      lineStyle: { width: 3 },
      itemStyle: { color: '#409eff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.3)' },
          { offset: 1, color: 'rgba(64,158,255,0.05)' }
        ])
      }
    }]
  })

  if (typeof ResizeObserver !== 'undefined' && !resizeObserver) {
    resizeObserver = new ResizeObserver(() => chartInstance && chartInstance.resize())
    resizeObserver.observe(chartRef.value)
  }
}

onMounted(() => renderChart())
onBeforeUnmount(() => destroyChart())

watch(() => dashboardStore.orderVolumeData, () => renderChart(), { deep: true })
</script>

<template>
  <div class="chart-wrap">
    <div v-if="!dashboardStore.orderVolumeData?.length" class="placeholder">暂无订单量数据</div>
    <div v-else ref="chartRef" class="chart" />
  </div>
</template>

<style scoped>
.chart-wrap { width: 100%; height: 100%; }
.chart { width: 100%; height: 350px; }
.placeholder { height: 350px; display: flex; align-items: center; justify-content: center; color: #909399; font-size: 14px; }
</style>
