package com.logistics.dashboard.dto;

import java.util.List;

public class CarrierBreakdownResponse {
    private List<CarrierBreakdown> data;

    // Constructors
    public CarrierBreakdownResponse() {}

    public CarrierBreakdownResponse(List<CarrierBreakdown> data) {
        this.data = data;
    }

    // Getters and setters
    public List<CarrierBreakdown> getData() { return data; }
    public void setData(List<CarrierBreakdown> data) { this.data = data; }
}