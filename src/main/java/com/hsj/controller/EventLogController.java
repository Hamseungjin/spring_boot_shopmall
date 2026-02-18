package com.hsj.controller;

import com.hsj.dto.common.ApiResponse;
import com.hsj.dto.event.EventLogRequest;
import com.hsj.entity.EventLog;
import com.hsj.entity.enums.EventType;
import com.hsj.security.CustomUserDetails;
import com.hsj.service.EventLogFileExporter;
import com.hsj.service.EventLogService;
import com.hsj.service.analytics.RealTimeStatsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventLogController {

    private final EventLogService eventLogService;
    private final EventLogFileExporter eventLogFileExporter;
    private final RealTimeStatsService realTimeStatsService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> logEvent(
            @Valid @RequestBody EventLogRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");

        eventLogService.logEvent(request, memberId, ip, ua);

        if (request.getSessionId() != null) {
            realTimeStatsService.recordUserOnline(request.getSessionId());
            realTimeStatsService.recordDailyVisitor(
                    LocalDate.now().toString(), request.getSessionId());
        }

        if (request.getEventType() == EventType.PRODUCT_VIEW && request.getTargetId() != null) {
            realTimeStatsService.incrementProductView(request.getTargetId());
        }

        return ResponseEntity.ok(ApiResponse.ok("이벤트가 기록되었습니다."));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> logEvents(
            @Valid @RequestBody List<EventLogRequest> requests,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {

        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");

        for (EventLogRequest request : requests) {
            eventLogService.logEvent(request, memberId, ip, ua);
        }

        return ResponseEntity.ok(ApiResponse.ok(requests.size() + "건의 이벤트가 기록되었습니다."));
    }

    @GetMapping("/realtime/online")
    public ResponseEntity<ApiResponse<Long>> getOnlineUserCount() {
        return ResponseEntity.ok(ApiResponse.ok(realTimeStatsService.getOnlineUserCount()));
    }

    @GetMapping("/realtime/product/{productId}/views")
    public ResponseEntity<ApiResponse<Long>> getProductViewCount(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(realTimeStatsService.getProductViewCount(productId)));
    }

    @GetMapping("/export")
    public void exportLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) EventType eventType,
            HttpServletResponse response) throws IOException {

        List<EventLog> logs;
        if (eventType != null) {
            logs = eventLogService.getLogsByTypeAndDateRange(eventType, from, to);
        } else {
            logs = eventLogService.getLogsByDateRange(from, to);
        }

        String filename = String.format("event_logs_%s_%s.csv", from, to);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setCharacterEncoding("UTF-8");

        eventLogFileExporter.exportToCsv(logs, response.getWriter());
    }
}
