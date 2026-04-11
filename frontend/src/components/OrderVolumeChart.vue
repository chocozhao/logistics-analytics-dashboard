<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null

const initChart = (retryCount = 0) => {
  console.log('OrderVolumeChart initChart called', retryCount, 'chartRef.value:', !!chartRef.value, 'chartInstance:', !!chartInstance)
  if (!chartRef.value) {
    if (retryCount < 10) {
      setTimeout(() => {
        initChart(retryCount + 1)
      }, 100)
    }
    return
  }

  // Check if chart container has dimensions
  if (chartRef.value.clientWidth === 0 || chartRef.value.clientHeight === 0) {
    if (retryCount === 0) {
      console.warn('OrderVolumeChart: Chart container has no dimensions, retrying...')
    }
    if (retryCount < 10) {
      setTimeout(() => {
        initChart(retryCount + 1)
      }, 100)
    }
    return
  }

  // Check if chart instance already exists and is still valid
  let needInit = true
  if (chartInstance) {
    try {
      // Try to get the DOM element to check if instance is still valid
      const dom = chartInstance.getDom()
      if (dom && dom === chartRef.value) {
        // Instance is still valid, update chart instead of reinitializing
        console.log('OrderVolumeChart: Chart instance already exists, updating...')
        updateChart()
        needInit = false
      }
    } catch (err) {
      // Instance is disposed or invalid, need to reinitialize
      console.log('OrderVolumeChart: Chart instance is disposed, reinitializing...')
    }
  }

  if (needInit) {
    if (chartInstance) {
      chartInstance.dispose()
    }
    chartInstance = echarts.init(chartRef.value)
    updateChart()
  }
}

const updateChart = () => {
  console.log('OrderVolumeChart updateChart called', 'chartInstance:', !!chartInstance, 'chartRef.value:', !!chartRef.value, 'data length:', dashboardStore.orderVolumeData?.length || 0)
  try {
    // Initialize chart if needed
    if (!chartInstance && chartRef.value) {
      initChart(0)
      return
    }
  if (!chartInstance) return

  const data = dashboardStore.orderVolumeData
  console.log('OrderVolumeChart updateChart data:', data)
  if (!data || !Array.isArray(data) || data.length === 0) {
    // Clear chart or show no data message
    if (chartInstance) {
      chartInstance.clear()
      chartInstance.setOption({
        title: {
          text: '暂无订单量数据',
          left: 'center',
          top: 'center',
          textStyle: {
            color: '#909399',
            fontSize: 14,
            fontWeight: 'normal'
          }
        }
      })
    }
    return
  }
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
        return `${formattedDate}<br/>订单数: ${param.value}`
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
      name: '订单数'
    },
    series: [
      {
        name: '订单量',
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

  chartInstance.clear()
  chartInstance.setOption(option)
  chartInstance.resize()
  } catch (error) {
    console.error('OrderVolumeChart updateChart error:', error)
    // Optionally show error in chart
    if (chartInstance) {
      chartInstance.clear()
      chartInstance.setOption({
        title: {
          text: '图表渲染错误',
          subtext: error.message,
          left: 'center',
          top: 'center',
          textStyle: { color: '#f56c6c', fontSize: 14 }
        }
      })
    }
  }
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  // Wait for next tick to ensure DOM is ready
  setTimeout(() => {
    initChart(0)
  }, 100)
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
  console.log('OrderVolumeChart orderVolumeData watch triggered', 'data length:', dashboardStore.orderVolumeData?.length || 0)
  updateChart()
})

watch(
  () => [dashboardStore.loading, dashboardStore.error],
  ([loading, error]) => {
    console.log('OrderVolumeChart loading/error watch triggered', { loading, error, chartInstance: !!chartInstance })
    if (!loading && !error) {
      // 图表容器变为可见，确保图表更新并调整尺寸
      if (chartInstance) {
        // 如果图表实例已存在，调整尺寸以确保正确渲染
        setTimeout(() => {
          chartInstance.resize()
        }, 10)
      }
      updateChart()
    }
    // 当loading或error时，图表容器隐藏，不需要特殊处理
  }
)
</script>

<template>
  <div class="order-volume-chart">
    <div v-show="dashboardStore.loading" class="chart-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在加载订单量数据...</span>
    </div>
    <div v-show="!dashboardStore.loading && dashboardStore.error" class="chart-error">
      <el-icon class="error-icon"><Warning /></el-icon>
      <span>加载订单量数据出错</span>
    </div>
    <div v-show="!dashboardStore.loading && !dashboardStore.error" class="chart-container">
      <div ref="chartRef" class="chart"></div>
      <div v-if="dashboardStore.orderVolumeData.length === 0" class="no-data">
        当前筛选条件下无订单量数据
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
  height: 400px;
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