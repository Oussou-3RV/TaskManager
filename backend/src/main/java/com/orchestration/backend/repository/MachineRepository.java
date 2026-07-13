package com.orchestration.backend.repository;

import com.orchestration.backend.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, String> {
    List<Machine> findByHealthyTrue();
    List<Machine> findByLevel(String level);
}
