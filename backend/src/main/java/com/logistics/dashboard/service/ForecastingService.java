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

            // Select algorithm
            ForecastResult result;
            if (values.length >= 4) {
                result = holtsDoubleExponentialSmoothing(values, periods);
            } else if (values.length >= 2) {
                result = sesWithBestAlpha(values, periods);
            } else {
                result = linearRegression(values, periods);
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
        String trendDesc = trendPct > 2 ? "↑ 上升趋势" : trendPct < -2 ? "↓ 下降趋势" : "→ 平稳";

        return String.format(
                "基于 %s：\n" +
                "- 趋势判断：%s (%.1f%%)\n" +
                "- 预测均值：%.0f 单/期\n" +
                "- 预测区间：%.0f ~ %.0f 单\n" +
                "- 建议备货量（安全库存20%%缓冲）：%.0f 单/期",
                algorithm, trendDesc, trendPct, avgForecast, minF, maxF, avgForecast * 1.2
        );
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
