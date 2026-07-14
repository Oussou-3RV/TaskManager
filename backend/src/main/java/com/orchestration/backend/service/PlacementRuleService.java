package com.orchestration.backend.service;

import com.orchestration.backend.dto.request.PlacementRuleRequest;
import com.orchestration.backend.dto.response.PlacementRuleResponse;
import com.orchestration.backend.entity.PlacementRule;
import com.orchestration.backend.entity.User;
import com.orchestration.backend.repository.PlacementRuleRepository;
import com.orchestration.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlacementRuleService {

    private final PlacementRuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public List<PlacementRuleResponse> getAll() {
        return ruleRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PlacementRuleResponse create(PlacementRuleRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve"));

        PlacementRule rule = PlacementRule.builder()
                .name(request.getName())
                .policy(request.getPolicy())
                .isActive(request.getIsActive())
                .createdBy(user)
                .build();

        PlacementRule saved = ruleRepository.save(rule);

        auditService.log("RULE_CREATED", username, "rule",
                saved.getId().toString(),
                "{\"policy\":\"" + saved.getPolicy() + "\"}");

        return toResponse(saved);
    }

    public PlacementRuleResponse toggleActive(Long id, String username) {
        PlacementRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Regle non trouvee"));

        rule.setIsActive(!rule.getIsActive());
        PlacementRule saved = ruleRepository.save(rule);

        auditService.log("RULE_UPDATED", username, "rule",
                saved.getId().toString(),
                "{\"is_active\":" + saved.getIsActive() + "}");

        return toResponse(saved);
    }

    private PlacementRuleResponse toResponse(PlacementRule rule) {
        return PlacementRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .policy(rule.getPolicy())
                .isActive(rule.getIsActive())
                .createdBy(rule.getCreatedBy().getUsername())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}