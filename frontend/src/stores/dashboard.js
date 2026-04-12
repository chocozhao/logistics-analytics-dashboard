import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

console.log('VITE_API_URL环境变量:', import.meta.env.VITE_API_URL)
console.log('当前环境模式:', import.meta.env.MODE, '是否为生产环境:', import.meta.env.PROD)
// 在生产环境中，如果VITE_API_URL未设置，使用相对路径
// 在开发环境中，使用localhost:8080作为回退
const API_BASE_URL = import.meta.env.VITE_API_URL || (import.meta.env.PROD ? '/api' : 'http://localhost:8080/api')
console.log('API_BASE_URL计算值:', API_BASE_URL)

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

  // Filters
  const dateRange = (() => {
    // Database has data from 2024-01-01 to 2025-03-31
    // Use a default range within this period
    const startDate = new Date('2024-01-01')
    const endDate = new Date('2024-01-31')
    console.log('dateRange initialization:', { startDate, endDate, startDateType: typeof startDate, endDateType: typeof endDate })
    return ref([startDate, endDate])
  })() // Default to January 2024 (within database range)
  const selectedCarriers = ref([])
  const selectedRegions = ref([])

  // Getters
  const startDate = computed(() => {
    const result = dateRange.value[0] ? formatDate(dateRange.value[0]) : null
    console.log('startDate computed:', { raw: dateRange.value[0], formatted: result })
    return result
  })

  const endDate = computed(() => {
    const result = dateRange.value[1] ? formatDate(dateRange.value[1]) : null
    console.log('endDate computed:', { raw: dateRange.value[1], formatted: result })
    return result
  })

  const filters = computed(() => {
    const filtersObj = {
      startDate: startDate.value,
      endDate: endDate.value,
      carriers: selectedCarriers.value.length > 0 ? selectedCarriers.value : null,
      regions: selectedRegions.value.length > 0 ? selectedRegions.value : null
    }
    console.log('filters computed:', filtersObj)
    return filtersObj
  })

  // Actions
  async function fetchKPIs() {
    loadingCount.value++
    error.value = null
    try {
      const params = buildParams(filters.value)
      console.log('fetchKPIs params:', Object.fromEntries(params.entries()))
      console.log('fetchKPIs filters:', filters.value)
      const response = await axios.get(`${API_BASE_URL}/dashboard/kpis`, { params })
      console.log('fetchKPIs response:', response.data)
      kpis.value = response.data
    } catch (err) {
      error.value = '加载KPI数据失败: ' + err.message
      console.error('Error fetching KPIs:', err.response ? err.response.data : err.message, err)
    } finally {
      loadingCount.value--
    }
  }

  async function fetchOrderVolume(granularity = 'day') {
    loadingCount.value++
    error.value = null
    try {
      const params = buildParams({ ...filters.value, granularity })
      const response = await axios.get(`${API_BASE_URL}/dashboard/order-volume`, { params })
      console.log('fetchOrderVolume response:', response.data)
      orderVolumeData.value = response.data.data
    } catch (err) {
      error.value = '加载订单量数据失败: ' + err.message
      console.error('Error fetching order volume:', err.response ? err.response.data : err.message, err)
    } finally {
      loadingCount.value--
    }
  }

  async function fetchDeliveryPerformance(granularity = 'week') {
    loadingCount.value++
    error.value = null
    try {
      const params = buildParams({ ...filters.value, granularity })
      const response = await axios.get(`${API_BASE_URL}/dashboard/delivery-performance`, { params })
      console.log('fetchDeliveryPerformance response:', response.data)
      deliveryPerformanceData.value = response.data.data
    } catch (err) {
      error.value = '加载交货性能数据失败: ' + err.message
      console.error('Error fetching delivery performance:', err.response ? err.response.data : err.message, err)
    } finally {
      loadingCount.value--
    }
  }

  async function fetchCarrierBreakdown() {
    loadingCount.value++
    error.value = null
    try {
      const params = buildParams(filters.value)
      const response = await axios.get(`${API_BASE_URL}/dashboard/carrier-breakdown`, { params })
      console.log('fetchCarrierBreakdown response:', response.data)
      carrierBreakdownData.value = response.data.data
    } catch (err) {
      error.value = '加载承运商细分数据失败: ' + err.message
      console.error('Error fetching carrier breakdown:', err.response ? err.response.data : err.message, err)
    } finally {
      loadingCount.value--
    }
  }

  async function fetchForecast(granularity = 'week', periods = 4, customStartDate = null, customEndDate = null) {
    console.log('fetchForecast called with:', { granularity, periods, customStartDate, customEndDate })
    console.log('fetchForecast - startDate.value:', startDate.value, 'endDate.value:', endDate.value)
    console.log('fetchForecast - filters.value:', filters.value)
    loadingCount.value++
    error.value = null
    try {
      // For forecasting, use database-appropriate date range if not specified
      // Database has data from 2024-01-01 to 2025-03-31
      const forecastStartDate = customStartDate || '2024-10-01' // October 1, 2024 for enough historical data
      const forecastEndDate = customEndDate || '2025-03-31'     // March 31, 2025 (end of database)

      const request = {
        granularity,
        periods,
        startDate: forecastStartDate,
        endDate: forecastEndDate,
        carriers: filters.value.carriers,
        regions: filters.value.regions
      }
      console.log('fetchForecast request:', request)
      const response = await axios.post(`${API_BASE_URL}/forecast`, request)
      console.log('fetchForecast response:', response.data)
      forecastData.value = response.data
    } catch (err) {
      error.value = '加载预测数据失败: ' + err.message
      console.error('Error fetching forecast:', err.response ? err.response.data : err.message, err)
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
        startDate: startDate.value,
        endDate: endDate.value,
        carriers: filters.value.carriers,
        regions: filters.value.regions
      }
      const response = await axios.post(`${API_BASE_URL}/query`, request)
      return response.data
    } catch (err) {
      error.value = '提交自然语言查询失败: ' + err.message
      console.error('Error submitting query:', err)
      throw err
    } finally {
      loadingCount.value--
    }
  }

  async function refreshDashboardData() {
    error.value = null
    try {
      await Promise.all([
        fetchKPIs(),
        fetchOrderVolume(),
        fetchDeliveryPerformance(),
        fetchCarrierBreakdown()
      ])
    } catch (err) {
      error.value = '刷新仪表板数据失败: ' + err.message
      console.error('Error refreshing dashboard data:', err)
    }
  }

  async function resetFilters() {
    // Reset to a reasonable range within database bounds
    dateRange.value = [new Date('2024-01-01'), new Date('2024-03-31')] // 3 months of data
    selectedCarriers.value = []
    selectedRegions.value = []
    // Refresh data after resetting filters
    await refreshDashboardData()
  }

  // Helper functions
  function buildParams(filters) {
    console.log('buildParams filters:', filters)
    const params = new URLSearchParams()
    if (filters.startDate) {
      console.log('startDate raw:', filters.startDate, 'formatted:', formatDate(filters.startDate))
      params.append('startDate', filters.startDate)
    }
    if (filters.endDate) {
      console.log('endDate raw:', filters.endDate, 'formatted:', formatDate(filters.endDate))
      params.append('endDate', filters.endDate)
    }
    if (filters.carriers) {
      filters.carriers.forEach(carrier => params.append('carrier', carrier))
    }
    if (filters.regions) {
      filters.regions.forEach(region => params.append('region', region))
    }
    if (filters.granularity) params.append('granularity', filters.granularity)
    console.log('buildParams result:', Object.fromEntries(params.entries()))
    return params
  }

  function formatDate(date) {
    if (!date) return null
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  return {
    // State
    kpis,
    orderVolumeData,
    deliveryPerformanceData,
    carrierBreakdownData,
    forecastData,
    loading,
    error,
    dateRange,
    selectedCarriers,
    selectedRegions,

    // Getters
    startDate,
    endDate,
    filters,

    // Actions
    fetchKPIs,
    fetchOrderVolume,
    fetchDeliveryPerformance,
    fetchCarrierBreakdown,
    fetchForecast,
    submitNaturalLanguageQuery,
    resetFilters,
    refreshDashboardData
  }
})