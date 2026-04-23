package com.logistics.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ForecastData {
    private LocalDate date;
    private Long value;
    private BigDecimal pctValue; // for metric forecasts like on-time rate (percentage)
    @JsonProperty("isForecast")
    private boolean isForecast;

    public ForecastData() {
    }

    public ForecastData(LocalDate date, Long value, boolean isForecast) {
        this.date = date;
        this.value = value;
        this.isForecast = isForecast;
    }

    public ForecastData(LocalDate date, BigDecimal pctValue, boolean isForecast) {
        this.date = date;
        this.pctValue = pctValue;
        this.isForecast = isForecast;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public BigDecimal getPctValue() {
        return pctValue;
    }

    public void setPctValue(BigDecimal pctValue) {
        this.pctValue = pctValue;
    }

    public boolean isForecast() {
        return isForecast;
    }

    public void setForecast(boolean forecast) {
        isForecast = forecast;
    }
}