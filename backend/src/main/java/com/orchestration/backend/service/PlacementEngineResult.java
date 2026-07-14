package com.orchestration.backend.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlacementEngineResult {
    private Boolean accepted;
    private String selectedNodeId;
    private String policy;
    private Double score;
    private String reason;
    private Long latencyMs;
}