package com.hsj.repository;

import com.hsj.entity.EventLog;
import com.hsj.entity.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    Page<EventLog> findByEventType(EventType eventType, Pageable pageable);

    List<EventLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<EventLog> findByEventTypeAndCreatedAtBetween(EventType eventType,
                                                       LocalDateTime from, LocalDateTime to);

    long countByEventTypeAndCreatedAtBetween(EventType eventType,
                                              LocalDateTime from, LocalDateTime to);
}
