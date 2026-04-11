package com.logistics.dashboard.service;

import com.logistics.dashboard.dto.ForecastData;
import com.logistics.dashboard.dto.ForecastResponse;
import com.logistics.dashboard.dto.TimeSeriesData;
import com.logistics.dashboard.dto.TimeSeriesResponse;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForecastingService {

    private static final Logger log = LoggerFactory.getLogger(ForecastingService.class);
    private final DashboardService dashboardService;

    public ForecastingService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Forecast future order volume using exponential smoothing
     */
    public ForecastResponse forecastDemand(String granularity, int periods,
                                           LocalDate startDate, LocalDate endDate,
                                           List<String> carriers, List<String> regions) {
        log.info("预测需求：粒度 {}，周期数 {}，时间范围 {} 到 {}",
                granularity, periods, startDate, endDate);

        try {
            // Get historical data
            TimeSeriesResponse historicalData = dashboardService.getOrderVolumeTimeSeries(
                    granularity, startDate, endDate, carriers, regions);

            if (historicalData.getData().isEmpty()) {
                return createEmptyForecast(granularity, periods,
                        "无历史数据可用于预测");
            }

            // Convert to double array for analysis
            double[] values = historicalData.getData().stream()
                    .mapToDouble(d -> d.getCount().doubleValue())
                    .toArray();

            // Try exponential smoothing first
            ForecastResult expResult = tryExponentialSmoothing(values, periods);

            List<ForecastData> forecastData = new ArrayList<>();

            // Add historical data (not forecast)
            for (int i = 0; i < historicalData.getData().size(); i++) {
                TimeSeriesData tsData = historicalData.getData().get(i);
                forecastData.add(new ForecastData(tsData.getDate(), tsData.getCount(), false));
            }

            // Add forecast data
            LocalDate lastDate = historicalData.getData().get(historicalData.getData().size() - 1).getDate();
            for (int i = 0; i < expResult.forecasts.length; i++) {
                LocalDate forecastDate = incrementDateByGranularity(lastDate, granularity, i + 1);
                forecastData.add(new ForecastData(forecastDate, Math.round(expResult.forecasts[i]), true));
            }

            // Generate recommendations
            String recommendations = generateRecommendations(expResult.forecasts, expResult.algorithm);

            return new ForecastResponse(
                    granularity,
                    periods,
                    expResult.algorithm,
                    new BigDecimal("1.20"), // Safety stock multiplier
                    forecastData,
                    recommendations
            );

        } catch (Exception e) {
            log.error("预测出错: {}", e.getMessage(), e);
            return createErrorForecast(granularity, periods,
                    "预测失败: " + e.getMessage());
        }
    }

    /**
     * Try exponential smoothing, fall back to linear regression if it fails
     */
    private ForecastResult tryExponentialSmoothing(double[] values, int periods) {
        // Simple Exponential Smoothing (SES) implementation
        // We'll use a grid search to find best alpha

        if (values.length < 3) {
            log.warn("指数平滑数据不足 (n={})，使用线性回归", values.length);
            return tryLinearRegression(values, periods);
        }

        double bestAlpha = 0.3; // default
        double bestMSE = Double.MAX_VALUE;

        // Grid search for alpha (0.1 to 0.9, step 0.1)
        for (double alpha = 0.1; alpha <= 0.9; alpha += 0.1) {
            try {
                double mse = calculateMSE(values, alpha);
                if (mse < bestMSE) {
                    bestMSE = mse;
                    bestAlpha = alpha;
                }
            } catch (Exception e) {
                log.debug("Alpha {} failed: {}", alpha, e.getMessage());
            }
        }

        log.info("选择的alpha={}，MSE={}", bestAlpha, bestMSE);

        // Generate forecasts using best alpha
        double[] forecasts = generateSESForecasts(values, bestAlpha, periods);

        return new ForecastResult(forecasts, "指数平滑法 (alpha=" +
                String.format("%.2f", bestAlpha) + ")");
    }

    /**
     * Calculate Mean Squared Error for given alpha
     * Uses last 20% of data for validation
     */
    private double calculateMSE(double[] values, double alpha) {
        int validationSize = Math.max(1, (int) (values.length * 0.2));
        int trainingSize = values.length - validationSize;

        if (trainingSize < 2) {
            return Double.MAX_VALUE; // Not enough training data
        }

        double[] training = new double[trainingSize];
        System.arraycopy(values, 0, training, 0, trainingSize);

        // Generate one-step-ahead forecasts for training data
        double[] forecasts = generateSESForecasts(training, alpha, validationSize);

        // Calculate MSE on validation set
        double sumSquaredError = 0;
        for (int i = 0; i < validationSize; i++) {
            double error = values[trainingSize + i] - forecasts[i];
            sumSquaredError += error * error;
        }

        return sumSquaredError / validationSize;
    }

    /**
     * Generate forecasts using Simple Exponential Smoothing
     */
    private double[] generateSESForecasts(double[] values, double alpha, int periods) {
        // SES formula: F_t+1 = alpha * Y_t + (1 - alpha) * F_t
        double[] forecasts = new double[periods];

        if (values.length == 0) {
            return forecasts;
        }

        // Initialize with first value
        double forecast = values[0];

        // Calculate smoothed values for historical data
        for (double value : values) {
            forecast = alpha * value + (1 - alpha) * forecast;
        }

        // Generate forecasts
        for (int i = 0; i < periods; i++) {
            forecasts[i] = forecast;
            // For SES, forecast remains constant for all future periods
        }

        return forecasts;
    }

    /**
     * Fallback method: linear regression
     */
    private ForecastResult tryLinearRegression(double[] values, int periods) {
        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < values.length; i++) {
            regression.addData(i, values[i]);
        }

        double[] forecasts = new double[periods];
        for (int i = 0; i < periods; i++) {
            forecasts[i] = regression.predict(values.length + i);
        }

        return new ForecastResult(forecasts, "线性回归");
    }

    /**
     * Increment date based on granularity
     */
    private LocalDate incrementDateByGranularity(LocalDate date, String granularity, int steps) {
        switch (granularity.toLowerCase()) {
            case "day":
                return date.plusDays(steps);
            case "week":
                return date.plusWeeks(steps);
            case "month":
                return date.plusMonths(steps);
            default:
                return date.plusDays(steps);
        }
    }

    /**
     * Generate inventory recommendations based on forecasts
     */
    private String generateRecommendations(double[] forecasts, String algorithm) {
        if (forecasts.length == 0) {
            return "未生成预测。";
        }

        double avgForecast = 0;
        for (double f : forecasts) {
            avgForecast += f;
        }
        avgForecast /= forecasts.length;

        double safetyStock = avgForecast * 1.2;
        double minForecast = Double.MAX_VALUE;
        double maxForecast = Double.MIN_VALUE;

        for (double f : forecasts) {
            if (f < minForecast) minForecast = f;
            if (f > maxForecast) maxForecast = f;
        }

        return String.format(
                "基于%s预测：\n" +
                "- 平均预期需求：%.0f 订单\n" +
                "- 建议安全库存：%.0f 订单 (20%% 缓冲)\n" +
                "- 预测范围：%.0f 到 %.0f 订单\n" +
                "- 请相应调整库存水平。",
                algorithm, avgForecast, safetyStock, minForecast, maxForecast
        );
    }

    /**
     * Create empty forecast response
     */
    private ForecastResponse createEmptyForecast(String granularity, int periods, String message) {
        return new ForecastResponse(
                granularity,
                periods,
                "None",
                BigDecimal.ZERO,
                new ArrayList<>(),
                message
        );
    }

    /**
     * Create error forecast response
     */
    private ForecastResponse createErrorForecast(String granularity, int periods, String message) {
        return new ForecastResponse(
                granularity,
                periods,
                "Error",
                BigDecimal.ZERO,
                new ArrayList<>(),
                message
        );
    }

    /**
     * Calculate safety stock recommendation (forecast * 1.2)
     */
    public BigDecimal calculateSafetyStockRecommendation(BigDecimal forecastValue) {
        if (forecastValue == null || forecastValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return forecastValue.multiply(new BigDecimal("1.20"));
    }

    /**
     * Helper class to store forecast results
     */
    private static class ForecastResult {
        double[] forecasts;
        String algorithm;

        ForecastResult(double[] forecasts, String algorithm) {
            this.forecasts = forecasts;
            this.algorithm = algorithm;
        }
    }
}