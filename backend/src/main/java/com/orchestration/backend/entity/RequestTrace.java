package com.orchestration.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "request_traces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 36)
    private String traceId;

    @Column(nullable = false, length = 150)
    private String endpoint;

    @Column(name = "api_ms", nullable = false)
    private Double apiMs;

    @Column(name = "business_ms", nullable = false)
    private Double businessMs;

    @Column(name = "db_ms", nullable = false)
    private Double dbMs;

    @Column(name = "total_ms", nullable = false)
    private Double totalMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
