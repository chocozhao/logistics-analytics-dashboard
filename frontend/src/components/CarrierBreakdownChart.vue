<script setup>
import { ref, onMounted, watch, onUnmounted, computed } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null

const carrierData = computed(() => {
  const data = dashboardStore.carrierBreakdownData
  if (!Array.isArray(data) || data.length === 0) return []

  return data.map(item => ({
    carrier: item.carrier,
    totalOrders: item.totalOrders,
    delayedOrders: item.delayedOrders,
    onTimeOrders: item.totalOrders - item.delayedOrders,
    delayRate: typeof item.delayRate === 'number' ? item.delayRate :
              (item.delayRate ? Number(item.delayRate) : 0)
  }))
})

const initChart = (retryCount = 0) => {
  console.log('CarrierBreakdownChart initChart called', retryCount, 'chartRef.value:', !!chartRef.value, 'chartInstance:', !!chartInstance)
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
      console.warn('CarrierBreakdownChart: Chart container has no dimensions, retrying...')
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
        console.log('CarrierBreakdownChart: Chart instance already exists, updating...')
        updateChart()
        needInit = false
      }
    } catch (err) {
      // Instance is disposed or invalid, need to reinitialize
      console.log('CarrierBreakdownChart: Chart instance is disposed, reinitializing...')
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
  console.log('CarrierBreakdownChart updateChart called', 'chartInstance:', !!chartInstance, 'chartRef.value:', !!chartRef.value, 'data length:', dashboardStore.carrierBreakdownData?.length || 0)
  try {
    // Initialize chart if needed
    if (!chartInstance && chartRef.value) {
      initChart(0)
      return
    }
    if (!chartInstance) return

    console.log('CarrierBreakdownChart carrierBreakdownData:', dashboardStore.carrierBreakdownData)
    const data = carrierData.value
    console.log('CarrierBreakdownChart computed data:', data)
    if (!data || data.length === 0) {
      chartInstance.clear()
      chartInstance.setOption({
        title: {
          text: '暂无承运商细分数据',
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
                总订单数: ${item.totalOrders}<br/>
                延迟率: ${item.delayRate}%`
      }
    },
    legend: {
      data: ['准时', '延迟'],
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
      name: '订单数'
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
        name: '准时',
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
        name: '延迟',
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

  chartInstance.clear()
  chartInstance.setOption(option)
  chartInstance.resize()
  } catch (error) {
    console.error('CarrierBreakdownChart updateChart error:', error)
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

watch(() => dashboardStore.carrierBreakdownData, () => {
  console.log('CarrierBreakdownChart carrierBreakdownData watch triggered', 'data length:', dashboardStore.carrierBreakdownData?.length || 0)
  updateChart()
})

watch(
  () => [dashboardStore.loading, dashboardStore.error],
  ([loading, error]) => {
    console.log('CarrierBreakdownChart loading/error watch triggered', { loading, error, chartInstance: !!chartInstance })
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
  <div class="carrier-breakdown-chart">
    <div v-show="dashboardStore.loading" class="chart-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在加载承运商细分数据...</span>
    </div>
    <div v-show="!dashboardStore.loading && dashboardStore.error" class="chart-error">
      <el-icon class="error-icon"><Warning /></el-icon>
      <span>加载承运商细分数据出错</span>
    </div>
    <div v-show="!dashboardStore.loading && !dashboardStore.error" class="chart-container">
      <div ref="chartRef" class="chart"></div>
      <div v-if="dashboardStore.carrierBreakdownData.length === 0" class="no-data">
        当前筛选条件下无承运商细分数据
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
  height: 400px;
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