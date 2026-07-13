package com.orchestration.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PlacementRuleResponse {
    private Long id;
    private String name;
    private String policy;
    private Boolean isActive;
    private String createdBy;
    private OffsetDateTime createdAt;
}