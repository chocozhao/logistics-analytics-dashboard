import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL ||
  (import.meta.env.PROD ? '/api' : 'http://localhost:8080/api')

export const useDashboardStore = defineStore('dashboard', () => {
  // State
  const kpis = ref(null)
  const orderVolumeData = ref([])
  const deliveryPerformanceData = ref([])
  const carrierBreakdownData = ref([])
  const forecastData = ref(null)
  const loadingCount = ref(0)
  const loading = computed(() => loadingCount.value > 0)
  const error = ref(null)

  // Filters — data lives in 2025
  const dateRange = ref([new Date('2025-01-01'), new Date('2025-12-31')])
  const selectedCarriers = ref([])
  const selectedRegions = ref([])
  const selectedCategories = ref([])

  // Available filter options (match new schema)
  const availableCarriers = ['UPS', 'FedEx', 'DHL', 'USPS', 'LaserShip', 'OnTrac', 'DPD', 'GLS', 'Royal Mail']
  const availableRegions  = ['US-W', 'US-C', 'US-E', 'EU', 'UK']
  const availableCategories = ['BOOK', 'PAPER', 'PENCIL', 'CRAYON', 'MARKER', 'BRUSH', 'PAINT', 'STICKER']

  // Computed
  const startDate = computed(() => dateRange.value[0] ? formatDate(dateRange.value[0]) : null)
  const endDate   = computed(() => dateRange.value[1] ? formatDate(dateRange.value[1]) : null)

  const filters = computed(() => ({
    startDate:  startDate.value,
    endDate:    endDate.value,
    carriers:   selectedCarriers.value.length   > 0 ? selectedCarriers.value   : null,
    regions:    selectedRegions.value.length    > 0 ? selectedRegions.value    : null,
    categories: selectedCategories.value.length > 0 ? selectedCategories.value : null,
  }))

  // Actions
  async function fetchKPIs() {
    loadingCount.value++
    error.value = null
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboard/kpis`, { params: buildParams(filters.value) })
      kpis.value = response.data
    } catch (err) {
      error.value = '加载KPI数据失败: ' + err.message
    } finally {
      loadingCount.value--
    }
  }

  async function fetchOrderVolume(granularity = 'month') {
    loadingCount.value++
    error.value = null
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboard/order-volume`,
        { params: buildParams({ ...filters.value, granularity }) })
      orderVolumeData.value = response.data.data
    } catch (err) {
      error.value = '加载订单量数据失败: ' + err.message
    } finally {
      loadingCount.value--
    }
  }

  async function fetchDeliveryPerformance(granularity = 'month') {
    loadingCount.value++
    error.value = null
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboard/delivery-performance`,
        { params: buildParams({ ...filters.value, granularity }) })
      deliveryPerformanceData.value = response.data.data
    } catch (err) {
      error.value = '加载交货性能数据失败: ' + err.message
    } finally {
      loadingCount.value--
    }
  }

  async function fetchCarrierBreakdown() {
    loadingCount.value++
    error.value = null
    try {
      const response = await axios.get(`${API_BASE_URL}/dashboard/carrier-breakdown`,
        { params: buildParams(filters.value) })
      carrierBreakdownData.value = response.data.data
    } catch (err) {
      error.value = '加载承运商细分数据失败: ' + err.message
    } finally {
      loadingCount.value--
    }
  }

  async function fetchForecast(granularity = 'week', periods = 4) {
    loadingCount.value++
    error.value = null
    try {
      const request = {
        granularity,
        periods,
        startDate: '2025-01-01',
        endDate:   '2025-12-31',
        carriers:   filters.value.carriers,
        regions:    filters.value.regions,
        categories: filters.value.categories,
      }
      const response = await axios.post(`${API_BASE_URL}/forecast`, request)
      forecastData.value = response.data
    } catch (err) {
      error.value = '加载预测数据失败: ' + err.message
    } finally {
      loadingCount.value--
    }
  }

  async function submitNaturalLanguageQuery(question) {
    loadingCount.value++
    error.value = null
    try {
      const request = {
        question,
        startDate:  startDate.value,
        endDate:    endDate.value,
        carriers:   filters.value.carriers,
        regions:    filters.value.regions,
        categories: filters.value.categories,
      }
      const response = await axios.post(`${API_BASE_URL}/query`, request)
      return response.data
    } catch (err) {
      error.value = '提交查询失败: ' + err.message
      throw err
    } finally {
      loadingCount.value--
    }
  }

  async function refreshDashboardData() {
    error.value = null
    await Promise.all([
      fetchKPIs(),
      fetchOrderVolume(),
      fetchDeliveryPerformance(),
      fetchCarrierBreakdown(),
    ])
  }

  async function resetFilters() {
    dateRange.value = [new Date('2025-01-01'), new Date('2025-12-31')]
    selectedCarriers.value   = []
    selectedRegions.value    = []
    selectedCategories.value = []
    await refreshDashboardData()
  }

  // Helpers
  function buildParams(f) {
    const params = new URLSearchParams()
    if (f.startDate)   params.append('startDate', f.startDate)
    if (f.endDate)     params.append('endDate',   f.endDate)
    if (f.carriers)    f.carriers.forEach(c   => params.append('carrier',  c))
    if (f.regions)     f.regions.forEach(r    => params.append('region',   r))
    if (f.categories)  f.categories.forEach(c => params.append('category', c))
    if (f.granularity) params.append('granularity', f.granularity)
    return params
  }

  function formatDate(date) {
    if (!date) return null
    const d = new Date(date)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
  }

  return {
    kpis, orderVolumeData, deliveryPerformanceData, carrierBreakdownData, forecastData,
    loading, error,
    dateRange, selectedCarriers, selectedRegions, selectedCategories,
    availableCarriers, availableRegions, availableCategories,
    startDate, endDate, filters,
    fetchKPIs, fetchOrderVolume, fetchDeliveryPerformance, fetchCarrierBreakdown,
    fetchForecast, submitNaturalLanguageQuery, resetFilters, refreshDashboardData,
  }
})
