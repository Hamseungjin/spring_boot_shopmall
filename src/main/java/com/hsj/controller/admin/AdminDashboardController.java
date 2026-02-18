package com.hsj.controller.admin;

import com.hsj.dto.admin.CategorySalesResponse;
import com.hsj.dto.admin.DailySalesResponse;
import com.hsj.dto.admin.DashboardResponse;
import com.hsj.dto.admin.KpiSummaryResponse;
import com.hsj.dto.common.ApiResponse;
import com.hsj.service.analytics.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DashboardResponse response = dashboardService.getDashboard(from, to);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<KpiSummaryResponse>> getKpi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        KpiSummaryResponse response = dashboardService.getKpiSummary(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/sales/daily")
    public ResponseEntity<ApiResponse<List<DailySalesResponse>>> getDailySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDailySales(from, to)));
    }

    @GetMapping("/sales/category")
    public ResponseEntity<ApiResponse<List<CategorySalesResponse>>> getCategorySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategorySales(from, to)));
    }
}
