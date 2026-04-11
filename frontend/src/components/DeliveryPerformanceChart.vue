<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null

const initChart = (retryCount = 0) => {
  console.log('DeliveryPerformanceChart initChart called', retryCount, 'chartRef.value:', !!chartRef.value, 'chartInstance:', !!chartInstance)
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
      console.warn('DeliveryPerformanceChart: Chart container has no dimensions, retrying...')
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
        console.log('DeliveryPerformanceChart: Chart instance already exists, updating...')
        updateChart()
        needInit = false
      }
    } catch (err) {
      // Instance is disposed or invalid, need to reinitialize
      console.log('DeliveryPerformanceChart: Chart instance is disposed, reinitializing...')
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
  console.log('DeliveryPerformanceChart updateChart called', 'chartInstance:', !!chartInstance, 'chartRef.value:', !!chartRef.value, 'data length:', dashboardStore.deliveryPerformanceData?.length || 0)
  try {
    // Initialize chart if needed
    if (!chartInstance && chartRef.value) {
      initChart(0)
      return
    }
  if (!chartInstance) return

  const data = dashboardStore.deliveryPerformanceData
  console.log('DeliveryPerformanceChart updateChart data:', data)
  if (!data || data.length === 0) {
    chartInstance.clear()
    chartInstance.setOption({
      title: {
        text: '暂无交货性能数据',
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

  // Transform API data to chart-compatible format
  const chartData = data.map(item => {
    const totalOrders = (item.onTime || 0) + (item.delayed || 0)
    return {
      period: item.period,
      onTime: item.onTime || 0,
      delayed: item.delayed || 0,
      totalOrders
    }
  })

  const xAxisData = chartData.map(item => {
    // period could be "2024-W01" for weekly or "2024-01" for monthly
    // Try to parse as date, fallback to using period string
    try {
      // Check if period is in ISO date format (YYYY-MM-DD)
      if (item.period && item.period.match(/^\d{4}-\d{2}-\d{2}$/)) {
        const date = new Date(item.period)
        return `${date.getMonth() + 1}/${date.getDate()}`
      }
      // For weekly format "2024-W01"
      const weekMatch = item.period.match(/W(\d+)$/)
      if (weekMatch) {
        return `W${weekMatch[1]}`
      }
      // For monthly format "2024-01"
      const monthMatch = item.period.match(/^\d{4}-(\d{2})$/)
      if (monthMatch) {
        return `M${parseInt(monthMatch[1])}`
      }
      // Return the period as-is
      return item.period
    } catch {
      return item.period || ''
    }
  })

  const onTimeData = chartData.map(item => item.onTime)
  const delayedData = chartData.map(item => item.delayed)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const param1 = params[0]
        const param2 = params[1]
        const dataIndex = param1.dataIndex
        const item = chartData[dataIndex]
        // Format period for display
        let displayPeriod = item.period
        try {
          if (item.period && item.period.match(/^\d{4}-\d{2}-\d{2}$/)) {
            const date = new Date(item.period)
            displayPeriod = date.toLocaleDateString('en-US', {
              year: 'numeric',
              month: 'short',
              day: 'numeric'
            })
          }
        } catch {}
        return `${displayPeriod}<br/>
                ${param1.marker} ${param1.seriesName}: ${param1.value}<br/>
                ${param2.marker} ${param2.seriesName}: ${param2.value}<br/>
                总计: ${item.totalOrders}`
      }
    },
    legend: {
      data: ['准时交货', '延迟交货'],
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
      name: '订单数'
    },
    series: [
      {
        name: '准时交货',
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
        name: '延迟交货',
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

  chartInstance.clear()
  chartInstance.setOption(option)
  chartInstance.resize()
  } catch (error) {
    console.error('DeliveryPerformanceChart updateChart error:', error)
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

watch(() => dashboardStore.deliveryPerformanceData, () => {
  console.log('DeliveryPerformanceChart deliveryPerformanceData watch triggered', 'data length:', dashboardStore.deliveryPerformanceData?.length || 0)
  updateChart()
})

watch(
  () => [dashboardStore.loading, dashboardStore.error],
  ([loading, error]) => {
    console.log('DeliveryPerformanceChart loading/error watch triggered', { loading, error, chartInstance: !!chartInstance })
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
  <div class="delivery-performance-chart">
    <div v-show="dashboardStore.loading" class="chart-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在加载交货性能数据...</span>
    </div>
    <div v-show="!dashboardStore.loading && dashboardStore.error" class="chart-error">
      <el-icon class="error-icon"><Warning /></el-icon>
      <span>加载交货性能数据出错</span>
    </div>
    <div v-show="!dashboardStore.loading && !dashboardStore.error" class="chart-container">
      <div ref="chartRef" class="chart"></div>
      <div v-if="dashboardStore.deliveryPerformanceData.length === 0" class="no-data">
        当前筛选条件下无交货性能数据
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