package com.hsj.service.analytics;

import com.hsj.dto.admin.*;
import com.hsj.repository.custom.AdminStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final AdminStatsRepository adminStatsRepository;
    private final RealTimeStatsService realTimeStatsService;

    public DashboardResponse getDashboard(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);

        KpiSummaryResponse kpi = getKpiSummary(start, end);
        List<DailySalesResponse> dailySales = adminStatsRepository.getDailySales(from, to);
        List<CategorySalesResponse> categorySales = adminStatsRepository.getCategorySales(start, end);

        return DashboardResponse.builder()
                .kpi(kpi)
                .dailySales(dailySales)
                .categorySales(categorySales)
                .build();
    }

    public KpiSummaryResponse getKpiSummary(LocalDateTime from, LocalDateTime to) {
        BigDecimal totalRevenue = adminStatsRepository.getTotalRevenue(from, to);
        long totalOrders = adminStatsRepository.getTotalOrderCount(from, to);
        long paidOrders = adminStatsRepository.getPaidOrderCount(from, to);
        long newMembers = adminStatsRepository.getNewMemberCount(from, to);
        long totalVisitors = adminStatsRepository.getTotalVisitorCount(from, to);
        long onlineUsers = realTimeStatsService.getOnlineUserCount();

        double conversionRate = 0.0;
        if (totalVisitors > 0) {
            conversionRate = BigDecimal.valueOf(paidOrders)
                    .divide(BigDecimal.valueOf(totalVisitors), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return KpiSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .newMembers(newMembers)
                .totalVisitors(totalVisitors)
                .conversionRate(conversionRate)
                .onlineUsers(onlineUsers)
                .build();
    }

    public List<DailySalesResponse> getDailySales(LocalDate from, LocalDate to) {
        return adminStatsRepository.getDailySales(from, to);
    }

    public List<CategorySalesResponse> getCategorySales(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        return adminStatsRepository.getCategorySales(start, end);
    }
}
