package com.orchestration.backend.repository;

import com.orchestration.backend.entity.PlacementRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlacementRuleRepository extends JpaRepository<PlacementRule, Long> {
    Optional<PlacementRule> findFirstByIsActiveTrueOrderByCreatedAtDesc();

}
