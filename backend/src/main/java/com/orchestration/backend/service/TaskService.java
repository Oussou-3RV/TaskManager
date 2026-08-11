package com.orchestration.backend.service;

import com.orchestration.backend.dto.request.TaskRequest;
import com.orchestration.backend.dto.response.PlacementDecisionResponse;
import com.orchestration.backend.dto.response.TaskResponse;
import com.orchestration.backend.entity.*;
import com.orchestration.backend.exception.TaskNotCancellableException;
import com.orchestration.backend.exception.TaskNotFoundException;
import com.orchestration.backend.repository.*;
import com.orchestration.backend.tracing.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
        TraceContext ctx = TraceContext.current();

        // 1. Verifier idempotency key
        var existing = ctx.timeDb(() -> taskRepository.findByIdempotencyKey(idempotencyKey));
        if (existing.isPresent()) {
            log.info("Tache deja existante pour idempotency key: {}", idempotencyKey);
            return ctx.timeDb(() -> decisionRepository.findByTask(existing.get()))
                    .map(this::toDecisionResponse)
                    .orElseThrow(() -> new RuntimeException("Decision introuvable"));
        }

        // 2. Charger l'utilisateur
        User user = ctx.timeDb(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouve"));

        // 3. Creer la tache avec status PENDING
        Task newTask = Task.builder()
                .cpuRequired(request.getCpuRequired())
                .memoryRequiredMb(request.getMemoryRequiredMb())
                .priority(request.getPriority())
                .status("PENDING")
                .submittedBy(user)
                .idempotencyKey(idempotencyKey)
                .build();

        final Task task = ctx.timeDb(() -> taskRepository.save(newTask));

        ctx.timeDb(() -> auditService.log("TASK_SUBMITTED", username, "task",
                task.getId().toString(),
                "{\"cpu\":" + task.getCpuRequired() + ",\"memory\":" + task.getMemoryRequiredMb() + "}"));

        // 4. Lire la regle active
        String policy = ctx.timeDb(ruleRepository::findFirstByIsActiveTrueOrderByCreatedAtDesc)
                .map(PlacementRule::getPolicy)
                .orElse("balanced");

        // 5. Lire les machines healthy
        List<Machine> machines = ctx.timeDb(machineRepository::findByHealthyTrue);

        if (machines.isEmpty()) {
            return handleRejection(task, policy, username,
                    "Aucune machine disponible dans le systeme.");
        }

        // 6. Appeler le moteur Python (logique metier)
        PlacementEngineResult result = ctx.timeBusiness(
                () -> placementEngineClient.place(task, machines, policy, ctx.getTraceId()));

        // 7. Sauvegarder la decision
        PlacementDecision newDecision = PlacementDecision.builder()
                .task(task)
                .selectedNodeId(result.getSelectedNodeId())
                .policy(result.getPolicy())
                .score(result.getScore() != null ?
                        BigDecimal.valueOf(result.getScore()) : null)
                .accepted(result.getAccepted())
                .reason(result.getReason())
                .build();

        final PlacementDecision decision = ctx.timeDb(() -> decisionRepository.save(newDecision));

        // 8. Mettre a jour le status de la tache
        task.setStatus(result.getAccepted() ? "PLACED" : "REJECTED");
        ctx.timeDb(() -> taskRepository.save(task));

        // 8b. Reserver les ressources de la machine retenue
        if (result.getAccepted()) {
            ctx.timeDb(() -> decrementMachineResources(
                    result.getSelectedNodeId(), task.getCpuRequired(), task.getMemoryRequiredMb()));
        }

        // 9. Audit
        String eventType = result.getAccepted() ? "TASK_PLACED" : "TASK_REJECTED";
        ctx.timeDb(() -> auditService.log(eventType, username, "task",
                task.getId().toString(),
                "{\"machine\":\"" + result.getSelectedNodeId() + "\",\"policy\":\"" + policy + "\"}"));

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
    public void cancel(UUID taskId, String username) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Tache introuvable: " + taskId));

        if (!task.getSubmittedBy().getUsername().equals(username)) {
            throw new AccessDeniedException("Vous ne pouvez pas annuler cette tache");
        }

        String status = task.getStatus();
        if (!status.equals("PENDING") && !status.equals("PLACED")) {
            throw new TaskNotCancellableException(
                    "Seules les taches PENDING ou PLACED peuvent etre annulees (statut actuel: " + status + ")");
        }

        if (status.equals("PLACED")) {
            decisionRepository.findByTask(task).ifPresent(decision -> {
                if (decision.getSelectedNodeId() != null) {
                    restoreMachineResources(decision.getSelectedNodeId(), task.getCpuRequired(), task.getMemoryRequiredMb());
                }
            });
        }

        task.setStatus("CANCELLED");
        taskRepository.save(task);

        auditService.log("TASK_CANCELLED", username, "task",
                task.getId().toString(), null);
    }

    private void decrementMachineResources(String machineId, BigDecimal cpu, Integer memoryMb) {
        machineRepository.findById(machineId).ifPresent(machine -> {
            machine.setCpuAvailable(machine.getCpuAvailable().subtract(cpu));
            machine.setMemoryAvailableMb(machine.getMemoryAvailableMb() - memoryMb);
            machineRepository.save(machine);
        });
    }

    private void restoreMachineResources(String machineId, BigDecimal cpu, Integer memoryMb) {
        machineRepository.findById(machineId).ifPresent(machine -> {
            machine.setCpuAvailable(machine.getCpuAvailable().add(cpu));
            machine.setMemoryAvailableMb(machine.getMemoryAvailableMb() + memoryMb);
            machineRepository.save(machine);
        });
    }

    private PlacementDecisionResponse handleRejection(Task task, String policy,
                                                      String username, String reason) {
        TraceContext ctx = TraceContext.current();

        PlacementDecision newDecision = PlacementDecision.builder()
                .task(task)
                .selectedNodeId(null)
                .policy(policy)
                .score(null)
                .accepted(false)
                .reason(reason)
                .build();

        final Task finalTask = task;
        final PlacementDecision decision = ctx.timeDb(() -> decisionRepository.save(newDecision));
        finalTask.setStatus("REJECTED");
        ctx.timeDb(() -> taskRepository.save(finalTask));

        ctx.timeDb(() -> auditService.log("TASK_REJECTED", username, "task",
                finalTask.getId().toString(),
                "{\"reason\":\"" + reason + "\"}"));

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