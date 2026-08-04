package com.orchestration.backend.controller;

import com.orchestration.backend.dto.response.PlacementHistoryResponse;
import com.orchestration.backend.service.PlacementDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/placements")
@RequiredArgsConstructor
public class PlacementDecisionController {

    private final PlacementDecisionService decisionService;

    @GetMapping
    public ResponseEntity<Page<PlacementHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "false") boolean all,
            @RequestParam(required = false) Boolean accepted,
            @PageableDefault(size = 20, sort = "decidedAt") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (accepted != null) {
            return ResponseEntity.ok(decisionService.getByAccepted(accepted, pageable));
        }
        if (all) {
            return ResponseEntity.ok(decisionService.getAll(pageable));
        }
        return ResponseEntity.ok(decisionService.getMine(userDetails.getUsername(), pageable));
    }
}
