<script setup>
import { ref } from 'vue'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const title = 'Logistics Analytics Dashboard'
</script>

<template>
  <el-header class="dashboard-header">
    <div class="header-content">
      <div class="header-left">
        <h1>{{ title }}</h1>
        <p class="subtitle">AI-powered insights for freight operations</p>
      </div>
      <div class="header-right">
        <div class="filter-section">
          <el-date-picker
            v-model="dashboardStore.dateRange"
            type="daterange"
            range-separator="to"
            start-placeholder="Start date"
            end-placeholder="End date"
            size="small"
            style="margin-right: 10px;"
          />
          <el-button @click="dashboardStore.resetFilters" size="small">
            Reset Filters
          </el-button>
        </div>
        <div class="status-indicator" v-if="dashboardStore.loading">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <span>Loading data...</span>
        </div>
        <div class="error-indicator" v-if="dashboardStore.error">
          <el-icon class="error-icon"><Warning /></el-icon>
          <span>Error: {{ dashboardStore.error }}</span>
        </div>
      </div>
    </div>
  </el-header>
</template>

<style scoped>
.dashboard-header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: auto;
  min-height: 80px;
  display: flex;
  align-items: center;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  flex-wrap: wrap;
  gap: 15px;
}

.header-left h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.subtitle {
  margin: 5px 0 0 0;
  font-size: 14px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.filter-section {
  display: flex;
  align-items: center;
}

.status-indicator,
.error-indicator {
  display: flex;
  align-items: center;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
}

.status-indicator {
  color: #409eff;
  background-color: #ecf5ff;
}

.error-indicator {
  color: #f56c6c;
  background-color: #fef0f0;
}

.loading-icon,
.error-icon {
  margin-right: 5px;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
  }
}
</style>