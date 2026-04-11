<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'
import { Refresh, Delete } from '@element-plus/icons-vue'

const dashboardStore = useDashboardStore()
const question = ref('')
const chartRef = ref(null)
let chartInstance = null

const queryResult = ref(null)
const loadingQuery = ref(false)
const queryError = ref(null)

const submitQuery = async () => {
  if (!question.value.trim()) return

  loadingQuery.value = true
  queryError.value = null
  queryResult.value = null

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  try {
    const result = await dashboardStore.submitNaturalLanguageQuery(question.value)
    queryResult.value = result
    if (result.chartData) {
      setTimeout(() => {
        renderChart(result.chartData)
      }, 100)
    }
  } catch (err) {
    queryError.value = err.message || '处理查询失败'
  } finally {
    loadingQuery.value = false
  }
}

const startNewQuery = () => {
  question.value = ''
  queryResult.value = null
  queryError.value = null
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

const clearQuery = () => {
  question.value = ''
  queryResult.value = null
  queryError.value = null
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

const formatFilters = (filters) => {
  if (!filters) return null
  if (typeof filters === 'string') return filters
  try {
    const entries = Object.entries(filters)
    if (entries.length === 0) return null
    return entries
      .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join(', ') : value}`)
      .join('; ')
  } catch (e) {
    console.warn('格式化筛选条件失败:', e)
    return JSON.stringify(filters)
  }
}

const renderChart = (chartData) => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  // Basic chart configuration based on chartData type
  // This is a simplified version - in a real app you'd parse the chartData
  // and create appropriate chart based on the data structure
  const option = {
    title: {
      text: '查询结果可视化',
      left: 'center',
      top: 10
    },
    tooltip: {
      trigger: 'axis'
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
      data: chartData.labels || []
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '结果',
        type: chartData.chartType === 'bar' ? 'bar' : 'line',
        data: chartData.values || [],
        itemStyle: {
          color: '#409eff'
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
  window.addEventListener('resize', resizeChart)
})

watch(() => dashboardStore.filters, () => {
  // Clear results when filters change
  queryResult.value = null
  queryError.value = null
  question.value = ''
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}, { deep: true })
</script>

<template>
  <div class="natural-language-query">
    <el-card class="query-card">
      <template #header>
        <h3>数据自然语言查询</h3>
        <p class="subtitle">使用自然语言提问关于订单、交货性能、承运商等问题。</p>
      </template>

      <div class="query-input-section">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="例如：显示过去30天各承运商的总订单数"
          :disabled="loadingQuery"
        />
        <div class="query-actions">
          <el-button
            type="primary"
            :loading="loadingQuery"
            :disabled="!question.trim()"
            @click="submitQuery"
          >
            提问
          </el-button>
          <el-button @click="question = ''" :disabled="loadingQuery">
            清除
          </el-button>
        </div>

        <div v-if="!loadingQuery" class="example-queries">
          <el-divider content-position="left">示例问题</el-divider>
          <div class="examples">
            <el-tag
              v-for="(example, index) in [
                '显示过去30天各承运商的总订单数',
                'UPS和FedEx的准时交货率是多少？',
                '一月份有多少订单延迟？',
                '按周显示订单量趋势',
                '哪个区域的交货性能最好？'
              ]"
              :key="index"
              class="example-tag"
              @click="question = example"
            >
              {{ example }}
            </el-tag>
          </div>
        </div>
      </div>

      <div v-if="queryError" class="error-message">
        <el-alert
          :title="queryError"
          type="error"
          show-icon
          :closable="false"
        />
      </div>

      <div v-if="queryResult" class="result-section">
        <div class="result-actions">
          <el-button type="primary" plain @click="startNewQuery">
            <el-icon><Refresh /></el-icon>
            新查询
          </el-button>
          <el-button @click="clearQuery">
            <el-icon><Delete /></el-icon>
            清除结果
          </el-button>
        </div>
        <el-divider content-position="left">答案</el-divider>
        <div class="answer">
          <div v-if="typeof queryResult.answer === 'string' && queryResult.answer.includes('\\n')" class="formatted-answer">
            <template v-for="(line, index) in queryResult.answer.split('\\n')" :key="index">
              <p v-if="line.trim()" class="answer-line">{{ line.trim() }}</p>
              <br v-else />
            </template>
          </div>
          <div v-else class="simple-answer">
            {{ queryResult.answer }}
          </div>
          <div v-if="queryResult.summary" class="answer-summary">
            <el-divider content-position="left">摘要</el-divider>
            <div class="summary-content">{{ queryResult.summary }}</div>
          </div>
        </div>

        <el-divider content-position="left">可视化</el-divider>
        <div v-if="queryResult.chartData" class="chart-container">
          <div v-if="queryResult.chartType === 'none' || queryResult.chartData.chartType === 'none'" class="no-chart-message">
            <el-alert
              :title="queryResult.chartData.message || '此问题无可视化数据'"
              type="info"
              show-icon
              :closable="false"
            />
          </div>
          <div v-else>
            <div ref="chartRef" class="chart"></div>
            <!-- Display KPI summary if available -->
            <div v-if="queryResult.chartData && queryResult.chartData.kpiSummary" class="kpi-summary">
              <el-divider content-position="left">关键指标摘要</el-divider>
              <div class="kpi-metrics">
                <div v-for="(value, key) in queryResult.chartData.kpiSummary" :key="key" class="kpi-metric">
                  <span class="kpi-label">{{ key === 'onTimeRate' ? '准时交付率' : key === 'avgDeliveryDays' ? '平均交付天数' : key }}:</span>
                  <span class="kpi-value">{{ value }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="no-chart">
          此查询无可视化数据
        </div>

        <el-divider content-position="left">查询详情</el-divider>
        <div class="explanation">
          <p><strong>AI解释:</strong> {{ queryResult.explanation || '系统正在分析您的问题...' }}</p>
          <p><strong>应用筛选条件:</strong> {{ formatFilters(queryResult.filters) || '无筛选条件' }}</p>
          <p><strong>使用指标:</strong> {{
            Array.isArray(queryResult.metrics) ?
            queryResult.metrics.join(', ') :
            queryResult.metrics || '通用指标'
          }}</p>
          <p><strong>查询计划:</strong> {{ queryResult.queryPlan || '标准查询处理流程' }}</p>
        </div>

        <el-divider content-position="left">原始数据</el-divider>
        <div class="raw-data">
          <el-table
            :data="queryResult.rawData"
            border
            style="width: 100%"
            max-height="300"
            v-if="queryResult.rawData && queryResult.rawData.length > 0"
          >
            <el-table-column
              v-for="(value, key) in queryResult.rawData[0]"
              :key="key"
              :prop="key"
              :label="key"
              width="180"
            />
          </el-table>
          <div v-else class="no-raw-data">
            无原始数据
          </div>
        </div>
      </div>

    </el-card>
  </div>
</template>

<style scoped>
.natural-language-query {
  padding: 20px;
}

.query-card {
  max-width: 1200px;
  margin: 0 auto;
}

.subtitle {
  margin: 10px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.query-input-section {
  margin-bottom: 20px;
}

.query-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.error-message {
  margin-bottom: 20px;
}

.result-section {
  margin-top: 30px;
}

.answer {
  font-size: 16px;
  line-height: 1.6;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.formatted-answer {
  line-height: 1.8;
}

.answer-line {
  margin: 8px 0;
  position: relative;
  padding-left: 20px;
}

.answer-line:before {
  content: "•";
  position: absolute;
  left: 0;
  color: #409eff;
  font-weight: bold;
}

.answer-summary {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #e0e0e0;
}

.summary-content {
  font-size: 15px;
  color: #333;
  background-color: #fff;
  padding: 15px;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.chart-container {
  margin: 20px 0;
}

.chart {
  width: 100%;
  height: 400px;
  min-height: 300px;
}

.no-chart-message {
  text-align: center;
  padding: 40px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border: 1px dashed #b3d8ff;
}

.no-chart {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-style: italic;
}

.explanation {
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.explanation p {
  margin: 8px 0;
}

.raw-data {
  margin: 20px 0;
}

.no-raw-data {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-style: italic;
}

.example-queries {
  margin-top: 30px;
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.example-tag {
  cursor: pointer;
  max-width: 300px;
  white-space: normal;
  height: auto;
  padding: 8px 12px;
  line-height: 1.4;
}

.example-tag:hover {
  background-color: #ecf5ff;
}

.kpi-summary {
  margin-top: 30px;
  padding: 20px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #b3d8ff;
}

.kpi-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.kpi-metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: white;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.kpi-label {
  font-weight: 600;
  color: #333;
}

.kpi-value {
  font-weight: 700;
  font-size: 16px;
  color: #409eff;
}
</style>