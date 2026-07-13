package com.orchestration.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class MachineResponse {
    private String id;
    private String level;
    private BigDecimal cpuAvailable;
    private Integer memoryAvailableMb;
    private BigDecimal latencyMs;
    private Boolean healthy;
    private OffsetDateTime updatedAt;
}