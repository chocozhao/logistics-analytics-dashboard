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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private DashboardService dashboardService;

    private final LocalDate startDate = LocalDate.of(2025, 1, 1);
    private final LocalDate endDate   = LocalDate.of(2025, 12, 31);

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(orderRepository);
    }

    @Test
    void testGetKPIs() {
        when(orderRepository.countOrdersByDateRangeWithFilters(startDate, endDate, null, null, null)).thenReturn(1000L);
        when(orderRepository.countDeliveredOrdersByDateRangeWithFilters(startDate, endDate, null, null, null)).thenReturn(800L);
        when(orderRepository.countDelayedOrdersByDateRangeWithFilters(startDate, endDate, null, null, null)).thenReturn(120L);
        when(orderRepository.averageDeliveryDaysByDateRangeWithFilters(eq(startDate), eq(endDate), isNull(), isNull(), isNull())).thenReturn(3.5);

        KpiResponse result = dashboardService.getKPIs(startDate, endDate, null, null, null);

        assertNotNull(result);
        assertEquals(1000L, result.getTotalOrders());
        assertEquals(800L, result.getDeliveredOrders());
        assertEquals(120L, result.getDelayedOrders());
        assertEquals(new BigDecimal("80.00"), result.getOnTimeRate());
        assertEquals(new BigDecimal("3.50"), result.getAvgDeliveryDays());
    }

    @Test
    void testGetKPIs_ZeroOrders() {
        when(orderRepository.countOrdersByDateRangeWithFilters(startDate, endDate, null, null, null)).thenReturn(0L);
        when(orderRepository.countDeliveredOrdersByDateRangeWithFilters(startDate, endDate, null, null, null)).thenReturn(0L);
        when(orderRepository.countDelayedOrdersByDateRangeWithFilters(startDate, endDate, null, null, null)).thenReturn(0L);
        when(orderRepository.averageDeliveryDaysByDateRangeWithFilters(eq(startDate), eq(endDate), isNull(), isNull(), isNull())).thenReturn(null);

        KpiResponse result = dashboardService.getKPIs(startDate, endDate, null, null, null);

        assertNotNull(result);
        assertEquals(0L, result.getTotalOrders());
        assertEquals(BigDecimal.ZERO, result.getOnTimeRate());
        assertEquals(BigDecimal.ZERO, result.getAvgDeliveryDays());
    }

    @Test
    void testGetOrderVolumeTimeSeries() {
        Object[] row1 = { java.sql.Date.valueOf("2025-01-01"), 150L };
        Object[] row2 = { java.sql.Date.valueOf("2025-02-01"), 200L };
        List<Object[]> mockResults = Arrays.asList(row1, row2);

        when(orderRepository.getOrderCountByTimePeriodWithFilters(eq("month"), eq(startDate), eq(endDate), isNull(), isNull(), isNull()))
                .thenReturn(mockResults);

        TimeSeriesResponse result = dashboardService.getOrderVolumeTimeSeries("month", startDate, endDate, null, null, null);

        assertNotNull(result);
        assertEquals("month", result.getGranularity());
        assertEquals(2, result.getData().size());
        assertEquals(150L, result.getData().get(0).getCount());
        assertEquals(200L, result.getData().get(1).getCount());
    }

    @Test
    void testGetCarrierBreakdown() {
        Object[] row1 = { "UPS", 500L, 50L };
        Object[] row2 = { "FedEx", 300L, 45L };

        when(orderRepository.getCarrierBreakdownWithFilters(eq(startDate), eq(endDate), isNull(), isNull(), isNull()))
                .thenReturn(Arrays.asList(row1, row2));

        CarrierBreakdownResponse result = dashboardService.getCarrierBreakdown(startDate, endDate, null, null, null);

        assertNotNull(result);
        assertEquals(2, result.getData().size());
        assertEquals("UPS", result.getData().get(0).getCarrier());
        assertEquals(new BigDecimal("10.00"), result.getData().get(0).getDelayRate());
    }

    @Test
    void testGetDeliveryPerformance() {
        Object[] row1 = { java.sql.Date.valueOf("2025-01-01"), 10L, 2L };
        Object[] row2 = { java.sql.Date.valueOf("2025-02-01"), 12L, 1L };

        when(orderRepository.getDeliveryPerformanceByTimePeriod(eq("month"), eq(startDate), eq(endDate), isNull(), isNull(), isNull()))
                .thenReturn(Arrays.asList(row1, row2));

        DeliveryPerformanceResponse result = dashboardService.getDeliveryPerformance("month", startDate, endDate, null, null, null);

        assertNotNull(result);
        assertEquals("month", result.getGranularity());
        assertEquals(2, result.getData().size());
        assertEquals(10L, result.getData().get(0).getOnTime());
        assertEquals(2L, result.getData().get(0).getDelayed());
    }
}
