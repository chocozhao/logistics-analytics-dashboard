<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const carrierData = computed(() => {
  const raw = dashboardStore.carrierBreakdownData
  if (!Array.isArray(raw)) return []
  return raw.map(d => ({
    carrier: d.carrier,
    total: d.totalOrders,
    delayed: d.delayedOrders,
    onTime: d.totalOrders - d.delayedOrders,
    rate: typeof d.delayRate === 'number' ? d.delayRate : Number(d.delayRate || 0)
  }))
})

const destroyChart = () => {
  if (resizeObserver && chartRef.value) resizeObserver.unobserve(chartRef.value)
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
}

const renderChart = () => {
  if (!chartRef.value) return
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)

  const data = carrierData.value
  if (!data.length) {
    chartInstance.setOption({ title: { text: '暂无承运商数据', left: 'center', top: 'center', textStyle: { color: '#909399', fontSize: 14 } } })
    return
  }

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const i = params[0].dataIndex
        const d = data[i]
        return `${d.carrier}<br/>准时: ${d.onTime}<br/>延误: ${d.delayed}<br/>合计: ${d.total}<br/>延误率: ${d.rate}%`
      }
    },
    legend: { data: ['准时', '延误'], top: 10 },
    grid: { left: 80, right: 20, top: 50, bottom: 20, containLabel: true },
    xAxis: { type: 'value', name: '订单数' },
    yAxis: { type: 'category', data: data.map(d => d.carrier) },
    series: [
      {
        name: '准时',
        type: 'bar',
        stack: 'carrier',
        data: data.map(d => d.onTime),
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '延误',
        type: 'bar',
        stack: 'carrier',
        data: data.map(d => d.delayed),
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

watch(carrierData, () => nextTick(renderChart), { flush: 'post' })
</script>

<template>
  <div class="chart-wrap">
    <div v-if="!carrierData.length" class="placeholder">暂无承运商数据</div>
    <div v-else ref="chartRef" class="chart" />
  </div>
</template>

<style scoped>
.chart-wrap { width: 100%; height: 100%; }
.chart { width: 100%; height: 400px; }
.placeholder { height: 400px; display: flex; align-items: center; justify-content: center; color: #909399; font-size: 14px; }
</style>
