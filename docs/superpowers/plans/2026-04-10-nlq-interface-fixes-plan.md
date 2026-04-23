# NLQ接口问题修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复自然语言查询接口的两个问题：1) 无关问题显示可视化数据，2) 查询详情空白

**Architecture:** 后端添加问题相关性检测，无关问题时设置chartType为"none"并提供空状态数据；修复后端字段填充确保所有分支都设置完整字段；前端增强空值处理并显示空状态提示。

**Tech Stack:** Java 17 + Spring Boot 3, Vue 3 + Element Plus, ECharts

---

## 文件结构

### 修改文件
- `backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java` - 添加问题检测，修复字段填充
- `frontend/src/components/NaturalLanguageQuery.vue` - 添加空状态处理，增强空值检查

### 测试文件
- `backend/src/test/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestratorTest.java` - 添加问题相关性检测测试
- 前端手动测试：测试不同问题类型的UI表现

---

### Task 1: 后端 - 添加问题相关性检测方法

**Files:**
- Modify: `backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java:200-260`

- [ ] **Step 1: 在populateResponseWithMockData方法前添加isDataRelatedQuestion方法**

```java
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
```

- [ ] **Step 2: 在isDataRelatedQuestion方法后添加populateBasicResponseFields方法**

```java
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
```

- [ ] **Step 3: 编译检查语法错误**

Run: `cd backend && mvn compile`
Expected: SUCCESS - 没有编译错误

- [ ] **Step 4: 提交修改**

```bash
git add backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java
git commit -m "feat: add question relevance detection and basic response fields methods"
```

---

### Task 2: 后端 - 修改populateResponseWithMockData方法处理无关问题

**Files:**
- Modify: `backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java:200-260` (populateResponseWithMockData方法)

- [ ] **Step 1: 在populateResponseWithMockData方法开头添加问题相关性检查**

```java
private void populateResponseWithMockData(QueryResponse response, QueryRequest request, String question) {
    if (question == null || question.isEmpty()) {
        question = "订单趋势";
    }
    
    String lowerQuestion = question.toLowerCase();
    Random random = ThreadLocalRandom.current();
    
    // 检查问题是否与物流数据相关
    boolean isDataRelated = isDataRelatedQuestion(question);
    
    if (!isDataRelated) {
        // 无关问题：设置无图表类型
        response.setChartType("none");
        
        // 创建空状态图表数据
        Map<String, Object> emptyChartData = new HashMap<>();
        emptyChartData.put("labels", new ArrayList<String>());
        emptyChartData.put("values", new ArrayList<Long>());
        emptyChartData.put("chartType", "none");
        emptyChartData.put("message", "此问题与物流数据无关，无可视化图表");
        
        response.setChartData(emptyChartData);
        
        // 设置基础字段（避免查询详情空白）
        populateBasicResponseFields(response, request, question, "这是一个系统交互问题，与物流数据分析无关。");
        return;
    }
    
    // 原有逻辑（相关问题时保持不变）...
}
```

注意：在`return;`语句后，原有逻辑继续执行。需要找到原有逻辑的开始位置（大约在200行），在`return;`后添加原有代码。

- [ ] **Step 2: 确保在return语句后保留原有逻辑**

查看现有代码，在populateResponseWithMockData方法中，找到原有逻辑的开始位置（大约在行号200-210），在`return;`后添加：

```java
    // 原有逻辑（相关问题时保持不变）
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
    
    // 原有逻辑继续...
```

- [ ] **Step 3: 编译检查**

Run: `cd backend && mvn compile`
Expected: SUCCESS - 没有编译错误

- [ ] **Step 4: 提交修改**

```bash
git add backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java
git commit -m "feat: add unrelated question handling in populateResponseWithMockData"
```

---

### Task 3: 后端 - 修复字段填充确保无空白字段

**Files:**
- Modify: `backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java:73-140` (processQuery方法) 和 `200-260` (populateResponseWithMockData方法)

- [ ] **Step 1: 在processQuery方法中确保字段初始化**

找到processQuery方法（大约73-140行），在try块开头添加字段初始化：

```java
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
            
            // 确保字段完整
            ensureResponseFieldsComplete(placeholderResponse);
            return placeholderResponse;
        }
        
        // ... 原有代码继续
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
        
        // 确保字段完整
        ensureResponseFieldsComplete(response);
        return response;
    }
}
```

- [ ] **Step 2: 添加ensureResponseFieldsComplete方法**

在类中添加新方法：

```java
private void ensureResponseFieldsComplete(QueryResponse response) {
    // 确保基础字段有默认值
    if (response.getExplanation() == null || response.getExplanation().isEmpty()) {
        response.setExplanation("基于您的查询生成的模拟数据分析结果。");
    }
    
    if (response.getMetrics() == null || response.getMetrics().isEmpty()) {
        List<String> defaultMetrics = Arrays.asList("订单数量", "交付表现", "承运商分析");
        response.setMetrics(defaultMetrics);
    }
    
    if (response.getQueryPlan() == null || response.getQueryPlan().isEmpty()) {
        response.setQueryPlan("使用模拟数据生成查询结果。实际AI功能恢复后，将基于真实数据执行分析。");
    }
    
    if (response.getRawData() == null) {
        response.setRawData(new ArrayList<>());
    }
    
    if (response.getChartData() == null && !"none".equals(response.getChartType())) {
        // 如果不是none类型但chartData为空，创建空数据
        Map<String, Object> emptyChartData = new HashMap<>();
        emptyChartData.put("labels", new ArrayList<String>());
        emptyChartData.put("values", new ArrayList<Long>());
        emptyChartData.put("chartType", "bar");
        emptyChartData.put("message", "数据生成中...");
        response.setChartData(emptyChartData);
    }
}
```

- [ ] **Step 3: 在populateResponseWithMockData方法开头调用ensureResponseFieldsComplete**

修改populateResponseWithMockData方法开头：

```java
private void populateResponseWithMockData(QueryResponse response, QueryRequest request, String question) {
    // 确保响应字段完整
    ensureResponseFieldsComplete(response);
    
    if (question == null || question.isEmpty()) {
        question = "订单趋势";
    }
    
    // ... 原有代码继续
}
```

- [ ] **Step 4: 编译测试**

Run: `cd backend && mvn compile`
Expected: SUCCESS - 没有编译错误

- [ ] **Step 5: 提交修改**

```bash
git add backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java
git commit -m "feat: ensure complete response fields with ensureResponseFieldsComplete method"
```

---

### Task 4: 后端 - 修复各mock数据方法的字段设置

**Files:**
- Modify: `backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java:265-400` (populateTimeSeriesMockData, populateCarrierBreakdownMockData等方法)

- [ ] **Step 1: 修复populateTimeSeriesMockData方法中的explanation设置**

找到populateTimeSeriesMockData方法（大约265-295行），修改解释设置逻辑：

```java
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
    
    // 确保有explanation
    String explanation = "显示过去30天的订单趋势模拟数据。实际AI功能恢复后，将基于您的筛选条件提供真实的时间序列分析。";
    if (response.getExplanation() == null || response.getExplanation().isEmpty()) {
        response.setExplanation(explanation);
    } else if (!response.getExplanation().contains("模拟数据")) {
        // 如果已有解释但不包含模拟数据说明，则追加
        response.setExplanation(response.getExplanation() + " " + explanation);
    }
}
```

- [ ] **Step 2: 修复populateCarrierBreakdownMockData方法中的explanation设置**

找到populateCarrierBreakdownMockData方法（大约297-325行）：

```java
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
    
    // 确保有explanation
    String explanation = "显示各承运商的订单分布模拟数据。实际AI功能恢复后，将基于您的筛选条件提供真实的承运商表现分析。";
    if (response.getExplanation() == null || response.getExplanation().isEmpty()) {
        response.setExplanation(explanation);
    } else if (!response.getExplanation().contains("模拟数据")) {
        response.setExplanation(response.getExplanation() + " " + explanation);
    }
}
```

- [ ] **Step 3: 修复populateDeliveryPerformanceMockData方法中的explanation设置**

找到populateDeliveryPerformanceMockData方法（大约327-359行）：

```java
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
    
    // 确保有explanation
    String explanation = "显示过去4周准时交付数量的模拟数据。实际AI功能恢复后，将基于您的筛选条件提供真实的交付性能分析。";
    if (response.getExplanation() == null || response.getExplanation().isEmpty()) {
        response.setExplanation(explanation);
    } else if (!response.getExplanation().contains("模拟数据")) {
        response.setExplanation(response.getExplanation() + " " + explanation);
    }
}
```

- [ ] **Step 4: 修复populateKpiMockData方法中的explanation设置**

找到populateKpiMockData方法（大约361-400行）：

```java
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
    
    // 确保有explanation
    String explanation = "显示关键绩效指标的模拟数据。准时交付率: " + onTimeRate + "%，平均交付天数: " + avgDeliveryDays + "天。实际AI功能恢复后，将基于您的筛选条件提供真实的KPI分析。";
    if (response.getExplanation() == null || response.getExplanation().isEmpty()) {
        response.setExplanation(explanation);
    } else if (!response.getExplanation().contains("模拟数据")) {
        response.setExplanation(response.getExplanation() + " " + explanation);
    }
}
```

- [ ] **Step 5: 编译测试**

Run: `cd backend && mvn compile`
Expected: SUCCESS - 没有编译错误

- [ ] **Step 6: 提交修改**

```bash
git add backend/src/main/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestrator.java
git commit -m "feat: fix explanation fields in all mock data methods"
```

---

### Task 5: 前端 - 增强formatFilters方法处理空值

**Files:**
- Modify: `frontend/src/components/NaturalLanguageQuery.vue:63-73` (formatFilters方法)

- [ ] **Step 1: 修改formatFilters方法添加更好的空值处理**

找到formatFilters方法（大约63-73行），修改为：

```javascript
const formatFilters = (filters) => {
  if (!filters) return null
  if (typeof filters === 'string') return filters
  try {
    const entries = Object.entries(filters)
    if (entries.length === 0) return null
    return entries
      .map(([key, value]) => {
        if (value === null || value === undefined) return `${key}: 空`
        if (Array.isArray(value)) {
          return value.length > 0 ? `${key}: ${value.join(', ')}` : null
        }
        return `${key}: ${value}`
      })
      .filter(item => item !== null)
      .join('; ')
  } catch (e) {
    console.warn('格式化筛选条件失败:', e)
    return JSON.stringify(filters)
  }
}
```

- [ ] **Step 2: 测试formatFilters方法**

在浏览器控制台或Vue开发工具中测试：
```javascript
// 测试空值
console.log(formatFilters(null)) // 预期: null
console.log(formatFilters({})) // 预期: null
console.log(formatFilters({startDate: '2024-01-01', endDate: null})) // 预期: "startDate: 2024-01-01; endDate: 空"
console.log(formatFilters({carriers: ['UPS', 'FedEx'], regions: []})) // 预期: "carriers: UPS, FedEx"
```

- [ ] **Step 3: 提交修改**

```bash
git add frontend/src/components/NaturalLanguageQuery.vue
git commit -m "feat: enhance formatFilters method with better null handling"
```

---

### Task 6: 前端 - 添加空状态图表处理

**Files:**
- Modify: `frontend/src/components/NaturalLanguageQuery.vue:230-247` (图表容器部分)

- [ ] **Step 1: 修改图表容器部分添加空状态处理**

找到图表容器部分（大约230-247行），修改为：

```vue
<el-divider content-position="left">可视化</el-divider>
<div v-if="queryResult.chartData" class="chart-container">
  <div v-if="queryResult.chartType === 'none' || queryResult.chartData.chartType === 'none'" class="no-chart-message">
    <el-alert
      :title="queryResult.chartData.message || '此问题无可视化数据'"
      type="info"
      show-icon
      :closable="false"
    />
  </div>
  <div v-else>
    <div ref="chartRef" class="chart"></div>
    <!-- Display KPI summary if available -->
    <div v-if="queryResult.chartData && queryResult.chartData.kpiSummary" class="kpi-summary">
      <el-divider content-position="left">关键指标摘要</el-divider>
      <div class="kpi-metrics">
        <div v-for="(value, key) in queryResult.chartData.kpiSummary" :key="key" class="kpi-metric">
          <span class="kpi-label">{{ key === 'onTimeRate' ? '准时交付率' : key === 'avgDeliveryDays' ? '平均交付天数' : key }}:</span>
          <span class="kpi-value">{{ value }}</span>
        </div>
      </div>
    </div>
  </div>
</div>
<div v-else class="no-chart">
  此查询无可视化数据
</div>
```

- [ ] **Step 2: 添加空状态CSS样式**

在style部分（大约370-400行）添加：

```css
.no-chart-message {
  text-align: center;
  padding: 40px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border: 1px dashed #b3d8ff;
  margin: 20px 0;
}

.no-chart-message .el-alert {
  background-color: transparent;
  border: none;
}
```

- [ ] **Step 3: 更新renderChart方法处理空图表**

找到renderChart方法（大约75-119行），在开头添加检查：

```javascript
const renderChart = (chartData) => {
  if (!chartRef.value) return
  
  // 检查是否是空图表
  if (chartData.chartType === 'none') {
    // 空图表已经在模板中显示消息，这里不需要渲染
    return
  }
  
  chartInstance = echarts.init(chartRef.value)
  
  // ... 原有代码继续
}
```

- [ ] **Step 4: 提交修改**

```bash
git add frontend/src/components/NaturalLanguageQuery.vue
git commit -m "feat: add empty chart state handling in frontend"
```

---

### Task 7: 前端 - 增强查询详情空值处理

**Files:**
- Modify: `frontend/src/components/NaturalLanguageQuery.vue:248-256` (查询详情部分)

- [ ] **Step 1: 修改查询详情部分添加默认值**

找到查询详情部分（大约248-256行），修改为：

```vue
<el-divider content-position="left">查询详情</el-divider>
<div class="explanation">
  <p><strong>AI解释:</strong> {{ queryResult.explanation || '系统正在分析您的问题...' }}</p>
  <p><strong>应用筛选条件:</strong> {{ formatFilters(queryResult.filters) || '无筛选条件' }}</p>
  <p><strong>使用指标:</strong> {{ 
    Array.isArray(queryResult.metrics) ? 
    queryResult.metrics.join(', ') : 
    queryResult.metrics || '通用指标' 
  }}</p>
  <p><strong>查询计划:</strong> {{ queryResult.queryPlan || '标准查询处理流程' }}</p>
</div>
```

- [ ] **Step 2: 修改原始数据表头处理避免空值错误**

找到原始数据表格部分（大约257-277行），修改为：

```vue
<el-divider content-position="left">原始数据</el-divider>
<div class="raw-data">
  <el-table
    :data="queryResult.rawData"
    border
    style="width: 100%"
    max-height="300"
    v-if="queryResult.rawData && queryResult.rawData.length > 0"
  >
    <el-table-column
      v-for="(value, key) in (queryResult.rawData[0] || {})"
      :key="key"
      :prop="key"
      :label="key"
      width="180"
    />
  </el-table>
  <div v-else class="no-raw-data">
    无原始数据
  </div>
</div>
```

- [ ] **Step 3: 测试前端修改**

启动前端开发服务器：
```bash
cd frontend && npm run dev
```

访问 `http://localhost:5173`，测试不同查询：
1. 问"你是谁" - 应该显示空状态提示
2. 问"显示订单趋势" - 应该正常显示图表
3. 检查查询详情部分是否有空白字段

- [ ] **Step 4: 提交修改**

```bash
git add frontend/src/components/NaturalLanguageQuery.vue
git commit -m "feat: enhance query details null handling in frontend"
```

---

### Task 8: 后端测试 - 创建NLUOrchestrator测试

**Files:**
- Create: `backend/src/test/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestratorTest.java`

- [ ] **Step 1: 创建测试文件目录结构**

```bash
mkdir -p backend/src/test/java/com/logistics/dashboard/ai/orchestrator
```

- [ ] **Step 2: 编写NLUOrchestratorTest测试类**

```java
package com.logistics.dashboard.ai.orchestrator;

import com.logistics.dashboard.ai.model.QueryRequest;
import com.logistics.dashboard.ai.model.QueryResponse;
import com.logistics.dashboard.ai.tools.AiTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NLUOrchestratorTest {

    @Mock
    private AiTools aiTools;
    
    private NLUOrchestrator nluOrchestrator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 创建无API密钥的测试实例
        nluOrchestrator = new NLUOrchestrator("", aiTools);
    }
    
    @Test
    void testIsDataRelatedQuestion_WithDataRelatedQuestions() {
        // 使用反射调用私有方法
        String[] dataRelatedQuestions = {
            "显示订单趋势",
            "最近交付表现如何",
            "各承运商比较",
            "What is the order volume",
            "carrier performance last week",
            "预测未来需求",
            "KPI指标是什么"
        };
        
        for (String question : dataRelatedQuestions) {
            boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                nluOrchestrator, "isDataRelatedQuestion", question);
            assertTrue(result, "应该识别为数据相关问题: " + question);
        }
    }
    
    @Test
    void testIsDataRelatedQuestion_WithUnrelatedQuestions() {
        String[] unrelatedQuestions = {
            "你是谁",
            "你好吗",
            "今天天气怎么样",
            "What is your name",
            "How are you",
            "帮助文档在哪里"
        };
        
        for (String question : unrelatedQuestions) {
            boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                nluOrchestrator, "isDataRelatedQuestion", question);
            assertFalse(result, "应该识别为无关问题: " + question);
        }
    }
    
    @Test
    void testProcessQuery_WithUnrelatedQuestion() {
        QueryRequest request = new QueryRequest();
        request.setQuestion("你是谁");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));
        request.setCarriers(Arrays.asList("UPS", "FedEx"));
        
        QueryResponse response = nluOrchestrator.processQuery(request);
        
        assertNotNull(response);
        assertEquals("none", response.getChartType());
        assertNotNull(response.getChartData());
        
        Map<String, Object> chartData = (Map<String, Object>) response.getChartData();
        assertEquals("none", chartData.get("chartType"));
        assertTrue(chartData.containsKey("message"));
        
        // 检查字段完整性
        assertNotNull(response.getExplanation());
        assertNotNull(response.getMetrics());
        assertNotNull(response.getQueryPlan());
        assertNotNull(response.getRawData());
        assertNotNull(response.getFilters());
    }
    
    @Test
    void testProcessQuery_WithDataRelatedQuestion() {
        QueryRequest request = new QueryRequest();
        request.setQuestion("显示订单趋势");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 1, 31));
        
        QueryResponse response = nluOrchestrator.processQuery(request);
        
        assertNotNull(response);
        assertNotEquals("none", response.getChartType()); // 应该不是none类型
        
        // 检查字段完整性
        assertNotNull(response.getExplanation());
        assertNotNull(response.getMetrics());
        assertNotNull(response.getQueryPlan());
        assertNotNull(response.getRawData());
    }
    
    @Test
    void testProcessQuery_WithNullQuestion() {
        QueryRequest request = new QueryRequest();
        // question为null
        
        QueryResponse response = nluOrchestrator.processQuery(request);
        
        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertNotNull(response.getExplanation());
        assertNotNull(response.getMetrics());
        assertNotNull(response.getQueryPlan());
    }
    
    @Test
    void testProcessQuery_WithEmptyQuestion() {
        QueryRequest request = new QueryRequest();
        request.setQuestion("");
        
        QueryResponse response = nluOrchestrator.processQuery(request);
        
        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertNotNull(response.getExplanation());
        assertNotNull(response.getMetrics());
        assertNotNull(response.getQueryPlan());
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `cd backend && mvn test -Dtest=NLUOrchestratorTest`
Expected: 所有测试通过

- [ ] **Step 4: 提交测试**

```bash
git add backend/src/test/java/com/logistics/dashboard/ai/orchestrator/NLUOrchestratorTest.java
git commit -m "test: add NLUOrchestrator tests for question relevance and field completeness"
```

---

### Task 9: 集成测试 - 验证端到端功能

**Files:**
- 手动测试

- [ ] **Step 1: 启动后端服务**

```bash
cd backend && mvn spring-boot:run
```
等待服务启动完成（看到"Started Application"消息）

- [ ] **Step 2: 启动前端服务**

```bash
cd frontend && npm run dev
```
访问 `http://localhost:5173`

- [ ] **Step 3: 测试无关问题（如"你是谁"）**

在NLQ界面输入"你是谁"，检查：
1. 是否显示文字回答
2. 图表区域是否显示"此问题无可视化数据"提示
3. 查询详情部分是否完整（AI解释、筛选条件、使用指标、查询计划都有内容）
4. 原始数据表是否显示

- [ ] **Step 4: 测试物流相关问题**

输入"显示过去30天各承运商的总订单数"，检查：
1. 是否正常显示图表
2. 查询详情部分是否完整
3. 原始数据是否显示

- [ ] **Step 5: 测试混合问题**

输入"你好，能告诉我最近的订单趋势吗？"，检查：
1. 是否显示订单趋势图表（包含"订单"关键词）
2. 查询详情是否完整

- [ ] **Step 6: 测试无筛选条件情况**

清空所有筛选条件，输入任意问题，检查筛选条件是否显示"无筛选条件"

- [ ] **Step 7: 记录测试结果**

所有测试通过后，记录结果。

- [ ] **Step 8: 提交最终修改**

```bash
git add .
git commit -m "fix: complete NLQ interface fixes with end-to-end testing"
```

---

## 自检清单

### 1. 规范覆盖检查
- [x] 问题1：无关问题显示空状态提示 ✓ Task 1-4, 6
- [x] 问题2：查询详情空白修复 ✓ Task 3-5, 7  
- [x] 后端字段完整性 ✓ Task 3-4
- [x] 前端空值处理 ✓ Task 5, 7
- [x] 测试覆盖 ✓ Task 8-9

### 2. 占位符扫描
- [x] 无"TBD"、"TODO"等占位符
- [x] 所有代码步骤都有完整实现
- [x] 所有测试都有具体代码

### 3. 类型一致性
- [x] 方法签名一致：`isDataRelatedQuestion`, `populateBasicResponseFields`, `ensureResponseFieldsComplete`
- [x] 字段名一致：`chartType`, `chartData`, `explanation`, `metrics`, `queryPlan`
- [x] 前端属性访问一致：`queryResult.chartType`, `queryResult.chartData`

---

计划完成并保存到 `docs/superpowers/plans/2026-04-10-nlq-interface-fixes-plan.md`。

**执行选项：**

1. **子代理驱动（推荐）** - 我为每个任务分派新的子代理，任务间进行审查，快速迭代

2. **内联执行** - 在此会话中使用executing-plans执行任务，使用检查点进行批量执行

**选择哪种方式？**