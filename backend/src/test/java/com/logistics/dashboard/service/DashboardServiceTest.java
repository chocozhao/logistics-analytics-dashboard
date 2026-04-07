package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private DashboardService dashboardService;

    private final LocalDate startDate = LocalDate.of(2024, 1, 1);
    private final LocalDate endDate = LocalDate.of(2024, 1, 31);

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(orderRepository);
    }

    @Test
    void testGetKPIs() {
        // Arrange
        when(orderRepository.countOrdersByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(1000L);
        when(orderRepository.countDeliveredOrdersByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(800L);
        when(orderRepository.countDelayedOrdersByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(120L);
        when(orderRepository.averageDeliveryDaysByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(3.5);

        // Act
        KpiResponse result = dashboardService.getKPIs(startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1000L, result.getTotalOrders());
        assertEquals(800L, result.getDeliveredOrders());
        assertEquals(120L, result.getDelayedOrders());
        assertEquals(new BigDecimal("80.00"), result.getOnTimeRate()); // 800/1000 = 80%
        assertEquals(new BigDecimal("3.50"), result.getAvgDeliveryDays());
    }

    @Test
    void testGetKPIs_ZeroOrders() {
        // Arrange
        when(orderRepository.countOrdersByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(0L);
        when(orderRepository.countDeliveredOrdersByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(0L);
        when(orderRepository.countDelayedOrdersByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(0L);
        when(orderRepository.averageDeliveryDaysByDateRangeWithFilters(startDate, endDate, null, null)).thenReturn(null);

        // Act
        KpiResponse result = dashboardService.getKPIs(startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotalOrders());
        assertEquals(0L, result.getDeliveredOrders());
        assertEquals(0L, result.getDelayedOrders());
        assertEquals(BigDecimal.ZERO, result.getOnTimeRate());
        assertEquals(BigDecimal.ZERO, result.getAvgDeliveryDays());
    }

    @Test
    void testGetOrderVolumeTimeSeries() {
        // Arrange
        Object[] row1 = new Object[] { java.sql.Date.valueOf("2024-01-01"), 150L };
        Object[] row2 = new Object[] { java.sql.Date.valueOf("2024-01-02"), 200L };
        List<Object[]> mockResults = Arrays.asList(row1, row2);

        when(orderRepository.getOrderCountByTimePeriodWithFilters("day", startDate, endDate, null, null))
                .thenReturn(mockResults);

        // Act
        TimeSeriesResponse result = dashboardService.getOrderVolumeTimeSeries(
                "day", startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("day", result.getGranularity());
        assertEquals(2, result.getData().size());
        assertEquals(LocalDate.of(2024, 1, 1), result.getData().get(0).getDate());
        assertEquals(150L, result.getData().get(0).getCount());
        assertEquals(LocalDate.of(2024, 1, 2), result.getData().get(1).getDate());
        assertEquals(200L, result.getData().get(1).getCount());
    }

    @Test
    void testGetCarrierBreakdown() {
        // Arrange
        Object[] row1 = new Object[] { "UPS", 500L, 50L };
        Object[] row2 = new Object[] { "FedEx", 300L, 45L };
        List<Object[]> mockResults = Arrays.asList(row1, row2);

        when(orderRepository.getCarrierBreakdownWithFilters(startDate, endDate, null, null))
                .thenReturn(mockResults);

        // Act
        CarrierBreakdownResponse result = dashboardService.getCarrierBreakdown(
                startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getData().size());

        CarrierBreakdown ups = result.getData().get(0);
        assertEquals("UPS", ups.getCarrier());
        assertEquals(500L, ups.getTotalOrders());
        assertEquals(50L, ups.getDelayedOrders());
        assertEquals(new BigDecimal("10.00"), ups.getDelayRate()); // 50/500 = 10%

        CarrierBreakdown fedex = result.getData().get(1);
        assertEquals("FedEx", fedex.getCarrier());
        assertEquals(300L, fedex.getTotalOrders());
        assertEquals(45L, fedex.getDelayedOrders());
        assertEquals(new BigDecimal("15.00"), fedex.getDelayRate()); // 45/300 = 15%
    }

    @Test
    void testGetDeliveryPerformance() {
        // Arrange
        Object[] row1 = new Object[] { java.sql.Date.valueOf("2024-01-01"), 10L, 2L };
        Object[] row2 = new Object[] { java.sql.Date.valueOf("2024-01-08"), 12L, 1L };
        List<Object[]> mockResults = Arrays.asList(row1, row2);

        when(orderRepository.getDeliveryPerformanceByTimePeriod("week", startDate, endDate, null, null))
                .thenReturn(mockResults);

        // Act
        DeliveryPerformanceResponse result = dashboardService.getDeliveryPerformance(
                "week", startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("week", result.getGranularity());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() > 0);
    }
}