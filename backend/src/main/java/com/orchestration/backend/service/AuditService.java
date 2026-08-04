package com.orchestration.backend.service;

import com.orchestration.backend.dto.response.AuditEventResponse;
import com.orchestration.backend.entity.AuditEvent;
import com.orchestration.backend.entity.User;
import com.orchestration.backend.repository.AuditEventRepository;
import com.orchestration.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public void log(String eventType, String username, String entityType, String entityId, String details) {
        User actor = userRepository.findByUsername(username).orElse(null);

        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .actor(actor)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditEventRepository.save(event);
    }

    public Page<AuditEventResponse> getAll(Pageable pageable) {
        return auditEventRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<AuditEventResponse> getByEventType(String eventType, Pageable pageable) {
        return auditEventRepository.findByEventType(eventType, pageable).map(this::toResponse);
    }

    public Page<AuditEventResponse> search(String eventType, String actor, String machine, String policy,
                                            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        // Requete native : le tri doit utiliser le nom de colonne SQL (created_at),
        // pas le nom de propriete Java (createdAt) injecte par defaut par Pageable.
        Pageable nativePageable = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "created_at"));
        return auditEventRepository.search(eventType, actor, machine, policy, from, to, nativePageable)
                .map(this::toResponse);
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .actor(event.getActor() != null ? event.getActor().getUsername() : "system")
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}