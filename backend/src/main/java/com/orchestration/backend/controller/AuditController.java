package com.orchestration.backend.controller;

import com.orchestration.backend.dto.response.AuditEventResponse;
import com.orchestration.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditEventResponse>> getAll(
            @RequestParam(required = false) String eventType,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        if (eventType != null) {
            return ResponseEntity.ok(auditService.getByEventType(eventType, pageable));
        }
        return ResponseEntity.ok(auditService.getAll(pageable));
    }
}