package com.orchestration.backend.repository;
import com.orchestration.backend.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
    Page<AuditEvent> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);
}