package com.logistics.dashboard.ai.model;

import java.time.LocalDate;
import java.util.List;

public class QueryRequest {
    private String question;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> carriers;
    private List<String> regions;
    private List<String> categories;

    public QueryRequest() {}
    public QueryRequest(String question) { this.question = question; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<String> getCarriers() { return carriers; }
    public void setCarriers(List<String> carriers) { this.carriers = carriers; }

    public List<String> getRegions() { return regions; }
    public void setRegions(List<String> regions) { this.regions = regions; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
}