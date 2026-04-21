<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const granularity = ref('week')
const periods = ref(4)
const loadingForecast = ref(false)
const forecastError = ref(null)

// ── Generate forecast ─────────────────────────────────────────────────────────

const generateForecast = async () => {
  loadingForecast.value = true
  forecastError.value = null
  destroyChart()

  try {
    await dashboardStore.fetchForecast(granularity.value, periods.value)
    await nextTick()
    renderChart()
  } catch (err) {
    forecastError.value = err.message || '生成预测失败'
  } finally {
    loadingForecast.value = false
  }
}

// ── Chart ─────────────────────────────────────────────────────────────────────

const destroyChart = () => {
  if (resizeObserver && chartRef.value) resizeObserver.unobserve(chartRef.value)
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
}

const renderChart = () => {
  if (!chartRef.value || !dashboardStore.forecastData?.data) return

  chartInstance = echarts.init(chartRef.value)

  const allData = dashboardStore.forecastData.data
  const historical = allData.filter(d => !d.isForecast)
  const forecast   = allData.filter(d =>  d.isForecast)

  const labels = allData.map(d => {
    const dt = new Date(d.date)
    return isNaN(dt) ? d.date : `${dt.getMonth()+1}/${dt.getDate()}`
  })

  const histVals = [...historical.map(d => d.value), ...Array(forecast.length).fill(null)]
  const foreVals = [...Array(historical.length).fill(null), ...forecast.map(d => d.value)]

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const idx = params[0].dataIndex
        const item = allData[idx]
        const dt = new Date(item.date)
        const dateStr = isNaN(dt) ? item.date : dt.toLocaleDateString('zh-CN')
        return `${dateStr} (${item.isForecast ? '预测' : '历史'})<br/>订单数: ${item.value}`
      }
    },
    legend: { data: ['历史数据', '预测数据'], top: 10 },
    grid: { left: 60, right: 20, top: 50, bottom: 60, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: { type: 'value', name: '订单数' },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: histVals,
        smooth: true,
        itemStyle: { color: '#409eff' },
        lineStyle: { width: 2 },
        connectNulls: false,
      },
      {
        name: '预测数据',
        type: 'line',
        data: foreVals,
        smooth: true,
        itemStyle: { color: '#e6a23c' },
        lineStyle: { type: 'dashed', width: 2 },
        symbol: 'circle',
        connectNulls: false,
      }
    ]
  })

  // Cross-browser resize
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => chartInstance && chartInstance.resize())
    resizeObserver.observe(chartRef.value)
  } else {
    window.addEventListener('resize', () => chartInstance && chartInstance.resize())
  }
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────

onMounted(() => {
  generateForecast()
})

onBeforeUnmount(() => {
  destroyChart()
})

watch(() => dashboardStore.filters, () => {
  generateForecast()
}, { deep: true })
</script>

<template>
  <div class="forecast-panel">
    <el-card>
      <template #header>
        <div>
          <h3 style="margin:0">需求预测</h3>
          <p style="margin:6px 0 0;color:#909399;font-size:13px">
            基于历史数据的确定性预测（Holt双指数平滑法）。AI仅负责参数提取，预测值由确定性算法计算。
          </p>
        </div>
      </template>

      <!-- Controls -->
      <div class="controls">
        <label>粒度</label>
        <el-select v-model="granularity" size="small" :disabled="loadingForecast" style="width:100px">
          <el-option label="日" value="day" />
          <el-option label="周" value="week" />
          <el-option label="月" value="month" />
        </el-select>

        <label>预测周期</label>
        <el-input-number v-model="periods" :min="1" :max="12" size="small" :disabled="loadingForecast" style="width:100px" />

        <el-button type="primary" size="small" :loading="loadingForecast" @click="generateForecast">
          生成预测
        </el-button>
      </div>

      <el-alert v-if="forecastError" :title="forecastError" type="error" show-icon :closable="false" style="margin:12px 0" />

      <!-- Chart -->
      <el-divider content-position="left">预测可视化</el-divider>
      <div v-if="loadingForecast" class="placeholder">正在生成预测...</div>
      <div v-else-if="!dashboardStore.forecastData?.data?.length" class="placeholder">暂无数据，请点击"生成预测"</div>
      <div v-else ref="chartRef" class="chart-area" />

      <!-- Recommendations -->
      <template v-if="dashboardStore.forecastData">
        <el-divider content-position="left">建议</el-divider>
        <pre class="rec-text">{{ dashboardStore.forecastData.recommendations }}</pre>

        <el-divider content-position="left">预测指标</el-divider>
        <div class="metrics-row">
          <div class="metric-card">
            <div class="metric-label">算法</div>
            <div class="metric-value small">{{ dashboardStore.forecastData.algorithm }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">预测周期</div>
            <div class="metric-value">{{ dashboardStore.forecastData.periods }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">安全库存系数</div>
            <div class="metric-value">× {{ dashboardStore.forecastData.safetyStockMultiplier }}</div>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.forecast-panel { padding: 20px; }

.controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.controls label { font-size: 13px; color: #606266; }

.chart-area { width: 100%; height: 400px; }
.placeholder { height: 300px; display: flex; align-items: center; justify-content: center; color: #909399; font-size: 14px; }

.rec-text {
  font-family: inherit;
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.7;
  padding: 12px 16px;
  background: #f8f9fa;
  border-left: 4px solid #67c23a;
  border-radius: 4px;
  margin: 0;
}

.metrics-row { display: flex; gap: 16px; flex-wrap: wrap; margin-top: 8px; }
.metric-card {
  flex: 1;
  min-width: 130px;
  padding: 14px;
  background: #f8f9fa;
  border-radius: 8px;
  text-align: center;
}
.metric-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.metric-value { font-size: 22px; font-weight: 600; color: #303133; }
.metric-value.small { font-size: 14px; }
</style>
