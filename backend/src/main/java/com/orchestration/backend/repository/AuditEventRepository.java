package com.orchestration.backend.repository;
import com.orchestration.backend.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
    Page<AuditEvent> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);

    // machine et policy sont extraits du champ jsonb `details` (pas de colonne dediee) ;
    // seuls les evenements TASK_PLACED/TASK_REJECTED les renseignent.
    @Query(
            value = "SELECT e.* FROM audit_events e " +
                    "LEFT JOIN users u ON u.id = e.actor_id " +
                    "WHERE (CAST(:eventType AS text) IS NULL OR e.event_type = CAST(:eventType AS text)) " +
                    "AND (CAST(:actor AS text) IS NULL OR u.username = CAST(:actor AS text)) " +
                    "AND (CAST(:machine AS text) IS NULL OR e.details ->> 'machine' = CAST(:machine AS text)) " +
                    "AND (CAST(:policy AS text) IS NULL OR e.details ->> 'policy' = CAST(:policy AS text)) " +
                    "AND (CAST(:from AS timestamptz) IS NULL OR e.created_at >= CAST(:from AS timestamptz)) " +
                    "AND (CAST(:to AS timestamptz) IS NULL OR e.created_at <= CAST(:to AS timestamptz))",
            countQuery = "SELECT count(*) FROM audit_events e " +
                    "LEFT JOIN users u ON u.id = e.actor_id " +
                    "WHERE (CAST(:eventType AS text) IS NULL OR e.event_type = CAST(:eventType AS text)) " +
                    "AND (CAST(:actor AS text) IS NULL OR u.username = CAST(:actor AS text)) " +
                    "AND (CAST(:machine AS text) IS NULL OR e.details ->> 'machine' = CAST(:machine AS text)) " +
                    "AND (CAST(:policy AS text) IS NULL OR e.details ->> 'policy' = CAST(:policy AS text)) " +
                    "AND (CAST(:from AS timestamptz) IS NULL OR e.created_at >= CAST(:from AS timestamptz)) " +
                    "AND (CAST(:to AS timestamptz) IS NULL OR e.created_at <= CAST(:to AS timestamptz))",
            nativeQuery = true)
    Page<AuditEvent> search(
            @Param("eventType") String eventType,
            @Param("actor") String actor,
            @Param("machine") String machine,
            @Param("policy") String policy,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}