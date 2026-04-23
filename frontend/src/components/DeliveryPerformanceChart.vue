<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
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

  const data = dashboardStore.deliveryPerformanceData
  if (!data || !data.length) {
    chartInstance.setOption({ title: { text: '暂无交货性能数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14 } } })
    return
  }

  const labels = data.map(d => {
    const dt = new Date(d.period)
    return isNaN(dt) ? d.period : `${dt.getMonth() + 1}/${dt.getDate()}`
  })

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const i = params[0].dataIndex
        const d = data[i]
        const total = (d.onTime || 0) + (d.delayed || 0)
        return `${d.period}<br/>准时: ${d.onTime || 0}<br/>延误: ${d.delayed || 0}<br/>合计: ${total}`
      }
    },
    legend: { data: ['准时交货', '延迟交货'], top: 10 },
    grid: { left: 50, right: 20, top: 50, bottom: 60, containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: 30, fontSize: 11 } },
    yAxis: { type: 'value', name: '订单数' },
    series: [
      {
        name: '准时交货',
        type: 'bar',
        stack: 'delivery',
        data: data.map(d => d.onTime || 0),
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '延迟交货',
        type: 'bar',
        stack: 'delivery',
        data: data.map(d => d.delayed || 0),
        itemStyle: { color: '#e6a23c' }
      }
    ]
  })

  if (typeof ResizeObserver !== 'undefined' && !resizeObserver) {
    resizeObserver = new ResizeObserver(() => chartInstance && chartInstance.resize())
    resizeObserver.observe(chartRef.value)
  }
}

onMounted(() => nextTick(renderChart))
onBeforeUnmount(() => destroyChart())

watch(() => dashboardStore.deliveryPerformanceData, () => nextTick(renderChart), { deep: true, flush: 'post' })
</script>

<template>
  <div class="chart-wrap">
    <div v-if="!dashboardStore.deliveryPerformanceData?.length" class="placeholder">暂无交货性能数据</div>
    <div v-else ref="chartRef" class="chart" />
  </div>
</template>

<style scoped>
.chart-wrap { width: 100%; height: 100%; }
.chart { width: 100%; height: 350px; }
.placeholder { height: 350px; display: flex; align-items: center; justify-content: center; color: #909399; font-size: 14px; }
</style>
