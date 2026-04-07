package com.logistics.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class ForecastData {
    private LocalDate date;
    private Long value;
    @JsonProperty("isForecast")
    private boolean isForecast;

    public ForecastData() {
    }

    public ForecastData(LocalDate date, Long value, boolean isForecast) {
        this.date = date;
        this.value = value;
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

    public boolean isForecast() {
        return isForecast;
    }

    public void setForecast(boolean forecast) {
        isForecast = forecast;
    }
}