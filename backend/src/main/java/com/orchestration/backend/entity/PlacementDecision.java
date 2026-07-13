package com.orchestration.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "placement_decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "selected_node_id", length = 50)
    private String selectedNodeId;

    @Column(nullable = false, length = 20)
    private String policy;

    @Column(precision = 10, scale = 6)
    private BigDecimal score;

    @Column(nullable = false)
    private Boolean accepted;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

    @PrePersist
    protected void onCreate() {
        this.decidedAt = OffsetDateTime.now();
    }
}