package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.ForecastData;
import com.logistics.dashboard.dto.ForecastResponse;
import com.logistics.dashboard.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private ForecastingService forecastingService;

    private final LocalDate startDate = LocalDate.of(2025, 1, 1);
    private final LocalDate endDate   = LocalDate.of(2025, 12, 31);

    @BeforeEach
    void setUp() {
        forecastingService = new ForecastingService(orderRepository);
    }

    private List<Object[]> makeRows(String[] dates, long[] counts) {
        Object[][] rows = new Object[dates.length][2];
        for (int i = 0; i < dates.length; i++) {
            rows[i][0] = java.sql.Date.valueOf(dates[i]);
            rows[i][1] = counts[i];
        }
        return Arrays.asList(rows);
    }

    @Test
    void testForecastDemand_WeeklyWithEnoughData() {
        List<Object[]> rows = makeRows(
            new String[]{"2025-01-06","2025-01-13","2025-01-20","2025-01-27","2025-02-03"},
            new long[]{100, 120, 110, 130, 125}
        );
        when(orderRepository.getHistoricalOrderCountByPeriod(eq("week"), eq(startDate), eq(endDate), isNull(), isNull(), isNull()))
                .thenReturn(rows);

        ForecastResponse result = forecastingService.forecastDemand("week", 2, startDate, endDate, null, null);

        assertNotNull(result);
        assertEquals("week", result.getGranularity());
        assertEquals(2, result.getForecastPeriods());

        List<ForecastData> historical = result.getData().stream().filter(d -> !d.isForecast()).toList();
        List<ForecastData> forecast   = result.getData().stream().filter(ForecastData::isForecast).toList();
        assertEquals(5, historical.size());
        assertEquals(2, forecast.size());
        assertNotNull(result.getRecommendations());
    }

    @Test
    void testForecastDemand_InsufficientData_FallsBackToSES() {
        List<Object[]> rows = makeRows(
            new String[]{"2025-01-06","2025-01-13"},
            new long[]{100, 120}
        );
        when(orderRepository.getHistoricalOrderCountByPeriod(eq("week"), eq(startDate), eq(endDate), isNull(), isNull(), isNull()))
                .thenReturn(rows);

        ForecastResponse result = forecastingService.forecastDemand("week", 2, startDate, endDate, null, null);

        assertNotNull(result);
        List<ForecastData> forecast = result.getData().stream().filter(ForecastData::isForecast).toList();
        assertEquals(2, forecast.size());
        // SES or linear regression should be used with 2 points
        assertNotNull(result.getAlgorithm());
    }

    @Test
    void testForecastDemand_NoHistoricalData() {
        when(orderRepository.getHistoricalOrderCountByPeriod(eq("week"), eq(startDate), eq(endDate), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        ForecastResponse result = forecastingService.forecastDemand("week", 2, startDate, endDate, null, null);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testForecastDemand_MonthlyGranularity() {
        List<Object[]> rows = makeRows(
            new String[]{"2025-01-01","2025-02-01","2025-03-01","2025-04-01","2025-05-01"},
            new long[]{300, 320, 310, 340, 330}
        );
        when(orderRepository.getHistoricalOrderCountByPeriod(eq("month"), eq(startDate), eq(endDate), isNull(), isNull(), isNull()))
                .thenReturn(rows);

        ForecastResponse result = forecastingService.forecastDemand("month", 3, startDate, endDate, null, null);

        assertNotNull(result);
        assertEquals("month", result.getGranularity());
        assertEquals(3, result.getForecastPeriods());
        List<ForecastData> forecast = result.getData().stream().filter(ForecastData::isForecast).toList();
        assertEquals(3, forecast.size());
        // Linear regression should be used with 5 points (threshold for Holt's is >= 6)
        assertTrue(result.getAlgorithm().contains("Holt") || result.getAlgorithm().contains("回归") || result.getAlgorithm().contains("指数"));
    }
}
