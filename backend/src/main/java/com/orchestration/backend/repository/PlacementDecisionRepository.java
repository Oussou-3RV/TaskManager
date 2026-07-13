package com.orchestration.backend.repository;

import com.orchestration.backend.entity.PlacementDecision;
import com.orchestration.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacementDecisionRepository extends JpaRepository<PlacementDecision, Long> {
    Optional<PlacementDecision> findByTask(Task task);
}
