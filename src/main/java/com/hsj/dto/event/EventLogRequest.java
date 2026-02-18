package com.hsj.dto.event;

import com.hsj.entity.enums.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EventLogRequest {

    @NotNull(message = "이벤트 타입은 필수입니다.")
    private EventType eventType;

    private String sessionId;
    private String pageUrl;
    private String referrerUrl;
    private Long targetId;
    private String targetType;
    private String metadata;
    private Long durationMs;
}
