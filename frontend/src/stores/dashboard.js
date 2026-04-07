import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

export const useDashboardStore = defineStore('dashboard', () => {
  // State
  const kpis = ref(null)
  const orderVolumeData = ref([])
  const deliveryPerformanceData = ref([])
  const carrierBreakdownData = ref([])
  const forecastData = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // Filters
  const dateRange = ref([new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), new Date()]) // Last 30 days
  const selectedCarriers = ref([])
  const selectedRegions = ref([])

  // Getters
  const startDate = computed(() => {
    return dateRange.value[0] ? formatDate(dateRange.value[0]) : null
  })

  const endDate = computed(() => {
    return dateRange.value[1] ? formatDate(dateRange.value[1]) : null
  })

  const filters = computed(() => {
    return {
      startDate: startDate.value,
      endDate: endDate.value,
      carriers: selectedCarriers.value.length > 0 ? selectedCarriers.value : null,
      regions: selectedRegions.value.length > 0 ? selectedRegions.value : null
    }
  })

  // Actions
  async function fetchKPIs() {
    loading.value = true
    error.value = null
    try {
      const params = buildParams(filters.value)
      const response = await axios.get(`${API_BASE_URL}/dashboard/kpis`, { params })
      kpis.value = response.data
    } catch (err) {
      error.value = err.message
      console.error('Error fetching KPIs:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchOrderVolume(granularity = 'day') {
    loading.value = true
    error.value = null
    try {
      const params = buildParams({ ...filters.value, granularity })
      const response = await axios.get(`${API_BASE_URL}/dashboard/order-volume`, { params })
      orderVolumeData.value = response.data
    } catch (err) {
      error.value = err.message
      console.error('Error fetching order volume:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchDeliveryPerformance(granularity = 'week') {
    loading.value = true
    error.value = null
    try {
      const params = buildParams({ ...filters.value, granularity })
      const response = await axios.get(`${API_BASE_URL}/dashboard/delivery-performance`, { params })
      deliveryPerformanceData.value = response.data
    } catch (err) {
      error.value = err.message
      console.error('Error fetching delivery performance:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchCarrierBreakdown() {
    loading.value = true
    error.value = null
    try {
      const params = buildParams(filters.value)
      const response = await axios.get(`${API_BASE_URL}/dashboard/carrier-breakdown`, { params })
      carrierBreakdownData.value = response.data
    } catch (err) {
      error.value = err.message
      console.error('Error fetching carrier breakdown:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchForecast(granularity = 'week', periods = 4) {
    loading.value = true
    error.value = null
    try {
      const request = {
        granularity,
        periods,
        startDate: startDate.value,
        endDate: endDate.value,
        carriers: filters.value.carriers,
        regions: filters.value.regions
      }
      const response = await axios.post(`${API_BASE_URL}/forecast`, request)
      forecastData.value = response.data
    } catch (err) {
      error.value = err.message
      console.error('Error fetching forecast:', err)
    } finally {
      loading.value = false
    }
  }

  async function submitNaturalLanguageQuery(question) {
    loading.value = true
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
      error.value = err.message
      console.error('Error submitting query:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function resetFilters() {
    dateRange.value = [new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), new Date()]
    selectedCarriers.value = []
    selectedRegions.value = []
  }

  // Helper functions
  function buildParams(filters) {
    const params = new URLSearchParams()
    if (filters.startDate) params.append('startDate', filters.startDate)
    if (filters.endDate) params.append('endDate', filters.endDate)
    if (filters.carriers) {
      filters.carriers.forEach(carrier => params.append('carriers', carrier))
    }
    if (filters.regions) {
      filters.regions.forEach(region => params.append('regions', region))
    }
    if (filters.granularity) params.append('granularity', filters.granularity)
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
    resetFilters
  }
})