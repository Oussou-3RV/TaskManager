package com.orchestration.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "machines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Machine {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(name = "cpu_available", nullable = false, precision = 6, scale = 2)
    private BigDecimal cpuAvailable;

    @Column(name = "memory_available_mb", nullable = false)
    private Integer memoryAvailableMb;

    @Column(name = "latency_ms", nullable = false, precision = 8, scale = 2)
    private BigDecimal latencyMs;

    @Column(nullable = false)
    private Boolean healthy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}