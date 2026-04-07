package com.logistics.dashboard.ai.model;

import com.logistics.dashboard.dto.*;
import java.util.List;
import java.util.Map;

public class QueryResponse {
    private String answer;
    private String chartType; // "time_series", "bar", "pie", "kpi", "none"
    private Object chartData; // Could be TimeSeriesResponse, CarrierBreakdownResponse, etc.
    private String explanation;
    private Map<String, Object> filters;
    private List<String> metrics;
    private String queryPlan;
    private List<Map<String, Object>> rawData;
    private String error;

    // Constructors
    public QueryResponse() {}

    public QueryResponse(String answer, String explanation) {
        this.answer = answer;
        this.explanation = explanation;
        this.chartType = "none";
    }

    public QueryResponse(String answer, String chartType, Object chartData, String explanation) {
        this.answer = answer;
        this.chartType = chartType;
        this.chartData = chartData;
        this.explanation = explanation;
    }

    // Getters and setters
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    public Object getChartData() {
        return chartData;
    }

    public void setChartData(Object chartData) {
        this.chartData = chartData;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public String getQueryPlan() {
        return queryPlan;
    }

    public void setQueryPlan(String queryPlan) {
        this.queryPlan = queryPlan;
    }

    public List<Map<String, Object>> getRawData() {
        return rawData;
    }

    public void setRawData(List<Map<String, Object>> rawData) {
        this.rawData = rawData;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}