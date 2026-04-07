<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null

const granularity = ref('week')
const periods = ref(4)
const loadingForecast = ref(false)
const forecastError = ref(null)

const generateForecast = async () => {
  loadingForecast.value = true
  forecastError.value = null

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  try {
    await dashboardStore.fetchForecast(granularity.value, periods.value)
    setTimeout(() => {
      renderForecastChart()
    }, 100)
  } catch (err) {
    forecastError.value = err.message || 'Failed to generate forecast'
  } finally {
    loadingForecast.value = false
  }
}

const renderForecastChart = () => {
  if (!chartRef.value || !dashboardStore.forecastData) return

  chartInstance = echarts.init(chartRef.value)

  const forecastData = dashboardStore.forecastData
  const historicalData = forecastData.historical || []
  const forecastSeries = forecastData.forecast || []

  const allData = [...historicalData, ...forecastSeries]

  const xAxisData = allData.map(item => {
    const date = new Date(item.date)
    return `${date.getMonth() + 1}/${date.getDate()}`
  })

  const historicalValues = historicalData.map(item => item.count)
  const forecastValues = forecastSeries.map(item => item.count)

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
      text: 'Order Volume Forecast',
      left: 'center',
      top: 10
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const param = params[0]
        const dataIndex = param.dataIndex
        const item = allData[dataIndex]
        const date = new Date(item.date)
        const formattedDate = date.toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric'
        })
        const type = item.isForecast ? 'Forecast' : 'Historical'
        return `${formattedDate} (${type})<br/>Orders: ${item.count}`
      }
    },
    legend: {
      data: ['Historical', 'Forecast'],
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
      name: 'Orders'
    },
    series: [
      {
        name: 'Historical',
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
        name: 'Forecast',
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

  chartInstance.setOption(option)
}

const resizeChart = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  window.addEventListener('resize', resizeChart)
  // Generate initial forecast
  generateForecast()
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
</script>

<template>
  <div class="forecast-panel">
    <el-card class="forecast-card">
      <template #header>
        <h3>Demand Forecasting</h3>
        <p class="subtitle">Generate predictive insights for future order volumes.</p>
      </template>

      <div class="forecast-controls">
        <div class="control-group">
          <div class="control-item">
            <label>Granularity</label>
            <el-select v-model="granularity" :disabled="loadingForecast" size="small">
              <el-option label="Day" value="day" />
              <el-option label="Week" value="week" />
              <el-option label="Month" value="month" />
            </el-select>
          </div>

          <div class="control-item">
            <label>Forecast Periods</label>
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
              Generate Forecast
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
        <el-divider content-position="left">Forecast Visualization</el-divider>
        <div class="chart-container">
          <div v-if="loadingForecast" class="chart-loading">
            <el-icon class="loading-icon"><Loading /></el-icon>
            <span>Generating forecast...</span>
          </div>
          <div v-else-if="forecastError" class="chart-error">
            <el-icon class="error-icon"><Warning /></el-icon>
            <span>Error generating forecast</span>
          </div>
          <div v-else class="chart-wrapper">
            <div ref="chartRef" class="chart"></div>
            <div v-if="!dashboardStore.forecastData" class="no-data">
              No forecast data available. Click "Generate Forecast" to create one.
            </div>
          </div>
        </div>

        <div v-if="dashboardStore.forecastData?.recommendations" class="recommendations">
          <el-divider content-position="left">Recommendations</el-divider>
          <div class="recommendation-list">
            <div
              v-for="(rec, index) in dashboardStore.forecastData.recommendations"
              :key="index"
              class="recommendation-item"
            >
              <el-icon class="recommendation-icon">
                <component :is="rec.type === 'inventory' ? 'Box' : 'Truck'" />
              </el-icon>
              <div class="recommendation-content">
                <div class="recommendation-title">{{ rec.title }}</div>
                <div class="recommendation-description">{{ rec.description }}</div>
                <div class="recommendation-details">
                  <span class="detail-label">Impact:</span>
                  <el-tag :type="rec.impact === 'high' ? 'danger' : rec.impact === 'medium' ? 'warning' : 'success'" size="small">
                    {{ rec.impact }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="dashboardStore.forecastData?.metrics" class="forecast-metrics">
          <el-divider content-position="left">Forecast Accuracy Metrics</el-divider>
          <div class="metrics-grid">
            <div class="metric-item">
              <div class="metric-label">Mean Absolute Error (MAE)</div>
              <div class="metric-value">{{ dashboardStore.forecastData.metrics.mae?.toFixed(2) || 'N/A' }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">Forecast Method</div>
              <div class="metric-value">{{ dashboardStore.forecastData.metrics.method || 'N/A' }}</div>
            </div>
            <div class="metric-item">
              <div class="metric-label">Confidence Level</div>
              <div class="metric-value">{{ dashboardStore.forecastData.metrics.confidence || 'N/A' }}</div>
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
</style>