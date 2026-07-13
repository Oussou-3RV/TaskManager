package com.orchestration.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResponse {
    private UUID id;
    private BigDecimal cpuRequired;
    private Integer memoryRequiredMb;
    private String priority;
    private String status;
    private String submittedBy;
    private OffsetDateTime createdAt;
}