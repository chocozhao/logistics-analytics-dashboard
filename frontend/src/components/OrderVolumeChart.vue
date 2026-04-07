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

  const data = dashboardStore.orderVolumeData
  const isDaily = data.length > 0 && data[0]?.date?.includes('-') // Simple check

  const xAxisData = data.map(item => {
    const date = new Date(item.date)
    return isDaily ? `${date.getMonth() + 1}/${date.getDate()}` : item.date
  })

  const seriesData = data.map(item => item.count)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const param = params[0]
        const date = new Date(data[param.dataIndex].date)
        const formattedDate = date.toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric'
        })
        return `${formattedDate}<br/>Orders: ${param.value}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
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
        name: 'Order Volume',
        type: 'line',
        data: seriesData,
        smooth: true,
        lineStyle: {
          width: 3
        },
        itemStyle: {
          color: '#409eff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
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

watch(() => dashboardStore.orderVolumeData, () => {
  updateChart()
}, { deep: true })

watch(() => dashboardStore.loading, (loading) => {
  if (!loading && chartInstance) {
    updateChart()
  }
})
</script>

<template>
  <div class="order-volume-chart">
    <div v-if="dashboardStore.loading" class="chart-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>Loading order volume data...</span>
    </div>
    <div v-else-if="dashboardStore.error" class="chart-error">
      <el-icon class="error-icon"><Warning /></el-icon>
      <span>Error loading order volume data</span>
    </div>
    <div v-else class="chart-container">
      <div ref="chartRef" class="chart"></div>
      <div v-if="dashboardStore.orderVolumeData.length === 0" class="no-data">
        No order volume data available for selected filters
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-volume-chart {
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