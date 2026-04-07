package com.logistics.dashboard.dto;

import java.util.List;
import java.math.BigDecimal;

public class ForecastResponse {
    private String granularity;
    private int forecastPeriods;
    private String algorithm;
    private BigDecimal safetyStockMultiplier;
    private List<ForecastData> data;
    private String recommendations;

    public ForecastResponse() {
    }

    public ForecastResponse(String granularity, int forecastPeriods, String algorithm,
                           BigDecimal safetyStockMultiplier, List<ForecastData> data, String recommendations) {
        this.granularity = granularity;
        this.forecastPeriods = forecastPeriods;
        this.algorithm = algorithm;
        this.safetyStockMultiplier = safetyStockMultiplier;
        this.data = data;
        this.recommendations = recommendations;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }

    public int getForecastPeriods() {
        return forecastPeriods;
    }

    public void setForecastPeriods(int forecastPeriods) {
        this.forecastPeriods = forecastPeriods;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public BigDecimal getSafetyStockMultiplier() {
        return safetyStockMultiplier;
    }

    public void setSafetyStockMultiplier(BigDecimal safetyStockMultiplier) {
        this.safetyStockMultiplier = safetyStockMultiplier;
    }

    public List<ForecastData> getData() {
        return data;
    }

    public void setData(List<ForecastData> data) {
        this.data = data;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }
}