<script setup>
import { ref, onMounted, watch, onUnmounted, computed } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null

const carrierData = computed(() => {
  const data = dashboardStore.carrierBreakdownData
  if (!data || data.length === 0) return []

  return data.map(item => ({
    carrier: item.carrier,
    totalOrders: item.totalOrders,
    delayedOrders: item.delayedOrders,
    onTimeOrders: item.totalOrders - item.delayedOrders,
    delayRate: item.delayRate
  }))
})

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return

  const data = carrierData.value
  if (data.length === 0) {
    chartInstance.setOption({
      title: {
        text: 'No carrier breakdown data available',
        left: 'center',
        top: 'center',
        textStyle: {
          color: '#909399',
          fontSize: 14,
          fontWeight: 'normal'
        }
      }
    })
    return
  }

  const carriers = data.map(item => item.carrier)
  const onTimeData = data.map(item => item.onTimeOrders)
  const delayedData = data.map(item => item.delayedOrders)
  const delayRates = data.map(item => item.delayRate)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const param1 = params[0]
        const param2 = params[1]
        const dataIndex = param1.dataIndex
        const item = data[dataIndex]
        return `${item.carrier}<br/>
                ${param1.marker} ${param1.seriesName}: ${param1.value}<br/>
                ${param2.marker} ${param2.seriesName}: ${param2.value}<br/>
                Total Orders: ${item.totalOrders}<br/>
                Delay Rate: ${item.delayRate}%`
      }
    },
    legend: {
      data: ['On-Time', 'Delayed'],
      top: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: 'Orders'
    },
    yAxis: {
      type: 'category',
      data: carriers,
      axisLabel: {
        width: 80,
        overflow: 'truncate'
      }
    },
    series: [
      {
        name: 'On-Time',
        type: 'bar',
        stack: 'carrier',
        data: onTimeData,
        itemStyle: {
          color: '#67c23a'
        },
        label: {
          show: true,
          position: 'insideLeft',
          formatter: ({ dataIndex }) => {
            const item = data[dataIndex]
            return `${item.onTimeOrders}`
          }
        }
      },
      {
        name: 'Delayed',
        type: 'bar',
        stack: 'carrier',
        data: delayedData,
        itemStyle: {
          color: '#e6a23c'
        },
        label: {
          show: true,
          position: 'insideRight',
          formatter: ({ dataIndex }) => {
            const item = data[dataIndex]
            return `${item.delayedOrders}`
          }
        }
      }
    ]
  }

  chartInstance.setOption(option)
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

watch(() => dashboardStore.carrierBreakdownData, () => {
  updateChart()
}, { deep: true })

watch(() => dashboardStore.loading, (loading) => {
  if (!loading && chartInstance) {
    updateChart()
  }
})
</script>

<template>
  <div class="carrier-breakdown-chart">
    <div v-if="dashboardStore.loading" class="chart-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>Loading carrier breakdown data...</span>
    </div>
    <div v-else-if="dashboardStore.error" class="chart-error">
      <el-icon class="error-icon"><Warning /></el-icon>
      <span>Error loading carrier breakdown data</span>
    </div>
    <div v-else class="chart-container">
      <div ref="chartRef" class="chart"></div>
      <div v-if="dashboardStore.carrierBreakdownData.length === 0" class="no-data">
        No carrier breakdown data available for selected filters
      </div>
    </div>
  </div>
</template>

<style scoped>
.carrier-breakdown-chart {
  width: 100%;
  height: 100%;
  position: relative;
}

.chart-container {
  width: 100%;
  height: 100%;
}

.chart {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.chart-loading,
.chart-error,
.no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 400px;
  color: #909399;
  font-size: 14px;
}

.loading-icon,
.error-icon {
  margin-right: 8px;
}

.chart-loading {
  color: #409eff;
}

.chart-error {
  color: #f56c6c;
}
</style>