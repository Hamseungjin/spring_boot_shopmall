package com.hsj.dto.event;

import com.hsj.entity.EventLog;
import com.hsj.entity.enums.EventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventLogResponse {

    private Long id;
    private EventType eventType;
    private Long memberId;
    private String sessionId;
    private String pageUrl;
    private Long targetId;
    private String targetType;
    private String metadata;
    private Long durationMs;
    private LocalDateTime createdAt;

    public static EventLogResponse from(EventLog log) {
        return EventLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .memberId(log.getMemberId())
                .sessionId(log.getSessionId())
                .pageUrl(log.getPageUrl())
                .targetId(log.getTargetId())
                .targetType(log.getTargetType())
                .metadata(log.getMetadata())
                .durationMs(log.getDurationMs())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
