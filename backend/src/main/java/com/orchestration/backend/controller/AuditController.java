package com.orchestration.backend.controller;

import com.orchestration.backend.dto.response.AuditEventResponse;
import com.orchestration.backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditEventResponse>> getAll(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String machine,
            @RequestParam(required = false) String policy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        if (actor != null || machine != null || policy != null || from != null || to != null) {
            return ResponseEntity.ok(auditService.search(eventType, actor, machine, policy, from, to, pageable));
        }
        if (eventType != null) {
            return ResponseEntity.ok(auditService.getByEventType(eventType, pageable));
        }
        return ResponseEntity.ok(auditService.getAll(pageable));
    }
}