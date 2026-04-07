<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chartInstance) return

  const data = dashboardStore.deliveryPerformanceData
  if (!data || data.length === 0) {
    chartInstance.setOption({
      title: {
        text: 'No delivery performance data available',
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

  const xAxisData = data.map(item => {
    const date = new Date(item.date)
    return `${date.getMonth() + 1}/${date.getDate()}`
  })

  const onTimeData = data.map(item => item.totalOrders - item.delayedOrders)
  const delayedData = data.map(item => item.delayedOrders)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const param1 = params[0]
        const param2 = params[1]
        const dataIndex = param1.dataIndex
        const date = new Date(data[dataIndex].date)
        const formattedDate = date.toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric'
        })
        return `${formattedDate}<br/>
                ${param1.marker} ${param1.seriesName}: ${param1.value}<br/>
                ${param2.marker} ${param2.seriesName}: ${param2.value}<br/>
                Total: ${data[dataIndex].totalOrders}`
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
      type: 'category',
      data: xAxisData,
      axisLabel: {
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: 'Orders'
    },
    series: [
      {
        name: 'On-Time',
        type: 'bar',
        stack: 'delivery',
        data: onTimeData,
        itemStyle: {
          color: '#67c23a'
        },
        emphasis: {
          focus: 'series'
        }
      },
      {
        name: 'Delayed',
        type: 'bar',
        stack: 'delivery',
        data: delayedData,
        itemStyle: {
          color: '#e6a23c'
        },
        emphasis: {
          focus: 'series'
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

watch(() => dashboardStore.deliveryPerformanceData, () => {
  updateChart()
}, { deep: true })

watch(() => dashboardStore.loading, (loading) => {
  if (!loading && chartInstance) {
    updateChart()
  }
})
</script>

<template>
  <div class="delivery-performance-chart">
    <div v-if="dashboardStore.loading" class="chart-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>Loading delivery performance data...</span>
    </div>
    <div v-else-if="dashboardStore.error" class="chart-error">
      <el-icon class="error-icon"><Warning /></el-icon>
      <span>Error loading delivery performance data</span>
    </div>
    <div v-else class="chart-container">
      <div ref="chartRef" class="chart"></div>
      <div v-if="dashboardStore.deliveryPerformanceData.length === 0" class="no-data">
        No delivery performance data available for selected filters
      </div>
    </div>
  </div>
</template>

<style scoped>
.delivery-performance-chart {
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
  min-height: 300px;
}

.chart-loading,
.chart-error,
.no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 300px;
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