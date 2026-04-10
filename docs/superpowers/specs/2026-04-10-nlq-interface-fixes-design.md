# NLQ接口问题修复设计

## 概述

修复物流分析仪表板自然语言查询(NLQ)界面的两个问题：
1. **无关问题显示可视化数据**：当用户询问与物流数据无关的问题（如"你是谁"）时，系统不应显示可视化图表
2. **查询详情不可用**：查询详情部分（AI解释、筛选条件、使用指标、查询计划）在某些情况下显示空白

## 问题分析

### 1. 无关问题显示可视化数据
- **根因**：`NLUOrchestrator.populateResponseWithMockData()`方法对所有问题都生成模拟数据
- **当前逻辑**：
  - 问题不匹配任何物流关键词 → 默认调用`populateTimeSeriesMockData()` 
  - 始终生成时间序列图表数据
- **影响**：用户问"你是谁"等无关问题时，仍显示图表，造成混淆

### 2. 查询详情完全空白
- **根因**：后端字段未在AI服务不可用时正确填充
- **具体问题**：
  - `explanation`、`metrics`、`queryPlan`字段可能为`null`或空
  - 前端直接显示空值，没有后备内容
- **场景**：当`OPENAI_API_KEY`未配置时，AI服务不可用，模拟数据生成可能遗漏字段

## 设计方案

### 问题1：无关问题可视化处理

#### 方案选择：空状态图表（用户选择）
当问题与物流数据无关时，显示"此问题无可视化数据"的友好提示，而不是隐藏整个图表区域。

#### 实现步骤：

**后端修改（`NLUOrchestrator.java`）：**

1. **添加问题相关性检测方法**：
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
           "预测", "forecast", "预计", "未来"
       };
       
       // 系统无关关键词（应跳过可视化）
       String[] unrelatedKeywords = {
           "你是谁", "你是什么", "你叫什么", "你从哪来",
           "你好", "hello", "hi", "你好吗", "how are you",
           "帮助", "help", "说明", "解释", "教程",
           "天气", "时间", "日期", "今天星期几"
       };
       
       // 检查是否包含无关关键词
       for (String keyword : unrelatedKeywords) {
           if (lower.contains(keyword)) return false;
       }
       
       // 检查是否包含物流关键词
       for (String keyword : dataKeywords) {
           if (lower.contains(keyword)) return true;
       }
       
       // 默认：假设与数据相关（保持现有行为，但显示提示）
       return true;
   }
   ```

2. **修改`populateResponseWithMockData()`方法**：
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
       
       // 原有逻辑（相关问题时保持不变）
       // ... 现有代码 ...
   }
   ```

3. **添加辅助方法`populateBasicResponseFields()`**：
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

**前端修改（`NaturalLanguageQuery.vue`）：**

1. **更新图表渲染逻辑**：
   ```vue
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
       <!-- KPI摘要显示逻辑保持不变 -->
     </div>
   </div>
   ```

2. **更新CSS添加空状态样式**：
   ```css
   .no-chart-message {
     text-align: center;
     padding: 40px;
     background-color: #f0f9ff;
     border-radius: 8px;
     border: 1px dashed #b3d8ff;
   }
   ```

### 问题2：查询详情空白修复

#### 方案选择：修复后端字段填充 + 前端空值处理

#### 实现步骤：

**后端修复（`NLUOrchestrator.java`）：**

1. **修复`populateResponseWithMockData()`中的字段填充**：
   - 确保在**所有分支**中都设置`explanation`、`metrics`、`queryPlan`
   - 检查`populateTimeSeriesMockData()`、`populateCarrierBreakdownMockData()`等方法的`explanation`拼接逻辑

2. **统一字段初始化**：修改`populateResponseWithMockData()`开头：
   ```java
   private void populateResponseWithMockData(QueryResponse response, QueryRequest request, String question) {
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
       
       // ... 原有逻辑继续 ...
   }
   ```

3. **修复各mock数据方法中的字段设置**：确保每个方法都正确设置相关字段

**前端增强（`NaturalLanguageQuery.vue`）：**

1. **添加空值检查和默认值**：
   ```vue
   <p><strong>AI解释:</strong> {{ queryResult.explanation || '系统正在分析您的问题...' }}</p>
   <p><strong>应用筛选条件:</strong> {{ formatFilters(queryResult.filters) || '无筛选条件' }}</p>
   <p><strong>使用指标:</strong> {{ 
     Array.isArray(queryResult.metrics) ? 
     queryResult.metrics.join(', ') : 
     queryResult.metrics || '通用指标' 
   }}</p>
   <p><strong>查询计划:</strong> {{ queryResult.queryPlan || '标准查询处理流程' }}</p>
   ```

2. **增强`formatFilters()`方法**：
   ```javascript
   const formatFilters = (filters) => {
     if (!filters) return null
     if (typeof filters === 'string') return filters
     try {
       const entries = Object.entries(filters)
       if (entries.length === 0) return null
       return entries
         .map(([key, value]) => `${key}: ${Array.isArray(value) ? value.join(', ') : value}`)
         .join('; ')
     } catch (e) {
       console.warn('格式化筛选条件失败:', e)
       return JSON.stringify(filters)
     }
   }
   ```

## 实施优先级

1. **高优先级**：修复查询详情空白（问题2） - 影响核心功能
2. **中优先级**：无关问题可视化处理（问题1） - 改善用户体验

## 测试计划

### 测试用例1：无关问题处理
- 输入："你是谁"
- 预期：显示文字回答，图表区域显示"此问题无可视化数据"
- 查询详情：显示完整信息（AI解释、查询计划等）

### 测试用例2：物流相关问题
- 输入："显示过去30天各承运商的总订单数"
- 预期：正常显示图表和数据
- 查询详情：显示完整信息

### 测试用例3：无API密钥场景
- 条件：`OPENAI_API_KEY`未配置
- 输入：任何问题
- 预期：显示模拟数据，查询详情完整
- 验证所有字段都有合理值

### 测试用例4：混合问题
- 输入："你好，能告诉我最近的订单趋势吗？"
- 预期：显示订单趋势图表（包含物流关键词）
- 验证问题检测逻辑的鲁棒性

## 风险与缓解

1. **问题检测误判**：
   - 风险：可能将相关问题误判为无关
   - 缓解：使用宽松的检测逻辑，默认假设问题相关，只有明显无关时才标记

2. **前后端兼容性**：
   - 风险：后端字段结构变化可能影响前端
   - 缓解：保持向后兼容，新字段有默认值

3. **性能影响**：
   - 风险：问题检测增加处理时间
   - 缓解：关键词检测是简单字符串操作，性能影响可忽略

## 成功标准

1. 用户询问无关问题时，界面清晰提示无可视化数据
2. 所有查询的详情部分都显示完整信息，无空白字段
3. 物流相关问题继续正常显示可视化图表
4. 前后端兼容性保持，不影响现有功能

## 部署计划

1. **开发阶段**：修改后端`NLUOrchestrator.java`和前端`NaturalLanguageQuery.vue`
2. **测试阶段**：本地验证所有测试用例
3. **部署阶段**：推送到Render部署，验证生产环境功能

---

*设计文档版本：1.0*  
*创建日期：2026-04-10*  
*作者：Claude Code*