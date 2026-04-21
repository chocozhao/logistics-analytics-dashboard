<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'
import { Refresh, Delete } from '@element-plus/icons-vue'

const dashboardStore = useDashboardStore()
const question = ref('')
const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const queryResult = ref(null)
const loadingQuery = ref(false)
const queryError = ref(null)

const EXAMPLE_QUERIES = [
  '2025年有多少UPS的订单延误了？',
  '按月显示2025年订单量趋势',
  '各承运商延误率对比',
  '预测未来4周的订单量',
  '2025年Q1 FedEx在EU地区的准时率',
  '哪个品类的订单延误最多？',
]

// ── Submit query ──────────────────────────────────────────────────────────────

const submitQuery = async () => {
  if (!question.value.trim()) return
  loadingQuery.value = true
  queryError.value = null
  queryResult.value = null
  destroyChart()

  try {
    const result = await dashboardStore.submitNaturalLanguageQuery(question.value)
    queryResult.value = result
    if (result.chartData && result.chartType !== 'none') {
      await nextTick()
      renderChart(result.chartData, result.chartType)
    }
  } catch (err) {
    queryError.value = err.message || '处理查询失败'
  } finally {
    loadingQuery.value = false
  }
}

const clearQuery = () => {
  question.value = ''
  queryResult.value = null
  queryError.value = null
  destroyChart()
}

// ── Chart rendering ───────────────────────────────────────────────────────────

const destroyChart = () => {
  if (resizeObserver && chartRef.value) {
    resizeObserver.unobserve(chartRef.value)
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

const renderChart = (chartData, chartType) => {
  if (!chartRef.value) return
  destroyChart()

  chartInstance = echarts.init(chartRef.value)

  let option = {}

  if (chartType === 'forecast' || chartData.chartType === 'forecast') {
    option = buildForecastChart(chartData)
  } else if (chartData.chartType === 'bar' && chartData.delayedValues) {
    option = buildStackedBarChart(chartData)
  } else if (chartData.chartType === 'bar') {
    option = buildBarChart(chartData)
  } else {
    option = buildLineChart(chartData)
  }

  chartInstance.setOption(option)

  // Cross-browser-safe resize via ResizeObserver
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      chartInstance && chartInstance.resize()
    })
    resizeObserver.observe(chartRef.value)
  } else {
    // Fallback for older browsers
    window.addEventListener('resize', () => chartInstance && chartInstance.resize())
  }
}

const buildLineChart = (chartData) => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, top: 40, bottom: 60, containLabel: true },
  xAxis: {
    type: 'category',
    data: chartData.labels || [],
    axisLabel: { rotate: 30, fontSize: 11 }
  },
  yAxis: { type: 'value', name: '订单数' },
  series: [{
    name: '订单量',
    type: 'line',
    data: chartData.values || [],
    smooth: true,
    itemStyle: { color: '#409eff' },
    areaStyle: { opacity: 0.1 }
  }]
})

const buildBarChart = (chartData) => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, top: 40, bottom: 80, containLabel: true },
  xAxis: {
    type: 'category',
    data: chartData.labels || [],
    axisLabel: { rotate: 30, fontSize: 11 }
  },
  yAxis: { type: 'value', name: '订单数' },
  series: [{
    name: '订单量',
    type: 'bar',
    data: chartData.values || [],
    itemStyle: { color: '#409eff' }
  }]
})

const buildStackedBarChart = (chartData) => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: ['准时', '延误'] },
  grid: { left: 60, right: 20, top: 50, bottom: 80, containLabel: true },
  xAxis: {
    type: 'category',
    data: chartData.labels || [],
    axisLabel: { rotate: 30, fontSize: 11 }
  },
  yAxis: { type: 'value', name: '订单数' },
  series: [
    {
      name: '准时',
      type: 'bar',
      stack: 'total',
      data: chartData.values || [],
      itemStyle: { color: '#67c23a' }
    },
    {
      name: '延误',
      type: 'bar',
      stack: 'total',
      data: chartData.delayedValues || [],
      itemStyle: { color: '#f56c6c' }
    }
  ]
})

const buildForecastChart = (chartData) => {
  const splitIdx = chartData.splitIndex || 0
  const allLabels = chartData.labels || []
  const histVals  = chartData.historicalValues || []
  const foreVals  = chartData.forecastValues   || []

  // Build a combined series: historical solid, forecast dashed
  const combined = [
    ...histVals.map(v => v),
    ...foreVals.map(v => v)
  ]

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['历史数据', '预测数据'] },
    grid: { left: 60, right: 20, top: 50, bottom: 80, containLabel: true },
    xAxis: {
      type: 'category',
      data: allLabels,
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: { type: 'value', name: '订单数' },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: [...histVals, ...Array(foreVals.length).fill(null)],
        smooth: true,
        itemStyle: { color: '#409eff' },
        lineStyle: { width: 2 },
      },
      {
        name: '预测数据',
        type: 'line',
        data: [...Array(histVals.length).fill(null), ...foreVals],
        smooth: true,
        itemStyle: { color: '#e6a23c' },
        lineStyle: { type: 'dashed', width: 2 },
        symbol: 'circle',
      }
    ],
    ...(splitIdx > 0 && {
      markLine: {
        silent: true,
        data: [{ xAxis: allLabels[splitIdx - 1] }],
        lineStyle: { type: 'dashed', color: '#999' },
        label: { formatter: '历史/预测分界' }
      }
    })
  }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const formatFilters = (filters) => {
  if (!filters) return null
  try {
    return Object.entries(filters)
      .filter(([, v]) => v != null && !(Array.isArray(v) && v.length === 0))
      .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
      .join(' | ')
  } catch { return null }
}

const hasChart = (result) =>
  result && result.chartData && result.chartType !== 'none' && result.chartData.chartType !== 'none'

const buildCarrierTable = (chartData) => {
  const labels  = chartData.labels        || []
  const values  = chartData.values        || []
  const delayed = chartData.delayedValues || []
  const rates   = chartData.delayRates    || []
  return labels.map((l, i) => ({
    carrier: l, total: values[i] ?? '', delayed: delayed[i] ?? '', rate: rates[i] ?? '',
  }))
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────

onBeforeUnmount(() => {
  destroyChart()
})

watch(() => dashboardStore.filters, () => {
  clearQuery()
}, { deep: true })
</script>

<template>
  <div class="nlq-wrapper">
    <el-card>
      <template #header>
        <div>
          <h3 style="margin:0">自然语言查询</h3>
          <p style="margin:6px 0 0;color:#909399;font-size:13px">
            用自然语言提问，系统自动选择分析工具并返回真实数据。
          </p>
        </div>
      </template>

      <!-- Input -->
      <el-input
        v-model="question"
        type="textarea"
        :rows="3"
        placeholder="例如：2025年有多少UPS的订单延误了？"
        :disabled="loadingQuery"
        @keydown.ctrl.enter="submitQuery"
      />
      <div class="actions">
        <el-button type="primary" :loading="loadingQuery" :disabled="!question.trim()" @click="submitQuery">
          提问
        </el-button>
        <el-button :disabled="loadingQuery" @click="clearQuery">清除</el-button>
      </div>

      <!-- Example queries -->
      <div v-if="!loadingQuery" class="examples-section">
        <el-divider content-position="left">示例问题</el-divider>
        <div class="examples">
          <el-tag
            v-for="(ex, i) in EXAMPLE_QUERIES"
            :key="i"
            class="example-tag"
            @click="question = ex"
          >{{ ex }}</el-tag>
        </div>
      </div>

      <!-- Error -->
      <el-alert v-if="queryError" :title="queryError" type="error" show-icon :closable="false" style="margin-top:16px" />

      <!-- Result -->
      <div v-if="queryResult" class="result">
        <div class="result-actions">
          <el-button type="primary" plain size="small" @click="clearQuery">
            <el-icon><Refresh /></el-icon> 新查询
          </el-button>
        </div>

        <!-- Answer -->
        <el-divider content-position="left">答案</el-divider>
        <div class="answer-box">
          <template v-for="(line, i) in (queryResult.answer || '').split('\n')" :key="i">
            <p v-if="line.trim()" class="answer-line">{{ line.trim() }}</p>
          </template>
        </div>

        <!-- Chart -->
        <el-divider content-position="left">可视化</el-divider>
        <div v-if="hasChart(queryResult)">
          <div ref="chartRef" class="chart-area" />
          <!-- Forecast recommendations -->
          <div v-if="queryResult.chartData.recommendations" class="recommendations">
            <el-alert type="info" :closable="false">
              <template #default>
                <pre class="rec-text">{{ queryResult.chartData.recommendations }}</pre>
              </template>
            </el-alert>
          </div>
          <!-- Carrier delay rates table -->
          <div v-if="queryResult.chartData.delayRates" class="delay-rates">
            <el-table :data="buildCarrierTable(queryResult.chartData)" size="small" border style="margin-top:12px">
              <el-table-column prop="carrier" label="承运商" />
              <el-table-column prop="total" label="总订单" />
              <el-table-column prop="delayed" label="延误订单" />
              <el-table-column prop="rate" label="延误率" />
            </el-table>
          </div>
          <!-- KPI summary -->
          <div v-if="queryResult.chartData.kpiSummary" class="kpi-row">
            <div v-for="(val, key) in queryResult.chartData.kpiSummary" :key="key" class="kpi-card">
              <div class="kpi-label">{{ key === 'onTimeRate' ? '准时率' : '平均送达天数' }}</div>
              <div class="kpi-value">{{ val }}</div>
            </div>
          </div>
        </div>
        <el-alert v-else-if="queryResult.chartType === 'none'" title="此问题无可视化数据" type="info" show-icon :closable="false" />

        <!-- Query detail -->
        <el-divider content-position="left">查询详情</el-divider>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="解释">{{ queryResult.explanation }}</el-descriptions-item>
          <el-descriptions-item label="筛选条件">{{ formatFilters(queryResult.filters) || '无' }}</el-descriptions-item>
          <el-descriptions-item label="指标">{{ Array.isArray(queryResult.metrics) ? queryResult.metrics.join(', ') : queryResult.metrics }}</el-descriptions-item>
          <el-descriptions-item label="查询计划">{{ queryResult.queryPlan }}</el-descriptions-item>
        </el-descriptions>

        <!-- Raw data -->
        <el-divider content-position="left">原始数据</el-divider>
        <el-table
          v-if="queryResult.rawData && queryResult.rawData.length"
          :data="queryResult.rawData"
          border
          size="small"
          max-height="260"
          style="width:100%"
        >
          <el-table-column
            v-for="key in Object.keys(queryResult.rawData[0])"
            :key="key"
            :prop="key"
            :label="key"
            min-width="120"
          />
        </el-table>
        <div v-else style="color:#999;padding:12px;text-align:center">无原始数据</div>
      </div>
    </el-card>
  </div>
</template>


<style scoped>
.nlq-wrapper { padding: 20px; }

.actions { display: flex; gap: 8px; margin-top: 12px; }

.examples-section { margin-top: 20px; }
.examples { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.example-tag {
  cursor: pointer;
  white-space: normal;
  height: auto;
  padding: 6px 10px;
  line-height: 1.4;
  max-width: 320px;
}
.example-tag:hover { background: #ecf5ff; }

.result { margin-top: 24px; }
.result-actions { margin-bottom: 12px; }

.answer-box {
  padding: 16px 20px;
  background: #f8f9fa;
  border-left: 4px solid #409eff;
  border-radius: 4px;
  line-height: 1.7;
}
.answer-line { margin: 6px 0; }

.chart-area { width: 100%; height: 380px; }

.recommendations { margin-top: 12px; }
.rec-text { margin: 0; font-family: inherit; white-space: pre-wrap; font-size: 13px; }

.delay-rates { margin-top: 8px; }

.kpi-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 16px;
}
.kpi-card {
  flex: 1;
  min-width: 140px;
  padding: 14px 18px;
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 8px;
  text-align: center;
}
.kpi-label { font-size: 13px; color: #606266; margin-bottom: 4px; }
.kpi-value { font-size: 22px; font-weight: 700; color: #409eff; }
</style>
