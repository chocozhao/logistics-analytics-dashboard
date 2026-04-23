package com.logistics.dashboard.dto;

import java.util.List;

public class CategoryBreakdownResponse {
    private List<CategoryBreakdown> data;

    public CategoryBreakdownResponse() {}

    public CategoryBreakdownResponse(List<CategoryBreakdown> data) {
        this.data = data;
    }

    public List<CategoryBreakdown> getData() { return data; }
    public void setData(List<CategoryBreakdown> data) { this.data = data; }
}
