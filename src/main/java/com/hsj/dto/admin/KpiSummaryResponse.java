package com.hsj.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class KpiSummaryResponse {

    private BigDecimal totalRevenue;
    private long totalOrders;
    private long paidOrders;
    private long newMembers;
    private long totalVisitors;
    private double conversionRate;
    private long onlineUsers;
}
