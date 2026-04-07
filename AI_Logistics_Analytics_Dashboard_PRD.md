AI驱动的物流分析仪表板 - PRD与技术规格说明书（中文版）
1. 项目概述
   1.1 目标
   构建一个全栈Web应用，使物流运营团队能够通过以下方式探索货运数据：

传统分析仪表板（KPI + 图表）

由AI驱动的自然语言界面

预测性需求预测

1.2 核心原则
AI作为编排器，而非事实来源 – AI解释问题、选择工具并格式化结果；实际计算由确定性函数完成。

只读数据 – 不允许对底层数据集进行任何修改。

可解释性 – 每个答案都需包含使用的筛选条件、指标、查询计划及原始数据访问。

简单、正确、可部署 – 优先保证清晰度和正确性，而非功能完整性。

1.3 目标用户
需要监控绩效、诊断问题并规划能力的物流经理和分析师。

2. 功能需求
   2.1 仪表板（描述性分析）
   2.1.1 KPI（基于数据集实时计算）
   KPI	定义
   总订单数	所有订单的数量
   已妥投订单数	状态为“已妥投”的订单数量
   延迟订单数	actual_delivery_date > promised_delivery_date 的订单数
   准时妥投率	已妥投订单数 / 总订单数（百分比）
   平均配送时间	AVG（实际妥投日期 - 下单日期），单位：天
   2.1.2 图表（至少2个，实际实现以下所有）
   订单量随时间变化 – 折线图，可按日/周/月分组（用户可选）

配送绩效 – 堆叠柱状图（准时 vs 延迟）

承运商细分 – 水平柱状图，展示各承运商延迟率

目的地细分 – 按地区/城市展示订单量的柱状图或地图（可选）

2.1.3 仪表板控件
日期范围选择器（默认：最近30天）

承运商筛选（多选）

地区筛选

2.2 自然语言查询（诊断性分析）
用户可以输入类似以下的问题：

“展示最近3个月每周延迟订单的情况”

“哪家承运商的延迟率最高？”

“上个月有多少订单延迟妥投？”

“承运商X的平均配送时间是多少？”

“展示Y地区近期的订单量趋势”

系统行为：

理解问题（AI）

选择合适的分析工具

提取参数（时间范围、维度、指标、筛选条件）

执行确定性计算

返回答案 + 图表（如适用）+ 解释

2.3 动态图表生成
根据查询输出，系统自动选择图表类型：

时间序列 → 折线图

类别比较 → 柱状图

部分与整体 → 饼图（仅限2-5个类别）

分布 → 直方图

2.4 可解释性层
对于每个响应（包括仪表板和自然语言查询），需展示：

应用的筛选条件（例如：日期=2025-01-01至2025-03-31，承运商=UPS）

指标与维度（例如：订单数量，按周分组）

查询计划 – 所执行计算的结构化表示

原始数据表格 – 结果背后的数据样本或完整数据（可展开）

2.5 预测性与规范性分析（预测）
2.5.1 预测工具
用户可以请求：“预测未来4周的需求” 或 “预测下个季度的订单量”

要求：

使用历史订单量（按日/周聚合）

应用简单的预测方法：指数平滑法（Holt-Winters） 或 线性回归 作为备用

返回：

预测值（数字）

可视化：历史 + 预测折线图

库存建议：“根据预测，建议每周备货X件”

方法说明

2.5.2 输入灵活性
预测周期：默认4周，用户可指定

粒度：日、周（默认）

可选：按承运商或地区预测（加分项）

3. 非功能需求
   需求	规格
   性能	仪表板加载 < 3秒；NLQ响应 < 5秒
   部署	公开可访问URL，无需本地配置
   稳定性	对于支持的查询，零崩溃
   安全性	代码库中无密钥；只读数据库用户
   浏览器支持	最新版 Chrome, Firefox, Safari
4. 技术栈
   层级	技术	理由
   前端	Vue 3 + Vite + Pinia	响应式，组件化，易于集成图表
   UI组件	Element Plus 或 naive-ui	预置图表和表格
   图表库	ECharts 或 Chart.js	动态渲染，支持多种图表类型
   后端	Java 17 + Spring Boot 3	健壮成熟，易于编写REST API
   AI编排	LangChain4j 或 自定义 + OpenAI GPT-3.5-turbo	工具调用，结构化输出
   数据库	PostgreSQL 15	可靠，支持JSON和聚合查询
   预测	Apache Commons Math（回归）或自实现指数平滑	轻量级，无需外部ML服务
   部署	Docker + (Render.com / Fly.io / AWS ECS) + Nginx	公开URL，可复现
5. 后端架构
   5.1 高层组件
   text
   ┌─────────────────────────────────────────────────────────────┐
   │                        前端 (Vue)                            │
   └─────────────────────────────┬───────────────────────────────┘
   │ REST / WebSocket（AI流式可选）
   ┌─────────────────────────────▼───────────────────────────────┐
   │                     Spring Boot Controller                   │
   │  /api/dashboard/kpis  /api/dashboard/charts  /api/query     │
   │  /api/forecast                                              │
   └─────────────────────────────┬───────────────────────────────┘
   │
   ┌─────────────────────────────▼───────────────────────────────┐
   │                      编排层                                  │
   │  - NLU服务（AI + 工具选择器）                                │
   │  - 工具注册表（分析工具）                                    │
   │  - 查询计划器（结构化表示）                                  │
   └─────────────────────────────┬───────────────────────────────┘
   │
   ┌─────────────────────────────▼───────────────────────────────┐
   │                      工具执行层                              │
   │  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
   │  │ KPI聚合器     │ │ 时间序列     │ │  预测引擎            │ │
   │  │ (SQL构建器)  │ │ 查询执行器   │ │  (指数平滑/回归)     │ │
   │  └──────────────┘ └──────────────┘ └──────────────────────┘ │
   └─────────────────────────────┬───────────────────────────────┘
   │
   ┌─────────────────────────────▼───────────────────────────────┐
   │                    PostgreSQL（只读）                        │
   └─────────────────────────────────────────────────────────────┘
   5.2 API设计（REST）
   5.2.1 仪表板API
   text
   GET /api/dashboard/kpis?startDate=...&endDate=...&carrier=...&region=...
   响应: { totalOrders, deliveredOrders, delayedOrders, onTimeRate, avgDeliveryDays }

GET /api/dashboard/order-volume?granularity=day&startDate=&endDate=&carrier=&region=
响应: { data: [{ date, count }] }

GET /api/dashboard/delivery-performance?granularity=week&...
响应: { data: [{ period, onTime, delayed }] }

GET /api/dashboard/carrier-breakdown?startDate=&endDate=
响应: { data: [{ carrier, totalOrders, delayedOrders, delayRate }] }
5.2.2 自然语言查询API
text
POST /api/query
请求: { question: "展示最近3个月每周延迟订单的情况" }
响应: {
answer: "最近3个月，延迟订单在第12周达到峰值45单...",
chart: { type: "bar", data: { labels: ["第10周","第11周","第12周"], datasets: [{ name: "延迟订单", values: [32,28,45] }] } },
explanation: { filters: { dateRange: "2025-01-01 至 2025-03-31" }, metrics: ["delayed_orders"], dimensions: ["week"], queryPlan: "按周分组，筛选 actual_delivery > promised_delivery" },
rawData: [ { week: 10, delayed: 32 }, ... ]
}
5.2.3 预测API
text
POST /api/forecast
请求: { granularity: "week", periods: 4, startDate? (可选), carrier? (可选) }
响应: {
historical: [{ date, value }],
forecast: [{ date, predicted }],
chartConfig: { type: "line", ... },
recommendation: "预测下周需求为1250件，建议安全库存：150件。",
method: "指数平滑法 (alpha=0.3)",
mae: 12.5
}
5.3 数据库结构
sql
-- 主订单表（为简单起见采用扁平结构）
CREATE TABLE orders (
id SERIAL PRIMARY KEY,
order_date DATE NOT NULL,
promised_delivery_date DATE NOT NULL,
actual_delivery_date DATE,  -- NULL 表示尚未妥投
status VARCHAR(20) CHECK (status IN ('pending', 'in_transit', 'delivered', 'cancelled')),
carrier VARCHAR(50),
destination_city VARCHAR(100),
destination_state VARCHAR(50),
destination_region VARCHAR(50),
order_value DECIMAL(10,2),
sku VARCHAR(50),
quantity INTEGER
);

-- 性能索引
CREATE INDEX idx_order_date ON orders(order_date);
CREATE INDEX idx_carrier ON orders(carrier);
CREATE INDEX idx_status ON orders(status);
CREATE INDEX idx_delivery_dates ON orders(promised_delivery_date, actual_delivery_date);

-- 示例数据（由客户提供，假设有10,000+行）
派生字段（查询时计算）：

is_delayed = (actual_delivery_date > promised_delivery_date)

delivery_days = actual_delivery_date - order_date

5.4 AI编排设计（关键）
5.4.1 原则
AI从不生成SQL或直接数据值。 AI只输出带有经过验证参数的结构化工具调用。

5.4.2 工具定义（用于AI函数调用）
json
[
{
"name": "get_time_series",
"description": "获取随时间变化的聚合值（日/周/月）",
"parameters": {
"metric": { "type": "string", "enum": ["order_count", "delayed_count", "avg_delivery_days"] },
"dimension": { "type": "string", "enum": ["order_date", "delivery_week"] },
"granularity": { "type": "string", "enum": ["day", "week", "month"] },
"date_range": { "type": "object", "properties": { "start": "date", "end": "date" } },
"filters": { "type": "object", "properties": { "carrier": "string", "region": "string" } }
}
},
{
"name": "get_breakdown",
"description": "按分类维度（承运商、地区、状态）获取聚合值",
"parameters": {
"metric": { "type": "string", "enum": ["order_count", "delay_rate", "avg_delivery_days"] },
"group_by": { "type": "string", "enum": ["carrier", "destination_region", "status"] },
"date_range": { ... },
"filters": { ... }
}
},
{
"name": "get_kpi",
"description": "获取单个KPI值",
"parameters": {
"kpi": { "type": "string", "enum": ["total_orders", "on_time_rate", "avg_delivery_days", "delayed_orders"] },
"date_range": { ... },
"filters": { ... }
}
},
{
"name": "forecast_demand",
"description": "预测未来订单量",
"parameters": {
"granularity": { "type": "string", "enum": ["day", "week"] },
"horizon": { "type": "integer", "minimum": 1, "maximum": 52 },
"date_range": { ... },
"filters": { ... }
}
}
]
5.4.3 AI系统提示词
text
你是一个物流分析助手。你无法直接访问数据。
你的职责：理解用户问题并从列表中选择合适的工具。
始终输出一个JSON对象：
{
"tool": "工具名称",
"parameters": { ... },
"explanation": "为什么选择这个工具"
}
如果问题模糊，请请求澄清（输出工具："clarify"）。
绝不编造数据。绝不输出SQL。
5.4.4 后端NLU流程
接收 { question }

调用 OpenAI API，附带系统提示词 + 工具定义 + 用户问题

解析响应为工具调用（强制JSON模式）

验证参数（日期范围、枚举值等）

执行对应的Java服务方法

格式化结果（答案文本可由第二次轻量AI调用生成，或使用模板）

返回给前端

安全性： 所有参数均经过验证，仅用于确定性查询构建器（不允许拼接原始SQL）。

5.5 预测工具实现
方法： 对周度数据使用简单指数平滑法（SES），若失败则降级为线性回归。

java
public class ForecastingService {
public ForecastResult forecastWeekly(List<HistoricalData> history, int periods) {
// 使用 Apache Commons Math 或自实现 SES：
// y_hat(t+1) = alpha * y(t) + (1-alpha) * y_hat(t)
// 通过最小化后20%历史数据的MSE来优化alpha
// 返回预测值 + 置信区间（可选）
}
}
库存建议逻辑： 预测值乘以1.2作为安全库存。

6. 前端设计（Vue 3）
   6.1 页面结构
   text
   App.vue
   ├── Header（标题、日期范围选择器、全局筛选器）
   ├── Sidebar（仪表板、查询、预测）
   └── Main Content
   ├── DashboardView
   │   ├── KPI卡片（5个）
   │   ├── 订单量趋势图
   │   ├── 配送绩效图
   │   └── 承运商细分图
   ├── NLQueryView
   │   ├── 输入框 + 提交按钮
   │   ├── 响应区域（答案 + 图表 + 解释面板 + 原始数据表）
   │   └── 示例查询按钮
   └── ForecastingView
   ├── 控制面板（粒度、周期、可选筛选器）
   ├── 图表（历史 + 预测）
   ├── 建议文本
   └── 方法说明
   6.2 关键组件
   KpiCard.vue – 展示指标，可选趋势指示器

DynamicChart.vue – 接收 { type, data, options }，使用ECharts渲染

ExplanationPanel.vue – 展示筛选条件、指标、查询计划、原始数据表（可展开）

QueryInput.vue – 文本域 + 示例查询标签

6.3 状态管理（Pinia）
存储：

dashboardStore: KPI、图表数据、加载状态

queryStore: 当前问题、响应、历史记录（加分项）

filterStore: 全局日期范围、承运商、地区

6.4 API客户端
typescript
// services/api.ts
export const fetchKPIs = (filters) => axios.get('/api/dashboard/kpis', { params: filters })
export const postQuery = (question) => axios.post('/api/query', { question })
export const postForecast = (params) => axios.post('/api/forecast', params)
7. 数据流示例
   7.1 NLQ：“哪家承运商的延迟率最高？”
   前端 → POST /api/query { question: "..." }

后端 → 调用OpenAI，工具定义 → 返回 { tool: "get_breakdown", parameters: { metric: "delay_rate", group_by: "carrier", date_range: { start: "2025-01-01", end: "2025-03-31" } } }

后端 → 执行 BreakdownService.getDelayRateByCarrier(dateRange)

计算：SELECT carrier, (COUNT(CASE WHEN actual_delivery > promised_delivery THEN 1 END) * 100.0 / COUNT(*)) AS delay_rate FROM orders WHERE order_date BETWEEN ... GROUP BY carrier ORDER BY delay_rate DESC LIMIT 1

结果：{ carrier: "FedEx", delay_rate: 12.5 }

后端 → 生成答案文本：“在所选时间段内，FedEx的延迟率最高，为12.5%。”

返回JSON，包含答案、图表（所有承运商的柱状图）、解释、原始数据。

7.2 预测
前端 → POST /api/forecast { granularity: "week", periods: 4 }

后端 → 查询过去52周的每周历史订单量

应用指数平滑法 → 获得未来4周预测

返回预测 + 建议：“下周计划320件，随后一周315件……”

前端渲染折线图，包含两个序列（历史实线 + 预测虚线）

8. 开发任务分解（供Claude Code使用）
   阶段1：后端核心（高优先级）
   搭建Spring Boot项目，添加PostgreSQL驱动

创建orders表结构并加载示例数据（如未提供则生成）

实现DashboardService：

getKPIs()

getOrderVolumeTimeSeries()

getDeliveryPerformance()

getCarrierBreakdown()

实现仪表板端点的REST控制器

为聚合查询编写单元测试

阶段2：AI编排
添加OpenAI客户端依赖（或LangChain4j）

将工具定义为Java类

实现NLUOrchestrator：

调用OpenAI（系统提示词+工具）

解析JSON响应

路由到对应服务

实现getBreakdown和getTimeSeries服务

实现POST /api/query端点

添加验证和错误处理（未知问题、缺少参数）

阶段3：预测功能
实现ForecastingService，使用指数平滑法

添加端点POST /api/forecast

用历史数据测试，计算MAE

阶段4：前端（Vue）
初始化Vue 3 + Vite + Pinia + ECharts

构建仪表板页面：KPI和图表（调用后端API）

构建自然语言查询页面：输入、提交、显示答案/图表/解释/原始表

构建预测页面：控件、图表、建议

添加全局日期范围选择器和筛选器（跨组件同步）

阶段5：部署
为后端（Spring Boot）编写Dockerfile

为前端（nginx）编写多阶段Dockerfile

编写docker-compose.yml（后端+PostgreSQL+前端）

在Render.com或类似平台配置PostgreSQL

部署到公开URL

设置环境变量（OPENAI_API_KEY, DB_URL, DB_USER, DB_PASSWORD）

阶段6：文档（README.md）
本地运行指南

架构图（Mermaid）

AI方法说明

假设与局限性

未来改进计划

9. 假设与简化
   假设	理由
   数据集为单表orders，包含所有必要字段	简化查询，避免连接
   预测仅基于订单量，不基于SKU	客户需求未明确要求SKU级预测；订单量足够
   日期范围默认最近30天	常见分析模式
   AI仅支持英文查询	范围内可行
   无需身份验证	部署公开但数据只读；如需可添加基本认证并提供测试凭据
   指数平滑法自实现（或使用Apache Commons Math）	避免重型ML依赖
   图表在前端使用ECharts渲染	动态且性能良好
10. 局限性（明确说明）
    自然语言支持仅限于物流领域（订单、承运商、延迟、量）。无关问题将返回“我无法回答该问题”。

无实时流式传输 – 所有查询均为同步。

预测不考虑季节性（简单指数平滑法对强周模式可能效果不佳；未来可升级为Holt-Winters）。

无查询历史或缓存（加分项，v1不实现）。

AI可能误解模糊问题 – 用户需重新表述。

原始数据表最多显示100行，避免性能问题。

11. 未来改进（v1之后）
    添加查询历史，保存结果

实现缓存（Redis）以应对重复查询

编写全面的测试（单元测试 + 集成测试）

Docker Compose支持开发热重载

高级可解释性：预测的特征重要性（类似SHAP）

处理模糊查询：对话式澄清

支持SKU级预测

添加身份验证（JWT + 用户角色）

通过WebSocket实现实时更新

12. 部署说明（供Claude Code使用）
    12.1 后端（Spring Boot）Dockerfile
    dockerfile
    FROM openjdk:17-jdk-slim
    COPY target/*.jar app.jar
    ENTRYPOINT ["java","-jar","/app.jar"]
    12.2 前端（Vue）多阶段构建
    dockerfile
    FROM node:18 as build
    WORKDIR /app
    COPY package*.json ./
    RUN npm install
    COPY . .
    RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
12.3 docker-compose.yml
yaml
version: '3'
services:
db:
image: postgres:15
environment:
POSTGRES_DB: logistics
POSTGRES_USER: reader
POSTGRES_PASSWORD: readonly
volumes:
- pgdata:/var/lib/postgresql/data
- ./init.sql:/docker-entrypoint-initdb.d/init.sql
backend:
build: ./backend
ports:
- "8080:8080"
environment:
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/logistics
OPENAI_API_KEY: ${OPENAI_API_KEY}
depends_on:
- db
frontend:
build: ./frontend
ports:
- "80:80"
12.4 环境变量
OPENAI_API_KEY – NLQ所需

SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD

12.5 示例数据（init.sql）
生成10,000行模拟订单，日期范围2024-01-01至2025-03-31，包含合理的承运商、地区，并随机产生延迟。

13. 验收标准（供评审使用）
    仪表板加载5个KPI和3个以上图表（订单量趋势、配送绩效、承运商细分）

日期范围和筛选器影响所有仪表板组件

自然语言查询至少处理5个示例问题

每个NLQ响应包含答案、图表（如适用）、解释、原始数据

预测端点返回历史+预测图表和建议

AI从不生成原始SQL或捏造的数字

应用公开可访问，无需本地配置

README包含所有必需章节

14. 给Claude Code的最终提示
    优先保证正确性，而非功能完整性。 确保KPI和聚合计算准确。

使用预编译语句或JPA条件API – 即使是筛选条件也不允许字符串拼接。

AI编排需要实现降级 – 当OpenAI失败时，返回友好的错误消息。

测试NLQ流程，使用问题“展示最近3个月每周延迟订单的情况”，验证后端调用了正确的工具。

部署到免费层（Render、Fly.io或Railway） – 在README中提供URL。

不要提交任何密钥 – 全部使用环境变量。