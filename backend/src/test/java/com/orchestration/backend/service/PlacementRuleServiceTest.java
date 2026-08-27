package com.orchestration.backend.service;

import com.orchestration.backend.dto.request.PlacementRuleRequest;
import com.orchestration.backend.dto.response.PlacementRuleResponse;
import com.orchestration.backend.entity.PlacementRule;
import com.orchestration.backend.entity.User;
import com.orchestration.backend.repository.PlacementRuleRepository;
import com.orchestration.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementRuleServiceTest {

    @Mock
    private PlacementRuleRepository ruleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PlacementRuleService ruleService;

    @Test
    void create_withUnknownUser_throwsUsernameNotFound() {
        PlacementRuleRequest request = new PlacementRuleRequest();
        request.setName("Regle equilibree");
        request.setPolicy("balanced");
        when(userRepository.findByUsername("inconnu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.create(request, "inconnu"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void create_persistsRuleAndLogsAudit() {
        User admin = User.builder().id(1L).username("admin").role("ADMIN").build();
        PlacementRuleRequest request = new PlacementRuleRequest();
        request.setName("Regle equilibree");
        request.setPolicy("balanced");
        request.setIsActive(true);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(ruleRepository.save(any(PlacementRule.class))).thenAnswer(inv -> {
            PlacementRule r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        PlacementRuleResponse response = ruleService.create(request, "admin");

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getPolicy()).isEqualTo("balanced");
        assertThat(response.getCreatedBy()).isEqualTo("admin");
        verify(auditService).log("RULE_CREATED", "admin", "rule", "10",
                "{\"policy\":\"balanced\"}");
    }

    @Test
    void toggleActive_flipsIsActiveFlag() {
        PlacementRule rule = PlacementRule.builder()
                .id(5L)
                .name("Regle")
                .policy("least_loaded")
                .isActive(true)
                .createdBy(User.builder().username("admin").build())
                .build();
        when(ruleRepository.findById(5L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(PlacementRule.class))).thenAnswer(inv -> inv.getArgument(0));

        PlacementRuleResponse response = ruleService.toggleActive(5L, "admin");

        assertThat(response.getIsActive()).isFalse();
        verify(auditService).log("RULE_UPDATED", "admin", "rule", "5",
                "{\"is_active\":false}");
    }

    @Test
    void toggleActive_whenRuleMissing_throws() {
        when(ruleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.toggleActive(99L, "admin"))
                .isInstanceOf(RuntimeException.class);
    }
}
