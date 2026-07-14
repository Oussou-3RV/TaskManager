package com.orchestration.backend.controller;

import com.orchestration.backend.dto.request.PlacementRuleRequest;
import com.orchestration.backend.dto.response.PlacementRuleResponse;
import com.orchestration.backend.service.PlacementRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class PlacementRuleController {

    private final PlacementRuleService ruleService;

    @GetMapping
    public ResponseEntity<List<PlacementRuleResponse>> getAll() {
        return ResponseEntity.ok(ruleService.getAll());
    }

    @PostMapping
    public ResponseEntity<PlacementRuleResponse> create(
            @Valid @RequestBody PlacementRuleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ruleService.create(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<PlacementRuleResponse> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ruleService.toggleActive(id, userDetails.getUsername()));
    }
}