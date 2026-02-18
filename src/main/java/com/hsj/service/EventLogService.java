package com.hsj.service;

import com.hsj.dto.event.EventLogRequest;
import com.hsj.entity.EventLog;
import com.hsj.entity.enums.EventType;
import com.hsj.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogService {

    private final EventLogRepository eventLogRepository;

    @Async("eventLogExecutor")
    @Transactional
    public void logEvent(EventLogRequest request, Long memberId, String ipAddress, String userAgent) {
        EventLog eventLog = EventLog.builder()
                .eventType(request.getEventType())
                .memberId(memberId)
                .sessionId(request.getSessionId())
                .pageUrl(request.getPageUrl())
                .referrerUrl(request.getReferrerUrl())
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .metadata(request.getMetadata())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .durationMs(request.getDurationMs())
                .build();

        eventLogRepository.save(eventLog);
        log.debug("이벤트 로그 저장: type={}, memberId={}, target={}",
                request.getEventType(), memberId, request.getTargetId());
    }

    @Async("eventLogExecutor")
    @Transactional
    public void logPageView(Long memberId, String pageUrl, String sessionId,
                            String ipAddress, String userAgent) {
        EventLog eventLog = EventLog.builder()
                .eventType(EventType.PAGE_VIEW)
                .memberId(memberId)
                .pageUrl(pageUrl)
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        eventLogRepository.save(eventLog);
    }

    @Async("eventLogExecutor")
    @Transactional
    public void logProductView(Long memberId, Long productId, String sessionId,
                               String ipAddress, String userAgent) {
        EventLog eventLog = EventLog.builder()
                .eventType(EventType.PRODUCT_VIEW)
                .memberId(memberId)
                .targetId(productId)
                .targetType("PRODUCT")
                .sessionId(sessionId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        eventLogRepository.save(eventLog);
    }

    public List<EventLog> getLogsByDateRange(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        return eventLogRepository.findByCreatedAtBetween(start, end);
    }

    public List<EventLog> getLogsByTypeAndDateRange(EventType type, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        return eventLogRepository.findByEventTypeAndCreatedAtBetween(type, start, end);
    }
}
