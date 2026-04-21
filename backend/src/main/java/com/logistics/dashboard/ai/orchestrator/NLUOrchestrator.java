package com.logistics.dashboard.ai.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.dashboard.ai.model.QueryRequest;
import com.logistics.dashboard.ai.model.QueryResponse;
import com.logistics.dashboard.dto.*;
import com.logistics.dashboard.service.DashboardService;
import com.logistics.dashboard.service.ForecastingService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * NLU Orchestrator: AI interprets the question and outputs a structured tool-call plan.
 * Java services execute all computations deterministically — AI never generates data values.
 *
 * Flow: user question → AI → JSON plan → Java service → real data → response
 */
@Service
public class NLUOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NLUOrchestrator.class);

    // Valid enum values for validation
    private static final Set<String> VALID_CARRIERS  = Set.of(
            "UPS","FedEx","DHL","USPS","LaserShip","OnTrac","DPD","GLS","Royal Mail");
    private static final Set<String> VALID_REGIONS   = Set.of("US-W","US-C","US-E","EU","UK");
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "BOOK","PAPER","PENCIL","CRAYON","MARKER","BRUSH","PAINT","STICKER");
    private static final Set<String> VALID_TOOLS     = Set.of(
            "getKpi","getTimeSeries","getBreakdown","getDeliveryPerformance","forecastDemand");
    private static final Set<String> VALID_GRAN      = Set.of("day","week","month");

    private static final LocalDate DATA_START = LocalDate.of(2025, 1, 1);
    private static final LocalDate DATA_END   = LocalDate.of(2025, 12, 31);

    private ChatLanguageModel model;
    private final DashboardService     dashboardService;
    private final ForecastingService   forecastingService;
    private final ObjectMapper         mapper = new ObjectMapper();

    // ── Prompt template ──────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
You are a logistics analytics assistant. Your job is to interpret the user's question about freight/order data and output a SINGLE JSON object describing which analysis tool to call.

DATABASE SCHEMA (table: orders):
- order_date DATE, delivery_date DATE (NULL if not yet delivered)
- status: 'delivered' | 'delayed' | 'in_transit' | 'exception' | 'canceled'
  NOTE: 'delayed' is a status value, not computed from dates.
- carrier: UPS, FedEx, DHL, USPS, LaserShip, OnTrac, DPD, GLS, Royal Mail
- region: US-W, US-C, US-E, EU, UK
- product_category: BOOK, PAPER, PENCIL, CRAYON, MARKER, BRUSH, PAINT, STICKER
- origin_city, destination_city, warehouse, sku
- order_value_usd, unit_price_usd, quantity, is_promo, promo_discount_pct

DATA DATE RANGE: 2025-01-01 to 2025-12-31

TOOLS:
1. getKpi       - Returns: totalOrders, deliveredOrders, delayedOrders, onTimeRate, avgDeliveryDays
2. getTimeSeries - Returns: time-bucketed order counts. Params: granularity (day/week/month)
3. getBreakdown  - Returns: carrier breakdown (total_orders, delayed_orders, delay_rate). Params: dimension=carrier
4. getDeliveryPerformance - Returns: on-time vs delayed counts per time period. Params: granularity
5. forecastDemand - Returns: historical + forecasted order counts. Params: periods (int), granularity

PARAMETER EXTRACTION RULES:
- Extract year/month/quarter from question → convert to startDate/endDate (YYYY-MM-DD)
- "2025" → startDate=2025-01-01, endDate=2025-12-31
- "Q1 2025" → startDate=2025-01-01, endDate=2025-03-31
- "January 2025" → startDate=2025-01-01, endDate=2025-01-31
- If no date mentioned → use full data range 2025-01-01 to 2025-12-31
- Extract carrier names from question if mentioned
- Extract region names (US-W, US-C, US-E, EU, UK) from question if mentioned
- Extract product categories if mentioned

OUTPUT FORMAT (strict JSON, no markdown):
{
  "tool": "<tool_name>",
  "params": {
    "startDate": "YYYY-MM-DD",
    "endDate": "YYYY-MM-DD",
    "granularity": "month",
    "carriers": ["UPS"],
    "regions": [],
    "categories": [],
    "periods": 4
  },
  "answerTemplate": "<one-sentence description of what the answer will show>"
}

EXAMPLES:
Q: "2025年有多少UPS的订单延误了？"
A: {"tool":"getKpi","params":{"startDate":"2025-01-01","endDate":"2025-12-31","carriers":["UPS"],"regions":[],"categories":[]},"answerTemplate":"2025年UPS延误订单总数"}

Q: "Show me monthly order volume trend"
A: {"tool":"getTimeSeries","params":{"startDate":"2025-01-01","endDate":"2025-12-31","granularity":"month","carriers":[],"regions":[],"categories":[]},"answerTemplate":"月度订单量趋势"}

Q: "Which carrier has the highest delay rate?"
A: {"tool":"getBreakdown","params":{"startDate":"2025-01-01","endDate":"2025-12-31","dimension":"carrier","carriers":[],"regions":[],"categories":[]},"answerTemplate":"各承运商延误率对比"}

Q: "Forecast next 4 weeks of orders"
A: {"tool":"forecastDemand","params":{"startDate":"2025-01-01","endDate":"2025-12-31","granularity":"week","periods":4,"carriers":[],"regions":[],"categories":[]},"answerTemplate":"未来4周订单量预测"}

Output ONLY the JSON object, nothing else.
""";

    public NLUOrchestrator(
            @Value("${openai.api.key:}") String apiKey,
            DashboardService dashboardService,
            ForecastingService forecastingService) {

        this.dashboardService   = dashboardService;
        this.forecastingService = forecastingService;

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY not set — NLQ will use keyword-based fallback");
            this.model = null;
        } else {
            try {
                this.model = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl("https://api.deepseek.com")
                        .modelName("deepseek-chat")
                        .temperature(0.0)
                        .maxTokens(300)
                        .build();
                log.info("AI model initialized (DeepSeek)");
            } catch (Exception e) {
                log.warn("Failed to initialize AI model: {}", e.getMessage());
                this.model = null;
            }
        }
    }

    // ── Main entry point ─────────────────────────────────────────────────────

    public QueryResponse processQuery(QueryRequest request) {
        String question = request.getQuestion();
        log.info("Processing query: {}", question);

        try {
            // 1. Get tool plan (from AI or fallback)
            ToolPlan plan = resolvePlan(question, request);
            log.info("Resolved plan: tool={} params={}", plan.tool, plan.params);

            // 2. Execute plan against real data
            return executePlan(plan, question);

        } catch (Exception e) {
            log.error("Query processing error: {}", e.getMessage(), e);
            QueryResponse err = new QueryResponse();
            err.setAnswer("查询处理时发生错误，请稍后重试。");
            err.setChartType("none");
            err.setError(e.getMessage());
            return err;
        }
    }

    // ── Plan resolution ──────────────────────────────────────────────────────

    private ToolPlan resolvePlan(String question, QueryRequest request) {
        ToolPlan plan = null;

        // Try AI first
        if (model != null) {
            plan = askAiForPlan(question);
        }

        // Fallback: keyword-based heuristic
        if (plan == null) {
            plan = heuristicPlan(question, request);
        }

        // Merge any explicit frontend filters (they take priority over AI-extracted)
        if (request.getCarriers() != null && !request.getCarriers().isEmpty()) {
            plan.params.put("carriers", request.getCarriers());
        }
        if (request.getRegions() != null && !request.getRegions().isEmpty()) {
            plan.params.put("regions", request.getRegions());
        }
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            plan.params.put("categories", request.getCategories());
        }
        if (request.getStartDate() != null) {
            plan.params.put("startDate", request.getStartDate().toString());
        }
        if (request.getEndDate() != null) {
            plan.params.put("endDate", request.getEndDate().toString());
        }

        return plan;
    }

    private ToolPlan askAiForPlan(String question) {
        try {
            String prompt = SYSTEM_PROMPT + "\n\nUser question: " + question;
            String response = model.generate(prompt);
            log.debug("AI raw response: {}", response);

            // Strip markdown code fences if present
            String json = response.trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("^```\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();

            JsonNode root = mapper.readTree(json);
            ToolPlan plan = new ToolPlan();
            plan.tool = root.path("tool").asText();
            plan.answerTemplate = root.path("answerTemplate").asText();

            if (!VALID_TOOLS.contains(plan.tool)) {
                log.warn("AI returned invalid tool: {}", plan.tool);
                return null;
            }

            JsonNode p = root.path("params");
            plan.params = new HashMap<>();
            if (p.has("startDate"))   plan.params.put("startDate", p.get("startDate").asText());
            if (p.has("endDate"))     plan.params.put("endDate", p.get("endDate").asText());
            if (p.has("granularity")) plan.params.put("granularity", p.get("granularity").asText());
            if (p.has("periods"))     plan.params.put("periods", p.get("periods").asInt(4));
            if (p.has("dimension"))   plan.params.put("dimension", p.get("dimension").asText("carrier"));

            // Validated list params
            plan.params.put("carriers",   parseAndValidateList(p.path("carriers"),   VALID_CARRIERS));
            plan.params.put("regions",    parseAndValidateList(p.path("regions"),    VALID_REGIONS));
            plan.params.put("categories", parseAndValidateList(p.path("categories"), VALID_CATEGORIES));

            return plan;

        } catch (Exception e) {
            log.warn("AI plan parsing failed: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseAndValidateList(JsonNode node, Set<String> validValues) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(n -> {
                String val = n.asText();
                if (validValues.contains(val)) result.add(val);
            });
        }
        return result;
    }

    // ── Keyword-based heuristic fallback ──────────────────────────────────────

    private ToolPlan heuristicPlan(String question, QueryRequest request) {
        String q = question.toLowerCase();
        ToolPlan plan = new ToolPlan();
        plan.params = new HashMap<>();

        // Date range
        LocalDate[] range = extractDateRange(q, request);
        plan.params.put("startDate", range[0].toString());
        plan.params.put("endDate",   range[1].toString());

        // Carriers
        List<String> carriers = VALID_CARRIERS.stream()
                .filter(c -> q.contains(c.toLowerCase()))
                .collect(Collectors.toList());
        plan.params.put("carriers", carriers);

        // Regions
        List<String> regions = VALID_REGIONS.stream()
                .filter(r -> q.contains(r.toLowerCase()))
                .collect(Collectors.toList());
        plan.params.put("regions", regions);

        // Categories
        List<String> cats = VALID_CATEGORIES.stream()
                .filter(c -> q.contains(c.toLowerCase()))
                .collect(Collectors.toList());
        plan.params.put("categories", cats);

        // Tool selection
        if (q.contains("预测") || q.contains("forecast") || q.contains("predict") || q.contains("未来")) {
            plan.tool = "forecastDemand";
            plan.params.put("granularity", "week");
            plan.params.put("periods", 4);
            plan.answerTemplate = "订单量预测";
        } else if (q.contains("承运商") || q.contains("carrier") || q.contains("快递公司") || q.contains("哪家")) {
            plan.tool = "getBreakdown";
            plan.params.put("dimension", "carrier");
            plan.answerTemplate = "承运商分析";
        } else if (q.contains("趋势") || q.contains("trend") || q.contains("变化") || q.contains("每月") || q.contains("monthly") || q.contains("每周") || q.contains("weekly")) {
            plan.tool = "getTimeSeries";
            plan.params.put("granularity", q.contains("日") || q.contains("day") ? "day"
                    : q.contains("周") || q.contains("week") ? "week" : "month");
            plan.answerTemplate = "订单量趋势";
        } else if (q.contains("准时") || q.contains("on-time") || q.contains("交付性能") || q.contains("performance")) {
            plan.tool = "getDeliveryPerformance";
            plan.params.put("granularity", "month");
            plan.answerTemplate = "交付性能分析";
        } else {
            plan.tool = "getKpi";
            plan.answerTemplate = "关键指标汇总";
        }

        return plan;
    }

    private LocalDate[] extractDateRange(String q, QueryRequest request) {
        // Explicit from request
        if (request.getStartDate() != null && request.getEndDate() != null) {
            return new LocalDate[]{request.getStartDate(), request.getEndDate()};
        }

        // Year pattern
        Matcher yearMatcher = Pattern.compile("(20\\d{2})年?").matcher(q);
        if (yearMatcher.find()) {
            int year = Integer.parseInt(yearMatcher.group(1));
            // Quarter
            if (q.contains("q1") || q.contains("第一季度")) return new LocalDate[]{LocalDate.of(year,1,1), LocalDate.of(year,3,31)};
            if (q.contains("q2") || q.contains("第二季度")) return new LocalDate[]{LocalDate.of(year,4,1), LocalDate.of(year,6,30)};
            if (q.contains("q3") || q.contains("第三季度")) return new LocalDate[]{LocalDate.of(year,7,1), LocalDate.of(year,9,30)};
            if (q.contains("q4") || q.contains("第四季度")) return new LocalDate[]{LocalDate.of(year,10,1), LocalDate.of(year,12,31)};
            return new LocalDate[]{LocalDate.of(year,1,1), LocalDate.of(year,12,31)};
        }

        // Default: full data range
        return new LocalDate[]{DATA_START, DATA_END};
    }

    // ── Plan execution ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private QueryResponse executePlan(ToolPlan plan, String question) {
        LocalDate startDate = LocalDate.parse(plan.params.getOrDefault("startDate", DATA_START.toString()).toString());
        LocalDate endDate   = LocalDate.parse(plan.params.getOrDefault("endDate", DATA_END.toString()).toString());
        List<String> carriers   = (List<String>) plan.params.getOrDefault("carriers",   List.of());
        List<String> regions    = (List<String>) plan.params.getOrDefault("regions",    List.of());
        List<String> categories = (List<String>) plan.params.getOrDefault("categories", List.of());

        QueryResponse response = new QueryResponse();
        response.setQueryPlan("工具: " + plan.tool
                + " | 时间范围: " + startDate + " 至 " + endDate
                + (carriers.isEmpty()   ? "" : " | 承运商: " + carriers)
                + (regions.isEmpty()    ? "" : " | 地区: " + regions)
                + (categories.isEmpty() ? "" : " | 品类: " + categories));

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("startDate", startDate.toString());
        filters.put("endDate",   endDate.toString());
        if (!carriers.isEmpty())   filters.put("carriers",   carriers);
        if (!regions.isEmpty())    filters.put("regions",    regions);
        if (!categories.isEmpty()) filters.put("categories", categories);
        response.setFilters(filters);

        switch (plan.tool) {

            case "getKpi" -> {
                KpiResponse kpi = dashboardService.getKPIs(startDate, endDate, carriers, regions, categories);
                response.setChartType("kpi");
                response.setAnswer(formatKpiAnswer(kpi, plan.answerTemplate, carriers, regions, startDate, endDate));
                response.setExplanation("从数据库实时聚合 — status='delayed' 计为延误，status='delivered' 计为已送达。");
                response.setMetrics(List.of("totalOrders", "deliveredOrders", "delayedOrders", "onTimeRate", "avgDeliveryDays"));

                Map<String, Object> chartData = new LinkedHashMap<>();
                chartData.put("chartType", "kpi");
                chartData.put("labels", List.of("总订单", "已送达", "延误"));
                chartData.put("values", List.of(kpi.getTotalOrders(), kpi.getDeliveredOrders(), kpi.getDelayedOrders()));
                chartData.put("kpiSummary", Map.of(
                        "onTimeRate",     kpi.getOnTimeRate() + "%",
                        "avgDeliveryDays", kpi.getAvgDeliveryDays() + " 天"
                ));
                response.setChartData(chartData);

                List<Map<String, Object>> raw = List.of(Map.of(
                        "总订单",     kpi.getTotalOrders(),
                        "已送达",     kpi.getDeliveredOrders(),
                        "延误",       kpi.getDelayedOrders(),
                        "准时率",     kpi.getOnTimeRate() + "%",
                        "平均送达天数", kpi.getAvgDeliveryDays()
                ));
                response.setRawData(raw);
            }

            case "getTimeSeries" -> {
                String gran = plan.params.getOrDefault("granularity", "month").toString();
                if (!VALID_GRAN.contains(gran)) gran = "month";
                TimeSeriesResponse ts = dashboardService.getOrderVolumeTimeSeries(gran, startDate, endDate, carriers, regions, categories);
                response.setChartType("time_series");
                response.setAnswer(plan.answerTemplate + "（" + startDate + " 至 " + endDate + "，粒度：" + gran + "）");
                response.setExplanation("按 " + gran + " 聚合的订单量趋势，来自实时数据库查询。");
                response.setMetrics(List.of("订单量", "时间趋势"));

                List<String> labels = ts.getData().stream().map(d -> d.getDate().toString()).collect(Collectors.toList());
                List<Long>   values = ts.getData().stream().map(TimeSeriesData::getCount).collect(Collectors.toList());
                Map<String, Object> chartData = new LinkedHashMap<>();
                chartData.put("chartType", "line");
                chartData.put("labels", labels);
                chartData.put("values", values);
                response.setChartData(chartData);

                List<Map<String, Object>> raw = ts.getData().stream()
                        .map(d -> Map.<String,Object>of("date", d.getDate().toString(), "count", d.getCount()))
                        .collect(Collectors.toList());
                response.setRawData(raw);
            }

            case "getBreakdown" -> {
                CarrierBreakdownResponse bd = dashboardService.getCarrierBreakdown(startDate, endDate, carriers, regions, categories);
                response.setChartType("bar");
                response.setAnswer("各承运商绩效分析（" + startDate + " 至 " + endDate + "）");
                response.setExplanation("按承运商汇总订单量和延误率，status='delayed' 计为延误。");
                response.setMetrics(List.of("总订单", "延误订单", "延误率"));

                List<String> labels = bd.getData().stream().map(CarrierBreakdown::getCarrier).collect(Collectors.toList());
                List<Long>   values = bd.getData().stream().map(CarrierBreakdown::getTotalOrders).collect(Collectors.toList());
                Map<String, Object> chartData = new LinkedHashMap<>();
                chartData.put("chartType", "bar");
                chartData.put("labels", labels);
                chartData.put("values", values);
                chartData.put("delayedValues", bd.getData().stream().map(CarrierBreakdown::getDelayedOrders).collect(Collectors.toList()));
                chartData.put("delayRates",    bd.getData().stream().map(c -> c.getDelayRate().toString()).collect(Collectors.toList()));
                response.setChartData(chartData);

                List<Map<String, Object>> raw = bd.getData().stream()
                        .map(c -> Map.<String,Object>of(
                                "carrier",      c.getCarrier(),
                                "totalOrders",  c.getTotalOrders(),
                                "delayedOrders", c.getDelayedOrders(),
                                "delayRate",    c.getDelayRate() + "%"
                        ))
                        .collect(Collectors.toList());
                response.setRawData(raw);
            }

            case "getDeliveryPerformance" -> {
                String gran = plan.params.getOrDefault("granularity", "month").toString();
                if (!VALID_GRAN.contains(gran)) gran = "month";
                DeliveryPerformanceResponse dp = dashboardService.getDeliveryPerformance(gran, startDate, endDate, carriers, regions, categories);
                response.setChartType("bar");
                response.setAnswer("交付性能分析（" + startDate + " 至 " + endDate + "）");
                response.setExplanation("按时间段展示准时 vs 延误订单数，直接来自 status 字段统计。");
                response.setMetrics(List.of("准时订单", "延误订单"));

                List<String> labels = dp.getData().stream().map(DeliveryPerformance::getPeriod).collect(Collectors.toList());
                List<Long> onTime  = dp.getData().stream().map(DeliveryPerformance::getOnTime).collect(Collectors.toList());
                List<Long> delayed = dp.getData().stream().map(DeliveryPerformance::getDelayed).collect(Collectors.toList());
                Map<String, Object> chartData = new LinkedHashMap<>();
                chartData.put("chartType", "bar");
                chartData.put("labels", labels);
                chartData.put("values", onTime);
                chartData.put("delayedValues", delayed);
                response.setChartData(chartData);

                List<Map<String, Object>> raw = dp.getData().stream()
                        .map(d -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("period",  d.getPeriod());
                            row.put("onTime",  d.getOnTime());
                            row.put("delayed", d.getDelayed());
                            return row;
                        })
                        .collect(Collectors.toList());
                response.setRawData(raw);
            }

            case "forecastDemand" -> {
                String gran = plan.params.getOrDefault("granularity", "week").toString();
                if (!VALID_GRAN.contains(gran)) gran = "week";
                int periods = Integer.parseInt(plan.params.getOrDefault("periods", 4).toString());
                ForecastResponse fc = forecastingService.forecastDemand(gran, periods, startDate, endDate, carriers, regions, categories);
                response.setChartType("forecast");
                response.setAnswer("未来 " + periods + " 个" + granLabel(gran) + "订单量预测");
                response.setExplanation("使用 " + fc.getAlgorithm() + " 基于历史数据确定性计算。AI 仅负责参数提取，不生成预测值。");
                response.setMetrics(List.of("历史订单量", "预测订单量", "安全库存建议"));

                List<String> labels   = fc.getData().stream().map(d -> d.getDate().toString()).collect(Collectors.toList());
                List<Long>   histVals = fc.getData().stream().filter(d -> !d.isForecast()).map(ForecastData::getValue).collect(Collectors.toList());
                List<Long>   foreVals = fc.getData().stream().filter(ForecastData::isForecast).map(ForecastData::getValue).collect(Collectors.toList());
                int splitIdx = (int) fc.getData().stream().filter(d -> !d.isForecast()).count();
                Map<String, Object> chartData = new LinkedHashMap<>();
                chartData.put("chartType", "forecast");
                chartData.put("labels", labels);
                chartData.put("historicalValues", histVals);
                chartData.put("forecastValues",   foreVals);
                chartData.put("splitIndex", splitIdx);
                chartData.put("algorithm",  fc.getAlgorithm());
                chartData.put("recommendations", fc.getRecommendations());
                response.setChartData(chartData);

                List<Map<String, Object>> raw = fc.getData().stream()
                        .map(d -> Map.<String,Object>of(
                                "date",       d.getDate().toString(),
                                "value",      d.getValue(),
                                "isForecast", d.isForecast()
                        ))
                        .collect(Collectors.toList());
                response.setRawData(raw);
            }

            default -> {
                response.setChartType("none");
                response.setAnswer("无法识别的分析类型，请换一种问法。");
            }
        }

        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatKpiAnswer(KpiResponse kpi, String template, List<String> carriers,
                                   List<String> regions, LocalDate start, LocalDate end) {
        String scope = "";
        if (!carriers.isEmpty()) scope += " [承运商: " + String.join(", ", carriers) + "]";
        if (!regions.isEmpty())  scope += " [地区: " + String.join(", ", regions) + "]";
        return String.format("%s (%s ~ %s)%s\n总订单: %d | 已送达: %d | 延误: %d | 准时率: %s%% | 平均送达: %s天",
                template, start, end, scope,
                kpi.getTotalOrders(), kpi.getDeliveredOrders(), kpi.getDelayedOrders(),
                kpi.getOnTimeRate(), kpi.getAvgDeliveryDays());
    }

    private String granLabel(String gran) {
        return switch (gran) { case "day" -> "天"; case "week" -> "周"; default -> "月"; };
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    private static class ToolPlan {
        String tool;
        String answerTemplate;
        Map<String, Object> params;
    }
}
