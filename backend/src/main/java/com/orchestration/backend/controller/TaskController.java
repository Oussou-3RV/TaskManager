package com.orchestration.backend.controller;

import com.orchestration.backend.dto.request.TaskRequest;
import com.orchestration.backend.dto.response.PlacementDecisionResponse;
import com.orchestration.backend.dto.response.TaskResponse;
import com.orchestration.backend.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<PlacementDecisionResponse> submit(
            @Valid @RequestBody TaskRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.accepted()
                .body(taskService.submit(request, idempotencyKey, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestParam(defaultValue = "false") boolean all,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (all) {
            return ResponseEntity.ok(taskService.getAllTasks(pageable));
        }
        return ResponseEntity.ok(taskService.getMyTasks(userDetails.getUsername(), pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        taskService.cancel(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}