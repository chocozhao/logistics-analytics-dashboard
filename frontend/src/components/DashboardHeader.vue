<script setup>
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()
const title = '物流分析仪表板'

const minDate = new Date('2025-01-01')
const maxDate = new Date('2025-12-31')

const disabledDate = (time) =>
  time.getTime() < minDate.getTime() || time.getTime() > maxDate.getTime()
</script>

<template>
  <el-header class="dashboard-header">
    <div class="header-content">
      <div class="header-left">
        <h1>{{ title }}</h1>
        <p class="subtitle">AI 驱动的货运运营洞察</p>
      </div>

      <div class="header-right">
        <!-- Date range -->
        <el-date-picker
          v-model="dashboardStore.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
          :disabled-date="disabledDate"
          style="width:260px"
        />

        <!-- Carrier filter -->
        <el-select
          v-model="dashboardStore.selectedCarriers"
          multiple
          collapse-tags
          placeholder="承运商"
          size="small"
          style="width:160px"
          clearable
        >
          <el-option v-for="c in dashboardStore.availableCarriers" :key="c" :label="c" :value="c" />
        </el-select>

        <!-- Region filter -->
        <el-select
          v-model="dashboardStore.selectedRegions"
          multiple
          collapse-tags
          placeholder="地区"
          size="small"
          style="width:130px"
          clearable
        >
          <el-option v-for="r in dashboardStore.availableRegions" :key="r" :label="r" :value="r" />
        </el-select>

        <!-- Category filter -->
        <el-select
          v-model="dashboardStore.selectedCategories"
          multiple
          collapse-tags
          placeholder="品类"
          size="small"
          style="width:130px"
          clearable
        >
          <el-option v-for="c in dashboardStore.availableCategories" :key="c" :label="c" :value="c" />
        </el-select>

        <el-button type="primary" size="small" @click="dashboardStore.refreshDashboardData">
          应用筛选
        </el-button>
        <el-button size="small" @click="dashboardStore.resetFilters">重置</el-button>

        <!-- Status -->
        <span v-if="dashboardStore.loading" class="status loading">正在加载...</span>
        <span v-if="dashboardStore.error" class="status error">{{ dashboardStore.error }}</span>
      </div>
    </div>
  </el-header>
</template>

<style scoped>
.dashboard-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: auto;
  min-height: 72px;
  display: flex;
  align-items: center;
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 0;
}
.header-left h1 { margin: 0; font-size: 22px; font-weight: 600; color: #303133; }
.subtitle { margin: 4px 0 0; font-size: 13px; color: #909399; }
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.status { font-size: 12px; padding: 4px 8px; border-radius: 4px; }
.status.loading { color: #409eff; background: #ecf5ff; }
.status.error   { color: #f56c6c; background: #fef0f0; }

@media (max-width: 900px) {
  .header-content { flex-direction: column; align-items: flex-start; }
  .header-right { width: 100%; }
}
</style>
