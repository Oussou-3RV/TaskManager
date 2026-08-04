package com.orchestration.backend.repository;

import com.orchestration.backend.entity.PlacementDecision;
import com.orchestration.backend.entity.Task;
import com.orchestration.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacementDecisionRepository extends JpaRepository<PlacementDecision, Long> {
    Optional<PlacementDecision> findByTask(Task task);
    Page<PlacementDecision> findByTask_SubmittedBy(User user, Pageable pageable);
    Page<PlacementDecision> findByAccepted(Boolean accepted, Pageable pageable);
}
