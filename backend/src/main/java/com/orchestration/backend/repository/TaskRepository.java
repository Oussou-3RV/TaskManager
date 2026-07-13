package com.orchestration.backend.repository;

import com.orchestration.backend.entity.Task;
import com.orchestration.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByIdempotencyKey(String idempotencyKey);
    Page<Task> findBySubmittedBy(User user, Pageable pageable);
    Page<Task> findByStatus(String status, Pageable pageable);
}
