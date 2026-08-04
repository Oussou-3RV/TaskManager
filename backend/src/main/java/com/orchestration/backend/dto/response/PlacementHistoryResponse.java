package com.orchestration.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PlacementHistoryResponse {
    private Long decisionId;
    private UUID taskId;
    private BigDecimal cpuRequired;
    private Integer memoryRequiredMb;
    private String priority;
    private String submittedBy;
    private Boolean accepted;
    private String selectedNodeId;
    private String policy;
    private BigDecimal score;
    private String reason;
    private OffsetDateTime decidedAt;
}
