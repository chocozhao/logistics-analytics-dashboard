package com.logistics.dashboard.ai.orchestrator;

import com.logistics.dashboard.ai.model.QueryRequest;
import com.logistics.dashboard.ai.model.QueryResponse;
import com.logistics.dashboard.ai.tools.AiTools;
import com.logistics.dashboard.dto.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.math.BigDecimal;

@Service
public class NLUOrchestrator {

    private ChatLanguageModel model;
    private AiAssistant assistant;
    private final AiTools aiTools;

    public NLUOrchestrator(
            @Value("${openai.api.key:}") String apiKey,
            AiTools aiTools) {

        this.aiTools = aiTools;

        // Check if API key is provided
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("WARNING: OPENAI_API_KEY not provided. Natural Language Query functionality will be limited.");
            // Create a mock model that returns placeholder responses
            this.model = null;
            this.assistant = null;
        } else {
            try {
                // Create chat model with DeepSeek (OpenAI-compatible API)
                this.model = dev.langchain4j.model.openai.OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl("https://api.deepseek.com")
                        .modelName("deepseek-chat")
                        .temperature(0.1)
                        .maxTokens(500)
                        .build();

                // Create chat memory
                ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

                // Create AI assistant with tool support
                this.assistant = AiServices.builder(AiAssistant.class)
                        .chatLanguageModel(model)
                        .chatMemory(chatMemory)
                        .tools(aiTools)
                        .build();

                System.out.println("AI model initialized successfully with DeepSeek API");
            } catch (Exception e) {
                System.out.println("WARNING: Failed to initialize AI model. Natural Language Query functionality will be limited. Error: " + e.getMessage());
                this.model = null;
                this.assistant = null;
            }
        }
    }

    /**
     * Process a natural language query and return structured response
     */
    public QueryResponse processQuery(QueryRequest request) {
        try {
            // Check if AI model is available
            if (model == null || assistant == null) {
                QueryResponse placeholderResponse = new QueryResponse();
                placeholderResponse.setAnswer("自然语言查询功能需要配置AI API密钥。请设置OPENAI_API_KEY环境变量。");
                placeholderResponse.setExplanation("此功能使用DeepSeek的AI模型来解释关于物流数据的自然语言问题。");
                placeholderResponse.setChartType("none");

                // You can still use other dashboard features without OpenAI
                if (request.getQuestion() != null && !request.getQuestion().isEmpty()) {
                    String question = request.getQuestion().toLowerCase();
                    if (question.contains("order") || question.contains("delivery") || question.contains("carrier")) {
                        placeholderResponse.setAnswer("自然语言查询当前不可用。请使用仪表板的标准分析功能查看订单、交货和承运商数据。");
                    }
                }

                // Add mock data for visualization and raw data when AI is not available
                populateResponseWithMockData(placeholderResponse, request, request.getQuestion());
                return placeholderResponse;
            }

            // Prepare system message with context
            String systemPrompt = createSystemPrompt(request);

            // Get response from AI assistant - combine system prompt and user question
            String fullMessage = "System: " + systemPrompt + "\n\nUser: " + request.getQuestion();
            String aiResponse = assistant.chat(fullMessage);

            // For now, return a simple response
            // In a full implementation, we would parse the AI's tool calls and execute them
            QueryResponse response = new QueryResponse();
            response.setAnswer(aiResponse);
            response.setExplanation("The AI interpreted your question and selected appropriate analytical tools.");
            response.setChartType("none");

            // Add filters if provided
            if (request.getStartDate() != null || request.getEndDate() != null ||
                request.getCarriers() != null || request.getRegions() != null) {
                Map<String, Object> filters = new HashMap<>();
                if (request.getStartDate() != null) filters.put("startDate", request.getStartDate());
                if (request.getEndDate() != null) filters.put("endDate", request.getEndDate());
                if (request.getCarriers() != null) filters.put("carriers", request.getCarriers());
                if (request.getRegions() != null) filters.put("regions", request.getRegions());
                response.setFilters(filters);
            }

            // Always add mock data for visualization and raw data for demo purposes
            // This ensures frontend has data to display even when AI only returns text
            populateResponseWithMockData(response, request, request.getQuestion());

            return response;

        } catch (Exception e) {
            // If AI fails, return a helpful message instead of error
            QueryResponse response = new QueryResponse();
            response.setAnswer("自然语言查询功能当前不可用。请使用仪表板的标准分析功能查看订单、交货和承运商数据。");
            response.setExplanation("AI服务暂时不可用，但您仍然可以使用筛选器查看数据。");
            response.setChartType("none");
            response.setError(null); // Explicitly set error to null
            System.out.println("AI query failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());

            // Add mock data for visualization and raw data
            populateResponseWithMockData(response, request, request.getQuestion());
            return response;
        }
    }

    /**
     * Create system prompt with context about available tools and data
     */
    private String createSystemPrompt(QueryRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant for a logistics analytics dashboard. ");
        prompt.append("Your role is to interpret user questions about logistics data and select appropriate analytical tools. ");
        prompt.append("\n\n");
        prompt.append("Available data includes: orders, delivery performance, carrier performance, and time series trends. ");
        prompt.append("All data is read-only from a PostgreSQL database. ");
        prompt.append("\n\n");
        prompt.append("You have access to these tools:\n");
        prompt.append("1. getTimeSeries - Get aggregated values over time (day, week, month)\n");
        prompt.append("2. getBreakdown - Get aggregated values by categorical dimension (carrier, region)\n");
        prompt.append("3. getKpi - Get key performance indicators (total orders, delivered, delayed, on-time rate, avg delivery days)\n");
        prompt.append("4. getDeliveryPerformance - Get delivery performance data (on-time vs delayed)\n");
        prompt.append("5. forecastDemand - Forecast future order volume (placeholder for now)\n");
        prompt.append("\n");
        prompt.append("IMPORTANT RULES:\n");
        prompt.append("- NEVER make up or guess data values. Always use the tools.\n");
        prompt.append("- If a user asks about specific dates, use the date range provided.\n");
        prompt.append("- If no date range is specified, suggest using a reasonable default (e.g., last 30 days).\n");
        prompt.append("- Always explain what tools you would use and why.\n");
        prompt.append("\n");

        // Add context about filters if provided
        if (request.getStartDate() != null || request.getEndDate() != null) {
            prompt.append("Context: User has specified date range: ");
            if (request.getStartDate() != null) prompt.append("from ").append(request.getStartDate());
            if (request.getEndDate() != null) prompt.append(" to ").append(request.getEndDate());
            prompt.append("\n");
        }

        if (request.getCarriers() != null && !request.getCarriers().isEmpty()) {
            prompt.append("Context: User filtered by carriers: ").append(String.join(", ", request.getCarriers())).append("\n");
        }

        if (request.getRegions() != null && !request.getRegions().isEmpty()) {
            prompt.append("Context: User filtered by regions: ").append(String.join(", ", request.getRegions())).append("\n");
        }

        prompt.append("\n");
        prompt.append("Your response should be a clear, concise answer to the user's question, ");
        prompt.append("mentioning which tools would be used and what the results would show.");

        return prompt.toString();
    }

    /**
     * AI Assistant interface for LangChain4j
     */
    interface AiAssistant {

        String chat(@UserMessage String message);
    }

    /**
     * Check if a question is related to logistics data
     */
    private boolean isDataRelatedQuestion(String question) {
        if (question == null || question.isEmpty()) return false;

        String lower = question.toLowerCase();

        // 物流数据相关关键词
        String[] dataKeywords = {
            "订单", "order", "订购", "购买", "销量",
            "交付", "delivery", "交货", "运输", "送达",
            "承运商", "carrier", "快递", "物流公司", "运输商",
            "趋势", "trend", "变化", "增长", "下降",
            "性能", "performance", "效率", "准时", "延迟",
            "地区", "region", "区域", "城市", "地点",
            "预测", "forecast", "预计", "未来", "kpi",
            "指标", "metric", "统计", "数字", "数量"
        };

        // 系统无关关键词（应跳过可视化）
        String[] unrelatedKeywords = {
            "你是谁", "你是什么", "你叫什么", "你从哪来",
            "你好", "hello", "hi", "你好吗", "how are you",
            "帮助", "help", "说明", "解释", "教程",
            "天气", "time", "日期", "今天星期几", "现在几点"
        };

        // 检查是否包含无关关键词
        for (String keyword : unrelatedKeywords) {
            if (lower.contains(keyword)) return false;
        }

        // 检查是否包含物流关键词
        for (String keyword : dataKeywords) {
            if (lower.contains(keyword)) return true;
        }

        // 默认：假设与数据相关（保持现有行为）
        return true;
    }

    /**
     * Populate basic response fields for non-data-related questions
     */
    private void populateBasicResponseFields(QueryResponse response, QueryRequest request,
                                            String question, String explanation) {
        response.setExplanation(explanation);

        // 设置默认指标
        List<String> defaultMetrics = Arrays.asList("系统交互", "问题分类");
        response.setMetrics(defaultMetrics);

        // 设置查询计划
        response.setQueryPlan("系统检测到此问题与物流数据无关，提供通用回答。");

        // 添加筛选条件
        if (request.getStartDate() != null || request.getEndDate() != null ||
            request.getCarriers() != null || request.getRegions() != null) {
            Map<String, Object> filters = new HashMap<>();
            if (request.getStartDate() != null) filters.put("startDate", request.getStartDate());
            if (request.getEndDate() != null) filters.put("endDate", request.getEndDate());
            if (request.getCarriers() != null) filters.put("carriers", request.getCarriers());
            if (request.getRegions() != null) filters.put("regions", request.getRegions());
            response.setFilters(filters);
        }

        // 生成最小化原始数据
        List<Map<String, Object>> rawData = new ArrayList<>();
        Map<String, Object> sampleRow = new HashMap<>();
        sampleRow.put("question", question);
        sampleRow.put("question_type", "system_interaction");
        sampleRow.put("response_type", "text_only");
        sampleRow.put("timestamp", LocalDate.now().toString());
        rawData.add(sampleRow);
        response.setRawData(rawData);
    }

    /**
     * Populate response with mock data for visualization and raw data when AI is unavailable
     */
    private void populateResponseWithMockData(QueryResponse response, QueryRequest request, String question) {
        if (question == null || question.isEmpty()) {
            question = "订单趋势";
        }

        String lowerQuestion = question.toLowerCase();
        Random random = ThreadLocalRandom.current();

        // Determine chart type based on question content
        if (lowerQuestion.contains("趋势") || lowerQuestion.contains("时间") || lowerQuestion.contains("order") || lowerQuestion.contains("trend")) {
            // Time series data
            populateTimeSeriesMockData(response, random);
        } else if (lowerQuestion.contains("承运商") || lowerQuestion.contains("carrier") || lowerQuestion.contains("快递")) {
            // Carrier breakdown data
            populateCarrierBreakdownMockData(response, random);
        } else if (lowerQuestion.contains("交付") || lowerQuestion.contains("delivery") || lowerQuestion.contains("performance")) {
            // Delivery performance data
            populateDeliveryPerformanceMockData(response, random);
        } else if (lowerQuestion.contains("kpi") || lowerQuestion.contains("指标") || lowerQuestion.contains("率") || lowerQuestion.contains("rate")) {
            // KPI data
            populateKpiMockData(response, random);
        } else {
            // Default to time series
            populateTimeSeriesMockData(response, random);
        }

        // Add filters from request
        if (request.getStartDate() != null || request.getEndDate() != null ||
            request.getCarriers() != null || request.getRegions() != null) {
            Map<String, Object> filters = new HashMap<>();
            if (request.getStartDate() != null) filters.put("startDate", request.getStartDate());
            if (request.getEndDate() != null) filters.put("endDate", request.getEndDate());
            if (request.getCarriers() != null) filters.put("carriers", request.getCarriers());
            if (request.getRegions() != null) filters.put("regions", request.getRegions());
            response.setFilters(filters);
        }

        // Set metrics based on question
        List<String> metrics = new ArrayList<>();
        if (lowerQuestion.contains("订单") || lowerQuestion.contains("order")) {
            metrics.add("订单数量");
            metrics.add("订单价值");
        }
        if (lowerQuestion.contains("交付") || lowerQuestion.contains("delivery")) {
            metrics.add("准时交付率");
            metrics.add("平均交付天数");
            metrics.add("延迟订单数");
        }
        if (lowerQuestion.contains("承运商") || lowerQuestion.contains("carrier")) {
            metrics.add("承运商分布");
            metrics.add("延迟率比较");
        }
        if (metrics.isEmpty()) {
            metrics.add("订单趋势");
            metrics.add("交付表现");
        }
        response.setMetrics(metrics);

        // Set query plan
        response.setQueryPlan("由于AI服务暂时不可用，系统使用模拟数据展示查询结果。实际功能恢复后，将使用真实数据分析工具处理您的查询。");

        // Generate raw data (sample rows)
        List<Map<String, Object>> rawData = generateRawMockData(random);
        response.setRawData(rawData);
    }

    private void populateTimeSeriesMockData(QueryResponse response, Random random) {
        response.setChartType("time_series");

        // Create simplified chart data for frontend
        Map<String, Object> chartData = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        LocalDate startDate = LocalDate.now().minusDays(30);

        for (int i = 0; i < 31; i++) {
            LocalDate date = startDate.plusDays(i);
            labels.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
            values.add(50L + random.nextInt(200)); // Random between 50-250
        }

        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("chartType", "line");

        response.setChartData(chartData);
        // Append mock data explanation to existing explanation if any
        String existingExplanation = response.getExplanation();
        String mockExplanation = "显示过去30天的订单趋势模拟数据。实际AI功能恢复后，将基于您的筛选条件提供真实的时间序列分析。";
        if (existingExplanation == null || existingExplanation.isEmpty()) {
            response.setExplanation(mockExplanation);
        } else {
            response.setExplanation(existingExplanation + " " + mockExplanation);
        }
    }

    private void populateCarrierBreakdownMockData(QueryResponse response, Random random) {
        response.setChartType("bar");

        // Create simplified chart data for frontend
        Map<String, Object> chartData = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        String[] carriers = {"UPS", "FedEx", "DHL", "USPS", "Amazon Logistics", "Regional Carrier"};

        for (String carrier : carriers) {
            labels.add(carrier);
            values.add(500L + random.nextInt(2000)); // 500-2500 orders
        }

        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("chartType", "bar");

        response.setChartData(chartData);
        // Append mock data explanation to existing explanation if any
        String existingExplanation = response.getExplanation();
        String mockExplanation = "显示各承运商的订单分布模拟数据。实际AI功能恢复后，将基于您的筛选条件提供真实的承运商表现分析。";
        if (existingExplanation == null || existingExplanation.isEmpty()) {
            response.setExplanation(mockExplanation);
        } else {
            response.setExplanation(existingExplanation + " " + mockExplanation);
        }
    }

    private void populateDeliveryPerformanceMockData(QueryResponse response, Random random) {
        response.setChartType("bar");

        // Create simplified chart data for frontend
        Map<String, Object> chartData = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        // Generate weekly data for last 4 weeks
        LocalDate today = LocalDate.now();

        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            String period = weekStart.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
            labels.add(period);
            Long onTime = 200L + random.nextInt(300); // 200-500
            values.add(onTime);
        }

        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("chartType", "bar");

        response.setChartData(chartData);
        // Append mock data explanation to existing explanation if any
        String existingExplanation = response.getExplanation();
        String mockExplanation = "显示过去4周准时交付数量的模拟数据。实际AI功能恢复后，将基于您的筛选条件提供真实的交付性能分析。";
        if (existingExplanation == null || existingExplanation.isEmpty()) {
            response.setExplanation(mockExplanation);
        } else {
            response.setExplanation(existingExplanation + " " + mockExplanation);
        }
    }

    private void populateKpiMockData(QueryResponse response, Random random) {
        response.setChartType("kpi");

        // Create simplified chart data for frontend - for KPI we can show a bar chart with metrics
        Map<String, Object> chartData = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();

        Long totalOrders = 10000L + random.nextInt(20000); // 10000-30000
        Long deliveredOrders = totalOrders * 8 / 10 + random.nextInt(totalOrders.intValue() / 10); // 80-90% delivered
        Long delayedOrders = deliveredOrders / 10 + random.nextInt(deliveredOrders.intValue() / 5); // 10-30% of delivered delayed
        BigDecimal onTimeRate = BigDecimal.valueOf((deliveredOrders - delayedOrders) * 100.0 / deliveredOrders).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal avgDeliveryDays = BigDecimal.valueOf(3.5 + random.nextDouble() * 2.5).setScale(2, BigDecimal.ROUND_HALF_UP); // 3.5-6.0 days

        // Create KPI summary for chart
        labels.add("总订单");
        values.add(totalOrders);
        labels.add("已交付");
        values.add(deliveredOrders);
        labels.add("延迟订单");
        values.add(delayedOrders);

        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("chartType", "bar");
        chartData.put("kpiSummary", Map.of(
            "onTimeRate", onTimeRate.toString() + "%",
            "avgDeliveryDays", avgDeliveryDays.toString() + " 天"
        ));

        response.setChartData(chartData);
        // Append mock data explanation to existing explanation if any
        String existingExplanation = response.getExplanation();
        String mockExplanation = "显示关键绩效指标的模拟数据。准时交付率: " + onTimeRate + "%，平均交付天数: " + avgDeliveryDays + "天。实际AI功能恢复后，将基于您的筛选条件提供真实的KPI分析。";
        if (existingExplanation == null || existingExplanation.isEmpty()) {
            response.setExplanation(mockExplanation);
        } else {
            response.setExplanation(existingExplanation + " " + mockExplanation);
        }
    }

    private List<Map<String, Object>> generateRawMockData(Random random) {
        List<Map<String, Object>> rawData = new ArrayList<>();
        String[] carriers = {"UPS", "FedEx", "DHL", "USPS", "Amazon Logistics", "Regional Carrier"};
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia"};
        String[] statuses = {"delivered", "in_transit", "pending", "cancelled"};

        for (int i = 0; i < 10; i++) { // Generate 10 sample rows
            Map<String, Object> row = new HashMap<>();
            row.put("id", i + 1);
            row.put("order_date", LocalDate.now().minusDays(random.nextInt(30)).toString());
            row.put("promised_delivery_date", LocalDate.now().minusDays(random.nextInt(20)).toString());
            row.put("actual_delivery_date", LocalDate.now().minusDays(random.nextInt(15)).toString());
            row.put("status", statuses[random.nextInt(statuses.length)]);
            row.put("carrier", carriers[random.nextInt(carriers.length)]);
            row.put("destination_city", cities[random.nextInt(cities.length)]);
            row.put("destination_state", "CA");
            row.put("destination_region", "West");
            row.put("order_value", BigDecimal.valueOf(50 + random.nextDouble() * 4950).setScale(2, BigDecimal.ROUND_HALF_UP));
            row.put("sku", "SKU-" + String.format("%04d", 1000 + random.nextInt(9000)));
            row.put("quantity", 1 + random.nextInt(50));

            rawData.add(row);
        }

        return rawData;
    }
}