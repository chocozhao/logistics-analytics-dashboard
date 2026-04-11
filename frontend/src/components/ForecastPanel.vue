<script setup>
import { ref, onMounted, watch, onUnmounted, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null

const granularity = ref('week')
const periods = ref(4)
const loadingForecast = ref(false)
const forecastError = ref(null)
const hasTriedInitialLoad = ref(false)

const generateForecast = async () => {
  console.log('generateForecast called with granularity:', granularity.value, 'periods:', periods.value)
  loadingForecast.value = true
  forecastError.value = null

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  try {
    console.log('Calling fetchForecast...')
    await dashboardStore.fetchForecast(granularity.value, periods.value)
    console.log('fetchForecast completed, forecastData:', dashboardStore.forecastData)
    // Mark that we've attempted initial load
    hasTriedInitialLoad.value = true
    // Render chart after a short delay to ensure DOM is ready
    setTimeout(() => {
      renderForecastChart()
    }, 300)
  } catch (err) {
    console.error('Error in generateForecast:', err)
    forecastError.value = err.message || '生成预测失败'
    hasTriedInitialLoad.value = true
  } finally {
    loadingForecast.value = false
  }
}

const initForecastChart = (maxRetries = 10, retryCount = 0) => {
  console.log('initForecastChart called', retryCount)
  if (!chartRef.value) {
    console.log('Missing chartRef')
    if (retryCount < maxRetries) {
      setTimeout(() => {
        initForecastChart(maxRetries, retryCount + 1)
      }, 100)
    }
    return false
  }

  // Check if chart container has dimensions
  if (chartRef.value.clientWidth === 0 || chartRef.value.clientHeight === 0) {
    if (retryCount === 0) {
      console.warn('Forecast chart container has no dimensions, retrying...')
    }
    if (retryCount < maxRetries) {
      setTimeout(() => {
        initForecastChart(maxRetries, retryCount + 1)
      }, 100)
    }
    return false
  }

  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)
  return true
}

const renderForecastChart = (retryCount = 0) => {
  console.log('renderForecastChart called, forecastData:', dashboardStore.forecastData, 'retryCount:', retryCount)
  if (!chartRef.value || !dashboardStore.forecastData) {
    console.log('Missing chartRef or forecastData:', { chartRef: chartRef.value, forecastData: dashboardStore.forecastData })
    // If we have forecast data but no chart ref, retry a few times
    if (dashboardStore.forecastData && !chartRef.value && retryCount < 10) {
      console.log('Chart ref not available, retrying...', retryCount)
      setTimeout(() => {
        renderForecastChart(retryCount + 1)
      }, 200)
    }
    return
  }

  // Initialize chart if needed
  if (!chartInstance && !initForecastChart()) {
    // Chart initialization failed, retry if we haven't exceeded max retries
    if (retryCount < 10) {
      console.log('Chart initialization failed, retrying...', retryCount)
      setTimeout(() => {
        renderForecastChart(retryCount + 1)
      }, 300)
    } else {
      console.error('Failed to initialize chart after', retryCount, 'retries')
      forecastError.value = '图表初始化失败，请重试'
    }
    return
  }
  if (!chartInstance) return

  const forecastResponse = dashboardStore.forecastData
  console.log('forecastResponse structure:', forecastResponse)
  console.log('forecastResponse keys:', Object.keys(forecastResponse || {}))

  if (!forecastResponse || !forecastResponse.data) {
    console.error('No forecast data available')
    chartInstance.clear()
    chartInstance.setOption({
      title: {
        text: '无预测数据',
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

  const allData = forecastResponse.data || []
  console.log('allData length:', allData.length, 'allData:', allData)

  if (allData.length === 0) {
    console.error('Empty forecast data array')
    chartInstance.clear()
    chartInstance.setOption({
      title: {
        text: '预测数据为空',
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

  // 分离历史数据和预测数据
  const historicalData = allData.filter(item => !item.isForecast)
  const forecastSeries = allData.filter(item => item.isForecast)
  console.log('historicalData length:', historicalData.length, 'forecastSeries length:', forecastSeries.length)

  const xAxisData = allData.map(item => {
    try {
      const date = new Date(item.date)
      if (isNaN(date.getTime())) {
        console.warn('Invalid date:', item.date)
        return '无效日期'
      }
      return `${date.getMonth() + 1}月${date.getDate()}日`
    } catch (e) {
      console.error('Error parsing date:', e, 'item:', item)
      return '日期错误'
    }
  })

  const historicalValues = historicalData.map(item => item.value || 0)
  const forecastValues = forecastSeries.map(item => item.value || 0)

  // Pad historical values with null for forecast positions
  const paddedHistorical = [...historicalValues]
  while (paddedHistorical.length < allData.length) {
    paddedHistorical.push(null)
  }

  // Pad forecast values with null for historical positions
  const paddedForecast = []
  for (let i = 0; i < historicalData.length; i++) {
    paddedForecast.push(null)
  }
  paddedForecast.push(...forecastValues)

  const option = {
    title: {
      text: '订单量预测',
      left: 'center',
      top: 10
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        try {
          const param = params[0]
          const dataIndex = param.dataIndex
          const item = allData[dataIndex]
          if (!item) {
            return `数据点 ${dataIndex}<br/>订单数: 0`
          }
          const date = new Date(item.date)
          let formattedDate = '无效日期'
          if (!isNaN(date.getTime())) {
            formattedDate = date.toLocaleDateString('zh-CN', {
              year: 'numeric',
              month: 'short',
              day: 'numeric'
            })
          }
          const type = item.isForecast ? '预测' : '历史'
          return `${formattedDate} (${type})<br/>订单数: ${item.value || 0}`
        } catch (e) {
          console.error('Error in tooltip formatter:', e)
          return '工具提示错误'
        }
      }
    },
    legend: {
      data: ['历史数据', '预测数据'],
      top: 40
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      axisLabel: {
        rotate: 45
      },
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '订单数'
    },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: paddedHistorical,
        itemStyle: {
          color: '#409eff'
        },
        lineStyle: {
          width: 3
        },
        symbol: 'circle',
        symbolSize: 6,
        markLine: {
          silent: true,
          lineStyle: {
            color: '#999',
            type: 'dashed',
            width: 1
          },
          data: [
            {
              xAxis: historicalData.length - 0.5
            }
          ]
        }
      },
      {
        name: '预测数据',
        type: 'line',
        data: paddedForecast,
        itemStyle: {
          color: '#e6a23c'
        },
        lineStyle: {
          width: 3,
          type: 'dashed'
        },
        symbol: 'circle',
        symbolSize: 6
      }
    ]
  }

  chartInstance.clear()
  chartInstance.setOption(option)
  // Ensure chart resizes after render
  chartInstance.resize()
  // Additional resize after a short delay to handle container sizing
  setTimeout(() => {
    if (chartInstance) {
      chartInstance.resize()
    }
  }, 50)
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  console.log('ForecastPanel mounted, setting up...')
  console.log('Current dashboardStore.dateRange:', dashboardStore.dateRange.value)
  console.log('Current dashboardStore.startDate:', dashboardStore.startDate)
  console.log('Current dashboardStore.endDate:', dashboardStore.endDate)
  console.log('Current dashboardStore.filters:', dashboardStore.filters.value)

  window.addEventListener('resize', resizeChart)
  // Generate initial forecast after a delay to ensure tab is fully rendered
  setTimeout(() => {
    console.log('Calling generateForecast from onMounted...')
    if (!hasTriedInitialLoad.value) {
      generateForecast()
    }
  }, 500)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

watch(() => dashboardStore.filters, () => {
  // Regenerate forecast when filters change
  generateForecast()
}, { deep: true })

watch(() => dashboardStore.forecastData, () => {
  // Render chart when forecast data changes
  if (dashboardStore.forecastData) {
    // Wait for next tick to ensure DOM is updated
    nextTick(() => {
      setTimeout(() => {
        renderForecastChart()
      }, 100)
    })
  }
}, { deep: true })
</script>

<template>
  <div class="forecast-panel">
    <el-card class="forecast-card">
      <template #header>
        <h3>需求预测</h3>
        <p class="subtitle">生成未来订单量的预测洞察。</p>
      </template>

      <div class="forecast-controls">
        <div class="control-group">
          <div class="control-item">
            <label>粒度</label>
            <el-select v-model="granularity" :disabled="loadingForecast" size="small">
              <el-option label="日" value="day" />
              <el-option label="周" value="week" />
              <el-option label="月" value="month" />
            </el-select>
          </div>

          <div class="control-item">
            <label>预测周期数</label>
            <el-input-number
              v-model="periods"
              :min="1"
              :max="12"
              :disabled="loadingForecast"
              size="small"
            />
          </div>

          <div class="control-item">
            <label>&nbsp;</label>
            <el-button
              type="primary"
              :loading="loadingForecast"
              @click="generateForecast"
            >
              生成预测
            </el-button>
          </div>
        </div>

        <div v-if="forecastError" class="error-message">
          <el-alert
            :title="forecastError"
            type="error"
            show-icon
            :closable="false"
          />
        </div>
      </div>

      <div class="forecast-results">
        <el-divider content-position="left">预测可视化</el-divider>
        <div class="chart-container">
          <div v-if="loadingForecast" class="chart-loading">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <span>正在生成预测...</span>
          </div>
          <div v-else-if="forecastError" class="chart-error">
            <el-icon class="error-icon"><Warning /></el-icon>
            <span>生成预测出错</span>
          </div>
          <div v-else class="chart-wrapper">
            <div ref="chartRef" class="chart"></div>
            <div v-if="!dashboardStore.forecastData && hasTriedInitialLoad" class="no-data">
              预测加载失败或无数据。点击"生成预测"重试。
            </div>
            <div v-else-if="!dashboardStore.forecastData" class="no-data">
              正在加载预测数据...
            </div>
          </div>
        </div>

        <div v-if="dashboardStore.forecastData?.recommendations" class="recommendations">
          <el-divider content-position="left">建议</el-divider>
          <div class="recommendation-text">
            {{ dashboardStore.forecastData.recommendations }}
          </div>
        </div>

        <div v-if="dashboardStore.forecastData" class="forecast-metrics">
          <el-divider content-position="left">预测指标</el-divider>
          <div class="metrics-grid">
            <div class="metric-item">
              <div class="metric-label">预测算法</div>
              <div class="metric-value">{{ dashboardStore.forecastData.algorithm || 'N/A' }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">预测周期数</div>
              <div class="metric-value">{{ dashboardStore.forecastData.forecastPeriods || 'N/A' }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">安全库存乘数</div>
              <div class="metric-value">{{ dashboardStore.forecastData.safetyStockMultiplier || 'N/A' }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.forecast-panel {
  padding: 20px;
}

.forecast-card {
  max-width: 1200px;
  margin: 0 auto;
}

.subtitle {
  margin: 10px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.forecast-controls {
  margin-bottom: 30px;
}

.control-group {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: flex-end;
}

.control-item {
  display: flex;
  flex-direction: column;
  min-width: 150px;
}

.control-item label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.error-message {
  margin-top: 20px;
}

.forecast-results {
  margin-top: 30px;
}

.chart-container {
  margin: 20px 0;
}

.chart-wrapper {
  position: relative;
}

.chart {
  width: 100%;
  height: 400px;
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

.recommendations {
  margin-top: 30px;
}

.recommendation-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.recommendation-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.recommendation-icon {
  font-size: 24px;
  color: #409eff;
  margin-top: 2px;
}

.recommendation-content {
  flex: 1;
}

.recommendation-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.recommendation-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.recommendation-details {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-label {
  font-size: 12px;
  color: #909399;
}

.forecast-metrics {
  margin-top: 30px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 15px;
}

.metric-item {
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  text-align: center;
}

.metric-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

@media (max-width: 768px) {
  .control-group {
    flex-direction: column;
    align-items: stretch;
  }

  .control-item {
    width: 100%;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
  .recommendation-text {
    padding: 15px;
    background-color: #f8f9fa;
    border-radius: 8px;
    border-left: 4px solid #67c23a;
    line-height: 1.6;
    white-space: pre-line;
  }

</style>