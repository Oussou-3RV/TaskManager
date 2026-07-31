package com.orchestration.backend.service;

import com.orchestration.backend.dto.request.TaskRequest;
import com.orchestration.backend.dto.response.PlacementDecisionResponse;
import com.orchestration.backend.dto.response.TaskResponse;
import com.orchestration.backend.entity.*;
import com.orchestration.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final MachineRepository machineRepository;
    private final PlacementRuleRepository ruleRepository;
    private final PlacementDecisionRepository decisionRepository;
    private final AuditService auditService;
    private final PlacementEngineClient placementEngineClient;

    @Transactional
    public PlacementDecisionResponse submit(TaskRequest request,
                                            String idempotencyKey,
                                            String username) {
        // 1. Verifier idempotency key
        var existing = taskRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Tache deja existante pour idempotency key: {}", idempotencyKey);
            return decisionRepository.findByTask(existing.get())
                    .map(this::toDecisionResponse)
                    .orElseThrow(() -> new RuntimeException("Decision introuvable"));
        }

        // 2. Charger l'utilisateur
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve"));

        // 3. Creer la tache avec status PENDING
        Task task = Task.builder()
                .cpuRequired(request.getCpuRequired())
                .memoryRequiredMb(request.getMemoryRequiredMb())
                .priority(request.getPriority())
                .status("PENDING")
                .submittedBy(user)
                .idempotencyKey(idempotencyKey)
                .build();

        task = taskRepository.save(task);

        auditService.log("TASK_SUBMITTED", username, "task",
                task.getId().toString(),
                "{\"cpu\":" + task.getCpuRequired() + ",\"memory\":" + task.getMemoryRequiredMb() + "}");

        // 4. Lire la regle active
        String policy = ruleRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .map(PlacementRule::getPolicy)
                .orElse("balanced");

        // 5. Lire les machines healthy
        List<Machine> machines = machineRepository.findByHealthyTrue();

        if (machines.isEmpty()) {
            return handleRejection(task, policy, username,
                    "Aucune machine disponible dans le systeme.");
        }

        // 6. Appeler le moteur Python
        PlacementEngineResult result = placementEngineClient.place(task, machines, policy);

        // 7. Sauvegarder la decision
        PlacementDecision decision = PlacementDecision.builder()
                .task(task)
                .selectedNodeId(result.getSelectedNodeId())
                .policy(result.getPolicy())
                .score(result.getScore() != null ?
                        BigDecimal.valueOf(result.getScore()) : null)
                .accepted(result.getAccepted())
                .reason(result.getReason())
                .build();

        decision = decisionRepository.save(decision);

        // 8. Mettre a jour le status de la tache
        task.setStatus(result.getAccepted() ? "PLACED" : "REJECTED");
        taskRepository.save(task);

        // 9. Audit
        String eventType = result.getAccepted() ? "TASK_PLACED" : "TASK_REJECTED";
        auditService.log(eventType, username, "task",
                task.getId().toString(),
                "{\"machine\":\"" + result.getSelectedNodeId() + "\",\"policy\":\"" + policy + "\"}");

        return toDecisionResponse(decision);
    }

    public Page<TaskResponse> getMyTasks(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve"));
        return taskRepository.findBySubmittedBy(user, pageable).map(this::toTaskResponse);
    }

    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(this::toTaskResponse);
    }

    @Transactional
    public TaskResponse cancel(UUID taskId, String username) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tache non trouvee"));

        if (!task.getSubmittedBy().getUsername().equals(username)) {
            throw new RuntimeException("Vous ne pouvez pas annuler cette tache");
        }

        if (!task.getStatus().equals("PENDING")) {
            throw new RuntimeException("Seules les taches PENDING peuvent etre annulees");
        }

        task.setStatus("CANCELLED");
        taskRepository.save(task);

        auditService.log("TASK_CANCELLED", username, "task",
                task.getId().toString(), null);

        return toTaskResponse(task);
    }

    private PlacementDecisionResponse handleRejection(Task task, String policy,
                                                      String username, String reason) {
        PlacementDecision decision = PlacementDecision.builder()
                .task(task)
                .selectedNodeId(null)
                .policy(policy)
                .score(null)
                .accepted(false)
                .reason(reason)
                .build();

        decision = decisionRepository.save(decision);
        task.setStatus("REJECTED");
        taskRepository.save(task);

        auditService.log("TASK_REJECTED", username, "task",
                task.getId().toString(),
                "{\"reason\":\"" + reason + "\"}");

        return toDecisionResponse(decision);
    }

    private TaskResponse toTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .cpuRequired(task.getCpuRequired())
                .memoryRequiredMb(task.getMemoryRequiredMb())
                .priority(task.getPriority())
                .status(task.getStatus())
                .submittedBy(task.getSubmittedBy().getUsername())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private PlacementDecisionResponse toDecisionResponse(PlacementDecision d) {
        return PlacementDecisionResponse.builder()
                .taskId(d.getTask().getId())
                .accepted(d.getAccepted())
                .selectedNodeId(d.getSelectedNodeId())
                .policy(d.getPolicy())
                .score(d.getScore())
                .reason(d.getReason())
                .decidedAt(d.getDecidedAt())
                .build();
    }
}