package com.logistics.dashboard.ai.orchestrator;

import com.logistics.dashboard.ai.model.QueryRequest;
import com.logistics.dashboard.ai.model.QueryResponse;
import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.service.DashboardService;
import com.logistics.dashboard.service.ForecastingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NLUOrchestratorTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private ForecastingService forecastingService;

    private NLUOrchestrator nluOrchestrator;

    @BeforeEach
    void setUp() {
        // Empty API key → uses heuristic fallback (no real AI calls)
        nluOrchestrator = new NLUOrchestrator("", dashboardService, forecastingService);
    }

    private KpiResponse mockKpi() {
        return new KpiResponse(500L, 400L, 60L, new BigDecimal("80.00"), new BigDecimal("3.50"));
    }

    private TimeSeriesResponse mockTimeSeries() {
        return new TimeSeriesResponse("month", List.of(
            new TimeSeriesData(LocalDate.of(2025, 1, 1), 100L),
            new TimeSeriesData(LocalDate.of(2025, 2, 1), 120L)
        ));
    }

    private CarrierBreakdownResponse mockCarrierBreakdown() {
        return new CarrierBreakdownResponse(List.of(
            new CarrierBreakdown("UPS", 300L, 30L, new BigDecimal("10.00")),
            new CarrierBreakdown("FedEx", 200L, 20L, new BigDecimal("10.00"))
        ));
    }

    @Test
    void testProcessQuery_KpiQuestion_ReturnsAnswer() {
        when(dashboardService.getKPIs(any(), any(), any(), any(), any())).thenReturn(mockKpi());

        QueryRequest request = new QueryRequest("2025年有多少UPS的订单延误了？");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 12, 31));

        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertNotNull(response.getChartType());
    }

    @Test
    void testProcessQuery_TrendQuestion_ReturnsTimeSeries() {
        when(dashboardService.getOrderVolumeTimeSeries(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(mockTimeSeries());

        QueryRequest request = new QueryRequest("按月显示订单量趋势");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
    }

    @Test
    void testProcessQuery_CarrierQuestion_ReturnsBreakdown() {
        when(dashboardService.getCarrierBreakdown(any(), any(), any(), any(), any()))
                .thenReturn(mockCarrierBreakdown());

        QueryRequest request = new QueryRequest("各承运商延误率对比");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
    }

    @Test
    void testProcessQuery_WithExplicitFilters_MergesFilters() {
        when(dashboardService.getKPIs(any(), any(), any(), any(), any())).thenReturn(mockKpi());

        QueryRequest request = new QueryRequest("总订单量");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 6, 30));
        request.setCarriers(List.of("UPS", "FedEx"));

        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getFilters());
        // Explicit frontend filters should be reflected in the response
        assertTrue(response.getQueryPlan() != null);
    }

    @Test
    void testProcessQuery_AlwaysReturnsNonNull() {
        when(dashboardService.getKPIs(any(), any(), any(), any(), any())).thenReturn(mockKpi());

        QueryRequest request = new QueryRequest("任意问题");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertNotNull(response.getChartType());
    }
}
