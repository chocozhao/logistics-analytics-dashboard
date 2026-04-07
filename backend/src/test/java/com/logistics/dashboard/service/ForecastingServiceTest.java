package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.ForecastResponse;
import com.logistics.dashboard.dto.TimeSeriesData;
import com.logistics.dashboard.dto.TimeSeriesResponse;
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
class ForecastingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DashboardService dashboardService;

    private ForecastingService forecastingService;

    private final LocalDate startDate = LocalDate.of(2024, 1, 1);
    private final LocalDate endDate = LocalDate.of(2024, 1, 31);

    @BeforeEach
    void setUp() {
        forecastingService = new ForecastingService(orderRepository, dashboardService);
    }

    @Test
    void testForecastDemand_WeeklyWithEnoughData() {
        // Arrange
        List<TimeSeriesData> historicalData = Arrays.asList(
            new TimeSeriesData(LocalDate.of(2024, 1, 1), BigDecimal.valueOf(100)),
            new TimeSeriesData(LocalDate.of(2024, 1, 8), BigDecimal.valueOf(120)),
            new TimeSeriesData(LocalDate.of(2024, 1, 15), BigDecimal.valueOf(110)),
            new TimeSeriesData(LocalDate.of(2024, 1, 22), BigDecimal.valueOf(130)),
            new TimeSeriesData(LocalDate.of(2024, 1, 29), BigDecimal.valueOf(125))
        );

        TimeSeriesResponse mockResponse = new TimeSeriesResponse("week", historicalData);
        when(dashboardService.getOrderVolumeTimeSeries(eq("week"), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        // Act
        ForecastResponse result = forecastingService.forecastDemand("week", 2, startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getHistorical());
        assertEquals(5, result.getHistorical().size());
        assertNotNull(result.getForecast());
        assertEquals(2, result.getForecast().size());
        assertNotNull(result.getMetrics());
        assertNotNull(result.getRecommendations());
        assertTrue(result.getMetrics().containsKey("method"));
        assertTrue(result.getMetrics().containsKey("mae"));
    }

    @Test
    void testForecastDemand_WeeklyWithInsufficientData() {
        // Arrange
        List<TimeSeriesData> historicalData = Arrays.asList(
            new TimeSeriesData(LocalDate.of(2024, 1, 1), BigDecimal.valueOf(100)),
            new TimeSeriesData(LocalDate.of(2024, 1, 8), BigDecimal.valueOf(120))
        );

        TimeSeriesResponse mockResponse = new TimeSeriesResponse("week", historicalData);
        when(dashboardService.getOrderVolumeTimeSeries(eq("week"), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        // Act
        ForecastResponse result = forecastingService.forecastDemand("week", 2, startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getHistorical());
        assertEquals(2, result.getHistorical().size());
        assertNotNull(result.getForecast());
        assertEquals(2, result.getForecast().size());
        assertNotNull(result.getMetrics());
        assertEquals("linear_regression", result.getMetrics().get("method"));
    }

    @Test
    void testForecastDemand_DailyGranularity() {
        // Arrange
        List<TimeSeriesData> historicalData = Arrays.asList(
            new TimeSeriesData(LocalDate.of(2024, 1, 1), BigDecimal.valueOf(50)),
            new TimeSeriesData(LocalDate.of(2024, 1, 2), BigDecimal.valueOf(55)),
            new TimeSeriesData(LocalDate.of(2024, 1, 3), BigDecimal.valueOf(60)),
            new TimeSeriesData(LocalDate.of(2024, 1, 4), BigDecimal.valueOf(58)),
            new TimeSeriesData(LocalDate.of(2024, 1, 5), BigDecimal.valueOf(62))
        );

        TimeSeriesResponse mockResponse = new TimeSeriesResponse("day", historicalData);
        when(dashboardService.getOrderVolumeTimeSeries(eq("day"), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        // Act
        ForecastResponse result = forecastingService.forecastDemand("day", 3, startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("day", result.getGranularity());
        assertNotNull(result.getHistorical());
        assertEquals(5, result.getHistorical().size());
        assertNotNull(result.getForecast());
        assertEquals(3, result.getForecast().size());
    }

    @Test
    void testForecastDemand_NoHistoricalData() {
        // Arrange
        TimeSeriesResponse mockResponse = new TimeSeriesResponse("week", List.of());
        when(dashboardService.getOrderVolumeTimeSeries(eq("week"), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        // Act
        ForecastResponse result = forecastingService.forecastDemand("week", 2, startDate, endDate, null, null);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getHistorical());
        assertTrue(result.getHistorical().isEmpty());
        assertNotNull(result.getForecast());
        assertEquals(2, result.getForecast().size());
        // Forecast should be zero when no historical data
        assertEquals(BigDecimal.ZERO, result.getForecast().get(0).getCount());
        assertEquals(BigDecimal.ZERO, result.getForecast().get(1).getCount());
    }

    @Test
    void testCalculateSafetyStockRecommendation() {
        // Act
        BigDecimal recommendation = forecastingService.calculateSafetyStockRecommendation(BigDecimal.valueOf(100.0));

        // Assert
        assertEquals(BigDecimal.valueOf(120.0), recommendation); // 100 * 1.2 = 120
    }

    @Test
    void testCalculateSafetyStockRecommendation_ZeroForecast() {
        // Act
        BigDecimal recommendation = forecastingService.calculateSafetyStockRecommendation(BigDecimal.ZERO);

        // Assert
        assertEquals(BigDecimal.ZERO, recommendation);
    }

    @Test
    void testGenerateRecommendations() {
        // Arrange
        BigDecimal forecastValue = BigDecimal.valueOf(100.0);
        String method = "exponential_smoothing";
        BigDecimal mae = BigDecimal.valueOf(5.0);

        // Act
        var recommendations = forecastingService.generateRecommendations(forecastValue, method, mae);

        // Assert
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());

        // Should contain at least one recommendation
        var inventoryRec = recommendations.stream()
                .filter(r -> r.getType().equals("inventory"))
                .findFirst();
        assertTrue(inventoryRec.isPresent());
        assertTrue(inventoryRec.get().getDescription().contains("safety stock"));
    }
}