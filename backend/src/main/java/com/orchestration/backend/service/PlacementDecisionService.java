package com.orchestration.backend.service;

import com.orchestration.backend.dto.response.PlacementHistoryResponse;
import com.orchestration.backend.entity.PlacementDecision;
import com.orchestration.backend.entity.User;
import com.orchestration.backend.repository.PlacementDecisionRepository;
import com.orchestration.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlacementDecisionService {

    private final PlacementDecisionRepository decisionRepository;
    private final UserRepository userRepository;

    public Page<PlacementHistoryResponse> getAll(Pageable pageable) {
        return decisionRepository.findAll(pageable).map(this::toResponse);
    }

    public Page<PlacementHistoryResponse> getMine(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve"));
        return decisionRepository.findByTask_SubmittedBy(user, pageable).map(this::toResponse);
    }

    public Page<PlacementHistoryResponse> getByAccepted(boolean accepted, Pageable pageable) {
        return decisionRepository.findByAccepted(accepted, pageable).map(this::toResponse);
    }

    private PlacementHistoryResponse toResponse(PlacementDecision d) {
        return PlacementHistoryResponse.builder()
                .decisionId(d.getId())
                .taskId(d.getTask().getId())
                .cpuRequired(d.getTask().getCpuRequired())
                .memoryRequiredMb(d.getTask().getMemoryRequiredMb())
                .priority(d.getTask().getPriority())
                .submittedBy(d.getTask().getSubmittedBy().getUsername())
                .accepted(d.getAccepted())
                .selectedNodeId(d.getSelectedNodeId())
                .policy(d.getPolicy())
                .score(d.getScore())
                .reason(d.getReason())
                .decidedAt(d.getDecidedAt())
                .build();
    }
}
