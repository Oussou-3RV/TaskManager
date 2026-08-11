package com.orchestration.backend.repository;

import com.orchestration.backend.entity.RequestTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestTraceRepository extends JpaRepository<RequestTrace, Long> {
}
