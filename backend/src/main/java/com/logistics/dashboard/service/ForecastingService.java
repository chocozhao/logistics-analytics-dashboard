package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.ForecastData;
import com.logistics.dashboard.dto.ForecastResponse;
import com.logistics.dashboard.dto.TimeSeriesData;
import com.logistics.dashboard.repository.OrderRepository;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic forecasting service.
 * Uses Holt's Double Exponential Smoothing (DES) which captures both level and trend.
 * Falls back to Simple Exponential Smoothing (SES) or Linear Regression as needed.
 * AI never generates forecast values — this service always computes from real data.
 */
@Service
public class ForecastingService {

    private static final Logger log = LoggerFactory.getLogger(ForecastingService.class);
    private final OrderRepository orderRepository;

    public ForecastingService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public ForecastResponse forecastDemand(String granularity, int periods,
                                           LocalDate startDate, LocalDate endDate,
                                           List<String> carriers, List<String> regions) {
        return forecastDemand(granularity, periods, startDate, endDate, carriers, regions, null);
    }

    /**
     * Forecast a metric (on-time rate or delay rate) over future periods.
     * Uses the same algorithms as demand forecasting but on percentage time series.
     */
    public ForecastResponse forecastMetric(String metricType, String granularity, int periods,
                                           LocalDate startDate, LocalDate endDate,
                                           List<String> carriers, List<String> regions,
                                           List<String> categories) {
        log.info("Forecasting metric: metricType={} granularity={} periods={} {} to {}",
                metricType, granularity, periods, startDate, endDate);

        try {
            String[] c   = toArray(carriers);
            String[] r   = toArray(regions);
            String[] cat = toArray(categories);

            List<Object[]> rows = orderRepository.getHistoricalMetricByPeriod(
                    metricType, granularity, startDate, endDate, c, r, cat);

            if (rows.isEmpty()) {
                return createEmptyForecast(granularity, periods, "无历史数据可用于预测");
            }

            List<LocalDate> dates  = new ArrayList<>();
            double[]        values = new double[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                dates.add(toLocalDate(rows.get(i)[0]));
                values[i] = ((Number) rows.get(i)[1]).doubleValue();
            }

            ForecastResult result;
            if (values.length >= 6) {
                result = holtsDoubleExponentialSmoothing(values, periods);
            } else if (values.length >= 2) {
                result = linearRegression(values, periods);
            } else {
                result = sesWithBestAlpha(values, periods);
            }

            List<ForecastData> data = new ArrayList<>();
            for (int i = 0; i < dates.size(); i++) {
                data.add(new ForecastData(dates.get(i), BigDecimal.valueOf(values[i]).setScale(2, RoundingMode.HALF_UP), false));
            }
            LocalDate lastDate = dates.get(dates.size() - 1);
            for (int i = 0; i < result.forecasts.length; i++) {
                double raw = Math.max(0, Math.min(100, result.forecasts[i]));
                BigDecimal val = BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
                data.add(new ForecastData(incrementDate(lastDate, granularity, i + 1), val, true));
            }

            String metricLabel = "on_time_rate".equals(metricType) ? "准时率" : "延误率";
            return new ForecastResponse(
                    granularity,
                    periods,
                    result.algorithm,
                    null,
                    data,
                    buildMetricRecommendations(values, result.forecasts, result.algorithm, metricLabel),
                    metricType
            );

        } catch (Exception e) {
            log.error("Metric forecast error: {}", e.getMessage(), e);
            return createEmptyForecast(granularity, periods, "预测失败: " + e.getMessage());
        }
    }

    public ForecastResponse forecastDemand(String granularity, int periods,
                                           LocalDate startDate, LocalDate endDate,
                                           List<String> carriers, List<String> regions,
                                           List<String> categories) {
        log.info("Forecasting: granularity={} periods={} {} to {}", granularity, periods, startDate, endDate);

        try {
            String[] c   = toArray(carriers);
            String[] r   = toArray(regions);
            String[] cat = toArray(categories);

            List<Object[]> rows = orderRepository.getHistoricalOrderCountByPeriod(
                    granularity, startDate, endDate, c, r, cat);

            if (rows.isEmpty()) {
                return createEmptyForecast(granularity, periods, "无历史数据可用于预测");
            }

            // Parse historical time series
            List<LocalDate> dates  = new ArrayList<>();
            double[]        values = new double[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                dates.add(toLocalDate(rows.get(i)[0]));
                values[i] = ((Number) rows.get(i)[1]).doubleValue();
            }

            // Select algorithm based on data length
            // >= 6: Holt's DES (enough data for stable level+trend estimation + validation)
            // 2-5:  Linear Regression (captures trend better than flat SES on short series)
            // 1:    Simple Exponential Smoothing (naive level-only forecast)
            ForecastResult result;
            if (values.length >= 6) {
                result = holtsDoubleExponentialSmoothing(values, periods);
            } else if (values.length >= 2) {
                result = linearRegression(values, periods);
            } else {
                result = sesWithBestAlpha(values, periods);
            }

            // Build response data list
            List<ForecastData> data = new ArrayList<>();
            for (int i = 0; i < dates.size(); i++) {
                data.add(new ForecastData(dates.get(i), Math.round(values[i]), false));
            }
            LocalDate lastDate = dates.get(dates.size() - 1);
            for (int i = 0; i < result.forecasts.length; i++) {
                double raw = result.forecasts[i];
                long   val = Math.max(0, Math.round(raw));
                data.add(new ForecastData(incrementDate(lastDate, granularity, i + 1), val, true));
            }

            return new ForecastResponse(
                    granularity,
                    periods,
                    result.algorithm,
                    new BigDecimal("1.20"),
                    data,
                    buildRecommendations(values, result.forecasts, result.algorithm)
            );

        } catch (Exception e) {
            log.error("Forecast error: {}", e.getMessage(), e);
            return createEmptyForecast(granularity, periods, "预测失败: " + e.getMessage());
        }
    }

    // ── Holt's Double Exponential Smoothing (level + trend) ──────────────────

    private ForecastResult holtsDoubleExponentialSmoothing(double[] values, int periods) {
        double bestAlpha = 0.3, bestBeta = 0.1;
        double bestMSE = Double.MAX_VALUE;

        for (double alpha = 0.1; alpha <= 0.9; alpha += 0.1) {
            for (double beta = 0.1; beta <= 0.5; beta += 0.1) {
                double mse = holtsInSampleMSE(values, alpha, beta);
                if (mse < bestMSE) {
                    bestMSE = mse;
                    bestAlpha = alpha;
                    bestBeta  = beta;
                }
            }
        }

        double[] forecasts = holtsForecasts(values, bestAlpha, bestBeta, periods);
        String algo = String.format("Holt双指数平滑法 (α=%.1f, β=%.1f)", bestAlpha, bestBeta);
        return new ForecastResult(forecasts, algo);
    }

    private double holtsInSampleMSE(double[] values, double alpha, double beta) {
        int n = values.length;
        int trainN = Math.max(2, n - Math.max(1, n / 5));
        double[] train = new double[trainN];
        System.arraycopy(values, 0, train, 0, trainN);

        double[] forecasts = holtsForecasts(train, alpha, beta, n - trainN);
        double sse = 0;
        for (int i = 0; i < forecasts.length; i++) {
            double e = values[trainN + i] - forecasts[i];
            sse += e * e;
        }
        return sse / forecasts.length;
    }

    private double[] holtsForecasts(double[] values, double alpha, double beta, int periods) {
        // Initialize level and trend
        double level = values[0];
        double trend = (values.length >= 2) ? values[1] - values[0] : 0;

        // Smooth over history
        for (int t = 1; t < values.length; t++) {
            double prevLevel = level;
            level = alpha * values[t] + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }

        // Project forward
        double[] forecasts = new double[periods];
        for (int h = 1; h <= periods; h++) {
            forecasts[h - 1] = level + h * trend;
        }
        return forecasts;
    }

    // ── Simple Exponential Smoothing fallback ─────────────────────────────────

    private ForecastResult sesWithBestAlpha(double[] values, int periods) {
        double bestAlpha = 0.3, bestMSE = Double.MAX_VALUE;
        for (double alpha = 0.1; alpha <= 0.9; alpha += 0.1) {
            double mse = sesMSE(values, alpha);
            if (mse < bestMSE) { bestMSE = mse; bestAlpha = alpha; }
        }
        double[] forecasts = sesForecasts(values, bestAlpha, periods);
        return new ForecastResult(forecasts, String.format("指数平滑法 (α=%.1f)", bestAlpha));
    }

    private double sesMSE(double[] values, double alpha) {
        int n = values.length;
        int trainN = Math.max(1, n - 1);
        double f = values[0];
        for (int t = 1; t < trainN; t++) {
            f = alpha * values[t] + (1 - alpha) * f;
        }
        double e = values[n - 1] - f;
        return e * e;
    }

    private double[] sesForecasts(double[] values, double alpha, int periods) {
        double f = values[0];
        for (double v : values) {
            f = alpha * v + (1 - alpha) * f;
        }
        double[] out = new double[periods];
        for (int i = 0; i < periods; i++) out[i] = f;
        return out;
    }

    // ── Linear regression fallback ────────────────────────────────────────────

    private ForecastResult linearRegression(double[] values, int periods) {
        SimpleRegression reg = new SimpleRegression();
        for (int i = 0; i < values.length; i++) reg.addData(i, values[i]);
        double[] forecasts = new double[periods];
        for (int i = 0; i < periods; i++) {
            forecasts[i] = reg.predict(values.length + i);
        }
        return new ForecastResult(forecasts, "线性回归");
    }

    // ── Recommendations text ──────────────────────────────────────────────────

    private String buildRecommendations(double[] history, double[] forecasts, String algorithm) {
        double avgForecast = 0, minF = Double.MAX_VALUE, maxF = -Double.MAX_VALUE;
        for (double f : forecasts) {
            avgForecast += f;
            if (f < minF) minF = f;
            if (f > maxF) maxF = f;
        }
        avgForecast /= forecasts.length;

        // Trend compared to recent history
        double recentAvg = 0;
        int window = Math.min(forecasts.length, history.length);
        for (int i = history.length - window; i < history.length; i++) recentAvg += history[i];
        recentAvg /= window;

        double trendPct = recentAvg > 0 ? ((avgForecast - recentAvg) / recentAvg) * 100 : 0;
        String trendDesc = trendPct > 5 ? "↑ 明显上升" : trendPct > 2 ? "↑ 温和上升" : trendPct < -5 ? "↓ 明显下降" : trendPct < -2 ? "↓ 温和下降" : "→ 平稳";

        // Historical fit quality (MAE on last 20% of data if algorithm supports back-testing)
        double mae = computeHistoricalMAE(history, algorithm);
        String reliability = mae > 0 ? String.format("历史拟合平均误差：%.0f 单/期", mae) : "历史数据较少，预测仅供参考";

        return String.format(
                "基于 %s\n" +
                "- 趋势判断：%s (%.1f%%)\n" +
                "- 预测均值：%.0f 单/期\n" +
                "- 预测区间：%.0f ~ %.0f 单\n" +
                "- %s\n" +
                "- 建议备货量（安全库存20%%缓冲）：%.0f 单/期",
                algorithm, trendDesc, trendPct, avgForecast, minF, maxF, reliability, avgForecast * 1.2
        );
    }

    private String buildMetricRecommendations(double[] history, double[] forecasts, String algorithm, String metricLabel) {
        double avgForecast = 0, minF = Double.MAX_VALUE, maxF = -Double.MAX_VALUE;
        for (double f : forecasts) {
            avgForecast += f;
            if (f < minF) minF = f;
            if (f > maxF) maxF = f;
        }
        avgForecast /= forecasts.length;

        double recentAvg = 0;
        int window = Math.min(forecasts.length, history.length);
        for (int i = history.length - window; i < history.length; i++) recentAvg += history[i];
        recentAvg /= window;

        double trendPct = recentAvg > 0 ? ((avgForecast - recentAvg) / recentAvg) * 100 : 0;
        String trendDesc = trendPct > 2 ? "↑ 预计改善" : trendPct < -2 ? "↓ 预计恶化" : "→ 预计保持平稳";
        String actionHint = "on_time_rate".equals(metricLabel) || "准时率".equals(metricLabel)
                ? (avgForecast >= 90 ? "服务质量优秀" : avgForecast >= 80 ? "建议关注潜在延误风险" : "建议优化物流流程以提升准时率")
                : (avgForecast >= 20 ? "延误风险较高，建议增加备货缓冲" : avgForecast >= 10 ? "延误率可控，保持当前策略" : "延误率较低，运营良好");

        double mae = computeHistoricalMAE(history, algorithm);
        String reliability = mae > 0 ? String.format("历史拟合平均误差：%.2f%%", mae) : "历史数据较少，预测仅供参考";

        return String.format(
                "基于 %s 对 %s 的预测\n" +
                "- 趋势判断：%s (%.1f%%)\n" +
                "- 预测均值：%.2f%%\n" +
                "- 预测区间：%.2f%% ~ %.2f%%\n" +
                "- %s\n" +
                "- 建议：%s",
                algorithm, metricLabel, trendDesc, trendPct, avgForecast, minF, maxF, reliability, actionHint
        );
    }

    private double computeHistoricalMAE(double[] history, String algorithm) {
        if (history.length < 4) return 0;
        // Simple back-test: fit on first 80%, predict last 20%, return MAE
        int trainN = Math.max(2, history.length - Math.max(1, history.length / 5));
        double[] train = new double[trainN];
        System.arraycopy(history, 0, train, 0, trainN);
        double[] testActual = new double[history.length - trainN];
        System.arraycopy(history, trainN, testActual, 0, testActual.length);

        double[] testPred;
        if (algorithm.contains("Holt")) {
            // Re-run Holt's with default alpha/beta for speed
            testPred = holtsForecasts(train, 0.3, 0.1, testActual.length);
        } else {
            SimpleRegression reg = new SimpleRegression();
            for (int i = 0; i < train.length; i++) reg.addData(i, train[i]);
            testPred = new double[testActual.length];
            for (int i = 0; i < testActual.length; i++) {
                testPred[i] = reg.predict(train.length + i);
            }
        }

        double mae = 0;
        for (int i = 0; i < testActual.length; i++) {
            mae += Math.abs(testActual[i] - testPred[i]);
        }
        return mae / testActual.length;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String[] toArray(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.toArray(new String[0]);
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj instanceof java.sql.Date)      return ((java.sql.Date) obj).toLocalDate();
        if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime().toLocalDate();
        return LocalDate.parse(obj.toString());
    }

    private LocalDate incrementDate(LocalDate date, String granularity, int steps) {
        return switch (granularity.toLowerCase()) {
            case "week"  -> date.plusWeeks(steps);
            case "month" -> date.plusMonths(steps);
            default      -> date.plusDays(steps);
        };
    }

    private ForecastResponse createEmptyForecast(String granularity, int periods, String msg) {
        return new ForecastResponse(granularity, periods, "N/A", BigDecimal.ZERO, new ArrayList<>(), msg);
    }

    private static class ForecastResult {
        double[] forecasts;
        String   algorithm;
        ForecastResult(double[] forecasts, String algorithm) {
            this.forecasts = forecasts;
            this.algorithm = algorithm;
        }
    }
}
