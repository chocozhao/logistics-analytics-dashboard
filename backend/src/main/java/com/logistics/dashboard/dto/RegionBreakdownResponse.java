package com.logistics.dashboard.dto;

import java.util.List;

public class RegionBreakdownResponse {
    private List<RegionBreakdown> data;

    public RegionBreakdownResponse() {}

    public RegionBreakdownResponse(List<RegionBreakdown> data) {
        this.data = data;
    }

    public List<RegionBreakdown> getData() { return data; }
    public void setData(List<RegionBreakdown> data) { this.data = data; }
}
