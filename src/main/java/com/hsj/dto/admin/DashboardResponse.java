package com.hsj.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardResponse {

    private KpiSummaryResponse kpi;
    private List<DailySalesResponse> dailySales;
    private List<CategorySalesResponse> categorySales;
}
