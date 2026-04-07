<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '../stores/dashboard'

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
    queryError.value = err.message || 'Failed to process query'
  } finally {
    loadingQuery.value = false
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
      text: 'Query Results Visualization',
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
        name: 'Result',
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
        <h3>Ask a Question About Your Data</h3>
        <p class="subtitle">Ask natural language questions about orders, delivery performance, carriers, and more.</p>
      </template>

      <div class="query-input-section">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="Example: Show me total orders by carrier for the last 30 days"
          :disabled="loadingQuery"
        />
        <div class="query-actions">
          <el-button
            type="primary"
            :loading="loadingQuery"
            :disabled="!question.trim()"
            @click="submitQuery"
          >
            Ask Question
          </el-button>
          <el-button @click="question = ''" :disabled="loadingQuery">
            Clear
          </el-button>
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
        <el-divider content-position="left">Answer</el-divider>
        <div class="answer">
          {{ queryResult.answer }}
        </div>

        <el-divider content-position="left">Visualization</el-divider>
        <div v-if="queryResult.chartData" class="chart-container">
          <div ref="chartRef" class="chart"></div>
        </div>
        <div v-else class="no-chart">
          No visualization available for this query
        </div>

        <el-divider content-position="left">Explanation</el-divider>
        <div class="explanation">
          <p><strong>Filters Applied:</strong> {{ queryResult.explanation?.filters || 'None' }}</p>
          <p><strong>Metrics Used:</strong> {{ queryResult.explanation?.metrics || 'N/A' }}</p>
          <p><strong>Query Plan:</strong> {{ queryResult.explanation?.queryPlan || 'N/A' }}</p>
        </div>

        <el-divider content-position="left">Raw Data</el-divider>
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
            No raw data available
          </div>
        </div>
      </div>

      <div v-else-if="!loadingQuery" class="example-queries">
        <el-divider content-position="left">Example Questions</el-divider>
        <div class="examples">
          <el-tag
            v-for="(example, index) in [
              'Show me total orders by carrier for the last 30 days',
              'What is the on-time delivery rate for UPS vs FedEx?',
              'How many orders were delayed in January?',
              'Display order volume trend by week',
              'Which region has the highest delivery performance?'
            ]"
            :key="index"
            class="example-tag"
            @click="question = example"
          >
            {{ example }}
          </el-tag>
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
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

.chart-container {
  margin: 20px 0;
}

.chart {
  width: 100%;
  height: 400px;
  min-height: 300px;
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
</style>