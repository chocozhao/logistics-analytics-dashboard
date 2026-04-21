<script setup>
import { ref, onMounted } from 'vue'
import { useDashboardStore } from './stores/dashboard'
import DashboardHeader from './components/DashboardHeader.vue'
import KPICards from './components/KPICards.vue'
import OrderVolumeChart from './components/OrderVolumeChart.vue'
import DeliveryPerformanceChart from './components/DeliveryPerformanceChart.vue'
import CarrierBreakdownChart from './components/CarrierBreakdownChart.vue'
import NaturalLanguageQuery from './components/NaturalLanguageQuery.vue'

const dashboardStore = useDashboardStore()
const activeTab = ref('dashboard')

onMounted(() => {
  dashboardStore.refreshDashboardData()
})
</script>

<template>
  <div id="app">
    <DashboardHeader />

    <el-main>
      <el-tabs v-model="activeTab" class="dashboard-tabs">
        <el-tab-pane label="仪表板" name="dashboard">
          <div class="dashboard-content">
            <KPICards />

            <div class="charts-grid">
              <el-card class="chart-card">
                <template #header>
                  <h3>订单量趋势</h3>
                </template>
                <OrderVolumeChart />
              </el-card>

              <el-card class="chart-card">
                <template #header>
                  <h3>交货性能</h3>
                </template>
                <DeliveryPerformanceChart />
              </el-card>
            </div>

            <el-card class="full-width-card">
              <template #header>
                <h3>承运商性能细分</h3>
              </template>
              <CarrierBreakdownChart />
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="自然语言查询" name="nlq">
          <NaturalLanguageQuery />
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </div>
</template>

<style>
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.dashboard-tabs {
  margin: 20px;
}

.dashboard-content {
  padding: 20px;
}

.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin: 20px 0;
}

.chart-card {
  height: 400px;
}

.full-width-card {
  margin: 20px 0;
  min-height: 400px;
}

@media (max-width: 1200px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
