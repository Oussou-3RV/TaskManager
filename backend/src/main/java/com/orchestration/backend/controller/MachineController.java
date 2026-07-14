package com.orchestration.backend.controller;

import com.orchestration.backend.dto.response.MachineResponse;
import com.orchestration.backend.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @GetMapping
    public ResponseEntity<List<MachineResponse>> getAll(
            @RequestParam(required = false) Boolean healthyOnly) {
        if (Boolean.TRUE.equals(healthyOnly)) {
            return ResponseEntity.ok(machineService.getHealthy());
        }
        return ResponseEntity.ok(machineService.getAll());
    }
}