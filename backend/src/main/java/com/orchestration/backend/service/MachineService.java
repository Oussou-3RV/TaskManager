package com.orchestration.backend.service;

import com.orchestration.backend.dto.response.MachineResponse;
import com.orchestration.backend.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;

    public List<MachineResponse> getAll() {
        return machineRepository.findAll().stream().map(m ->
                MachineResponse.builder()
                        .id(m.getId())
                        .level(m.getLevel())
                        .cpuAvailable(m.getCpuAvailable())
                        .memoryAvailableMb(m.getMemoryAvailableMb())
                        .latencyMs(m.getLatencyMs())
                        .healthy(m.getHealthy())
                        .updatedAt(m.getUpdatedAt())
                        .build()
        ).toList();
    }

    public List<MachineResponse> getHealthy() {
        return machineRepository.findByHealthyTrue().stream().map(m ->
                MachineResponse.builder()
                        .id(m.getId())
                        .level(m.getLevel())
                        .cpuAvailable(m.getCpuAvailable())
                        .memoryAvailableMb(m.getMemoryAvailableMb())
                        .latencyMs(m.getLatencyMs())
                        .healthy(m.getHealthy())
                        .updatedAt(m.getUpdatedAt())
                        .build()
        ).toList();
    }
}