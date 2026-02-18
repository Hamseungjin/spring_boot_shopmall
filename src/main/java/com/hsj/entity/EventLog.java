package com.hsj.entity;

import com.hsj.entity.enums.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_logs", indexes = {
        @Index(name = "idx_event_log_type", columnList = "event_type"),
        @Index(name = "idx_event_log_created_at", columnList = "created_at"),
        @Index(name = "idx_event_log_member", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_log_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "referrer_url", length = 500)
    private String referrerUrl;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "duration_ms")
    private Long durationMs;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public EventLog(EventType eventType, Long memberId, String sessionId,
                    String pageUrl, String referrerUrl, Long targetId, String targetType,
                    String metadata, String ipAddress, String userAgent, Long durationMs) {
        this.eventType = eventType;
        this.memberId = memberId;
        this.sessionId = sessionId;
        this.pageUrl = pageUrl;
        this.referrerUrl = referrerUrl;
        this.targetId = targetId;
        this.targetType = targetType;
        this.metadata = metadata;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.durationMs = durationMs;
    }
}
