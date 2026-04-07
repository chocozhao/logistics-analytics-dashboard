<script setup>
import { computed } from 'vue'
import { useDashboardStore } from '../stores/dashboard'

const dashboardStore = useDashboardStore()

const kpis = computed(() => dashboardStore.kpis)

const kpiCards = computed(() => [
  {
    title: 'Total Orders',
    value: kpis.value?.totalOrders || 0,
    icon: 'Document',
    color: '#409eff',
    trend: null,
    description: 'Total orders in selected period'
  },
  {
    title: 'Delivered Orders',
    value: kpis.value?.deliveredOrders || 0,
    icon: 'Check',
    color: '#67c23a',
    trend: null,
    description: 'Successfully delivered orders'
  },
  {
    title: 'Delayed Orders',
    value: kpis.value?.delayedOrders || 0,
    icon: 'Clock',
    color: '#e6a23c',
    trend: null,
    description: 'Orders delivered after promised date'
  },
  {
    title: 'On-Time Rate',
    value: kpis.value?.onTimeRate ? `${kpis.value.onTimeRate}%` : '0%',
    icon: 'SuccessFilled',
    color: '#67c23a',
    trend: null,
    description: 'Percentage of orders delivered on time'
  },
  {
    title: 'Avg Delivery Days',
    value: kpis.value?.avgDeliveryDays ? kpis.value.avgDeliveryDays.toFixed(1) : '0.0',
    icon: 'Timer',
    color: '#909399',
    trend: null,
    description: 'Average days from order to delivery'
  }
])
</script>

<template>
  <div class="kpi-cards">
    <div v-for="(kpi, index) in kpiCards" :key="index" class="kpi-card">
      <el-card shadow="hover" class="card">
        <div class="card-content">
          <div class="card-icon" :style="{ backgroundColor: kpi.color + '20' }">
            <el-icon :color="kpi.color" size="24">
              <component :is="kpi.icon" />
            </el-icon>
          </div>
          <div class="card-text">
            <div class="card-value">{{ kpi.value }}</div>
            <div class="card-title">{{ kpi.title }}</div>
            <div class="card-description">{{ kpi.description }}</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.kpi-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.kpi-card .card {
  height: 100%;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.card-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-text {
  flex: 1;
  min-width: 0;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.card-title {
  font-size: 14px;
  color: #606266;
  margin-top: 4px;
}

.card-description {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

@media (max-width: 768px) {
  .kpi-cards {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}

@media (max-width: 480px) {
  .kpi-cards {
    grid-template-columns: 1fr;
  }
}
</style>