package com.logistics.dashboard.ai.orchestrator;

import com.logistics.dashboard.ai.model.QueryRequest;
import com.logistics.dashboard.ai.model.QueryResponse;
import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.service.DashboardService;
import com.logistics.dashboard.service.ForecastingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NLUOrchestratorTest {

    private NLUOrchestrator nluOrchestrator;

    @BeforeEach
    void setUp() {
        DashboardServiceStub dashboardService = new DashboardServiceStub();
        ForecastingServiceStub forecastingService = new ForecastingServiceStub();
        nluOrchestrator = new NLUOrchestrator("", dashboardService, forecastingService);
    }

    @Test
    void testProcessQuery_KpiQuestion_ReturnsAnswer() {
        QueryRequest request = new QueryRequest("2025年有多少UPS的订单延误了？");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 12, 31));

        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertEquals("kpi", response.getChartType());
        assertTrue(response.getAnswer().contains("UPS") || response.getQueryPlan().contains("UPS"));
    }

    @Test
    void testProcessQuery_TrendQuestion_ReturnsTimeSeries() {
        QueryRequest request = new QueryRequest("按月显示订单量趋势");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
        assertEquals("time_series", response.getChartType());
    }

    @Test
    void testProcessQuery_CarrierQuestion_ReturnsBreakdown() {
        QueryRequest request = new QueryRequest("各承运商延误率对比");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
        assertEquals("bar", response.getChartType());
    }

    @Test
    void testProcessQuery_RegionQuestion_ReturnsRegionBreakdown() {
        QueryRequest request = new QueryRequest("哪个地区的订单延误最多？");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
        assertEquals("bar", response.getChartType());
        assertTrue(response.getAnswer().contains("地区"));
    }

    @Test
    void testProcessQuery_CategoryQuestion_ReturnsCategoryBreakdown() {
        QueryRequest request = new QueryRequest("哪个品类的订单延误最多？");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
        assertEquals("bar", response.getChartType());
        assertTrue(response.getAnswer().contains("品类"));
        assertEquals("category", ((java.util.Map<String, Object>) response.getChartData()).get("dimension"));
    }

    @Test
    void testProcessQuery_ForecastQuestion_ReturnsForecast() {
        QueryRequest request = new QueryRequest("预测未来4周的订单量");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertEquals("forecast", response.getChartType());
        assertTrue(response.getExplanation().contains("AI 仅负责参数提取"));
    }

    @Test
    void testProcessQuery_WithExplicitFilters_MergesFilters() {
        QueryRequest request = new QueryRequest("总订单量");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 6, 30));
        request.setCarriers(List.of("UPS", "FedEx"));

        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getFilters());
        assertTrue(response.getQueryPlan().contains("UPS"));
        assertTrue(response.getQueryPlan().contains("FedEx"));
    }

    @Test
    void testProcessQuery_AlwaysReturnsNonNull() {
        QueryRequest request = new QueryRequest("任意问题");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertNotNull(response.getChartType());
    }

    @Test
    void testProcessQuery_RegionOnTimeRateQuestion_ReturnsBreakdownWithOnTimeRate() {
        QueryRequest request = new QueryRequest("2025年，哪个地区准时率最高？");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getChartData());
        assertEquals("bar", response.getChartType());
        assertTrue(response.getAnswer().contains("准时率"));
        assertEquals("region", ((java.util.Map<String, Object>) response.getChartData()).get("dimension"));
        @SuppressWarnings("unchecked")
        java.util.List<String> onTimeRates = (java.util.List<String>) ((java.util.Map<String, Object>) response.getChartData()).get("onTimeRates");
        assertNotNull(onTimeRates);
        assertFalse(onTimeRates.isEmpty());
    }

    @Test
    void testProcessQuery_SpecificOnTimeRateQuestion_ReturnsKpi() {
        QueryRequest request = new QueryRequest("2025年Q1 FedEx在EU地区的准时率？");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 3, 31));
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertEquals("kpi", response.getChartType());
        assertTrue(response.getAnswer().contains("准时率"));
        assertTrue(response.getQueryPlan().contains("FedEx"));
        assertTrue(response.getQueryPlan().contains("EU"));
    }

    @Test
    void testProcessQuery_ForecastOnTimeRateQuestion_ReturnsMetricForecast() {
        QueryRequest request = new QueryRequest("预测未来4周US-W的准时率？");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertEquals("forecast", response.getChartType());
        assertTrue(response.getAnswer().contains("准时率预测"));
        assertTrue(response.getQueryPlan().contains("US-W"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> chartData = (java.util.Map<String, Object>) response.getChartData();
        assertEquals("on_time_rate", chartData.get("metricType"));
    }

    @Test
    void testProcessQuery_ForecastDelayRateQuestion_ReturnsMetricForecast() {
        QueryRequest request = new QueryRequest("预测未来4周FedEx的延误率");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertEquals("forecast", response.getChartType());
        assertTrue(response.getAnswer().contains("延误率预测"));
        assertTrue(response.getQueryPlan().contains("FedEx"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> chartData = (java.util.Map<String, Object>) response.getChartData();
        assertEquals("delay_rate", chartData.get("metricType"));
    }

    @Test
    void testProcessQuery_IrrelevantQuestion_ReturnsFallback() {
        QueryRequest request = new QueryRequest("你是谁？");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertEquals("none", response.getChartType());
        assertTrue(response.getAnswer().contains("物流数据分析助手"));
        assertTrue(response.getExplanation().contains("无关"));
    }

    @Test
    void testProcessQuery_CasualGreeting_ReturnsFallback() {
        QueryRequest request = new QueryRequest("hello，今天天气怎么样？");
        QueryResponse response = nluOrchestrator.processQuery(request);

        assertNotNull(response);
        assertEquals("none", response.getChartType());
        assertTrue(response.getAnswer().contains("物流数据分析助手"));
    }

    // ── Stubs ─────────────────────────────────────────────────────────────────

    static class DashboardServiceStub extends DashboardService {
        DashboardServiceStub() {
            super(null);
        }

        @Override
        public KpiResponse getKPIs(LocalDate startDate, LocalDate endDate,
                                   List<String> carriers, List<String> regions, List<String> categories) {
            return new KpiResponse(500L, 400L, 60L, new BigDecimal("80.00"), new BigDecimal("3.50"));
        }

        @Override
        public TimeSeriesResponse getOrderVolumeTimeSeries(String granularity,
                                                           LocalDate startDate, LocalDate endDate,
                                                           List<String> carriers, List<String> regions, List<String> categories) {
            return new TimeSeriesResponse("month", List.of(
                new TimeSeriesData(LocalDate.of(2025, 1, 1), 100L),
                new TimeSeriesData(LocalDate.of(2025, 2, 1), 120L)
            ));
        }

        @Override
        public CarrierBreakdownResponse getCarrierBreakdown(LocalDate startDate, LocalDate endDate,
                                                             List<String> carriers, List<String> regions, List<String> categories) {
            return new CarrierBreakdownResponse(List.of(
                new CarrierBreakdown("UPS", 300L, 30L, new BigDecimal("10.00")),
                new CarrierBreakdown("FedEx", 200L, 20L, new BigDecimal("10.00"))
            ));
        }

        @Override
        public RegionBreakdownResponse getRegionBreakdown(LocalDate startDate, LocalDate endDate,
                                                           List<String> carriers, List<String> regions, List<String> categories) {
            return new RegionBreakdownResponse(List.of(
                new RegionBreakdown("US-W", 400L, 40L, new BigDecimal("10.00")),
                new RegionBreakdown("EU", 300L, 20L, new BigDecimal("6.67"))
            ));
        }

        @Override
        public CategoryBreakdownResponse getCategoryBreakdown(LocalDate startDate, LocalDate endDate,
                                                               List<String> carriers, List<String> regions, List<String> categories) {
            return new CategoryBreakdownResponse(List.of(
                new CategoryBreakdown("Electronics", 350L, 35L, new BigDecimal("10.00")),
                new CategoryBreakdown("Apparel", 250L, 15L, new BigDecimal("6.00"))
            ));
        }

        @Override
        public DeliveryPerformanceResponse getDeliveryPerformance(String granularity,
                                                                   LocalDate startDate, LocalDate endDate,
                                                                   List<String> carriers, List<String> regions, List<String> categories) {
            return new DeliveryPerformanceResponse("month", List.of(
                new DeliveryPerformance("2025-01", 80L, 20L),
                new DeliveryPerformance("2025-02", 90L, 10L)
            ));
        }
    }

    static class ForecastingServiceStub extends ForecastingService {
        ForecastingServiceStub() {
            super(null);
        }

        @Override
        public ForecastResponse forecastDemand(String granularity, int periods,
                                               LocalDate startDate, LocalDate endDate,
                                               List<String> carriers, List<String> regions, List<String> categories) {
            return new ForecastResponse(
                granularity,
                periods,
                "Holt双指数平滑法 (α=0.3, β=0.1)",
                new BigDecimal("1.20"),
                List.of(
                    new ForecastData(LocalDate.of(2025, 1, 6), 100L, false),
                    new ForecastData(LocalDate.of(2025, 1, 13), 120L, false),
                    new ForecastData(LocalDate.of(2025, 1, 20), 115L, true),
                    new ForecastData(LocalDate.of(2025, 1, 27), 118L, true)
                ),
                "基于 Holt双指数平滑法：\n- 趋势判断：↑ 上升趋势 (5.0%)\n- 预测均值：117 单/期\n- 预测区间：115 ~ 118 单\n- 建议备货量（安全库存20%缓冲）：140 单/期"
            );
        }

        @Override
        public ForecastResponse forecastMetric(String metricType, String granularity, int periods,
                                               LocalDate startDate, LocalDate endDate,
                                               List<String> carriers, List<String> regions, List<String> categories) {
            return new ForecastResponse(
                granularity,
                periods,
                "Holt双指数平滑法 (α=0.3, β=0.1)",
                null,
                List.of(
                    new ForecastData(LocalDate.of(2025, 1, 6), new BigDecimal("85.50"), false),
                    new ForecastData(LocalDate.of(2025, 1, 13), new BigDecimal("87.20"), false),
                    new ForecastData(LocalDate.of(2025, 1, 20), new BigDecimal("88.00"), true),
                    new ForecastData(LocalDate.of(2025, 1, 27), new BigDecimal("88.50"), true)
                ),
                "基于 Holt双指数平滑法对 准时率 的预测\n- 趋势判断：↑ 预计改善 (2.5%)\n- 预测均值：88.00%\n- 预测区间：88.00% ~ 88.50%\n- 历史拟合平均误差：1.20%\n- 建议：服务质量优秀",
                metricType
            );
        }
    }
}
