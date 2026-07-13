package com.orchestration.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class AuditEventResponse {
    private Long id;
    private String eventType;
    private String actor;
    private String entityType;
    private String entityId;
    private String details;
    private OffsetDateTime createdAt;
}