package com.orchestration.backend.service;

import com.orchestration.backend.dto.request.TaskRequest;
import com.orchestration.backend.dto.response.PlacementDecisionResponse;
import com.orchestration.backend.entity.*;
import com.orchestration.backend.exception.TaskNotCancellableException;
import com.orchestration.backend.exception.TaskNotFoundException;
import com.orchestration.backend.repository.*;
import com.orchestration.backend.tracing.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MachineRepository machineRepository;
    @Mock
    private PlacementRuleRepository ruleRepository;
    @Mock
    private PlacementDecisionRepository decisionRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private PlacementEngineClient placementEngineClient;

    @InjectMocks
    private TaskService taskService;

    private User user;

    @BeforeEach
    void setUp() {
        TraceContext.start("test-trace");
        user = User.builder().id(1L).username("operateur").role("OPERATOR").build();
    }

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    private TaskRequest sampleRequest() {
        TaskRequest request = new TaskRequest();
        request.setCpuRequired(BigDecimal.valueOf(2));
        request.setMemoryRequiredMb(512);
        request.setPriority("normal");
        return request;
    }

    @Test
    void submit_withKnownIdempotencyKey_returnsExistingDecisionWithoutCreatingNewTask() {
        Task existingTask = Task.builder().id(UUID.randomUUID()).idempotencyKey("key-1").build();
        PlacementDecision existingDecision = PlacementDecision.builder()
                .task(existingTask)
                .accepted(true)
                .selectedNodeId("node-1")
                .policy("balanced")
                .reason("ok")
                .build();

        when(taskRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existingTask));
        when(decisionRepository.findByTask(existingTask)).thenReturn(Optional.of(existingDecision));

        PlacementDecisionResponse response = taskService.submit(sampleRequest(), "key-1", "operateur");

        assertThat(response.getSelectedNodeId()).isEqualTo("node-1");
        verify(taskRepository, never()).save(any());
        verifyNoInteractions(placementEngineClient);
    }

    @Test
    void submit_noHealthyMachines_rejectsTaskWithoutCallingPlacementEngine() {
        when(taskRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername("operateur")).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(ruleRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(machineRepository.findByHealthyTrue()).thenReturn(List.of());
        when(decisionRepository.save(any(PlacementDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        PlacementDecisionResponse response = taskService.submit(sampleRequest(), "key-2", "operateur");

        assertThat(response.getAccepted()).isFalse();
        assertThat(response.getReason()).contains("Aucune machine disponible");
        verifyNoInteractions(placementEngineClient);
    }

    @Test
    void submit_placementAccepted_decrementsMachineResources() {
        Machine machine = Machine.builder()
                .id("node-1")
                .cpuAvailable(BigDecimal.valueOf(4))
                .memoryAvailableMb(2048)
                .healthy(true)
                .build();

        when(taskRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername("operateur")).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(ruleRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(machineRepository.findByHealthyTrue()).thenReturn(List.of(machine));
        when(machineRepository.findById("node-1")).thenReturn(Optional.of(machine));
        when(placementEngineClient.place(any(), any(), any(), anyString())).thenReturn(
                PlacementEngineResult.builder()
                        .accepted(true)
                        .selectedNodeId("node-1")
                        .policy("balanced")
                        .score(0.9)
                        .reason("meilleure machine disponible")
                        .latencyMs(5L)
                        .build());
        when(decisionRepository.save(any(PlacementDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        PlacementDecisionResponse response = taskService.submit(sampleRequest(), "key-3", "operateur");

        assertThat(response.getAccepted()).isTrue();
        assertThat(machine.getCpuAvailable()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(machine.getMemoryAvailableMb()).isEqualTo(1536);
        verify(machineRepository).save(machine);
    }

    @Test
    void submit_noActiveRule_letsPlacementEngineChoosePolicyAutomatically() {
        Machine machine = Machine.builder()
                .id("node-1")
                .cpuAvailable(BigDecimal.valueOf(4))
                .memoryAvailableMb(2048)
                .healthy(true)
                .build();

        when(taskRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername("operateur")).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        // Aucune regle active en base.
        when(ruleRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(machineRepository.findByHealthyTrue()).thenReturn(List.of(machine));
        when(machineRepository.findById("node-1")).thenReturn(Optional.of(machine));
        when(placementEngineClient.place(any(), any(), any(), anyString())).thenReturn(
                PlacementEngineResult.builder()
                        .accepted(true)
                        .selectedNodeId("node-1")
                        .policy("least_loaded") // choisi automatiquement par le moteur
                        .score(4.0)
                        .reason("Machine node-1 retenue selon la politique least_loaded.")
                        .latencyMs(3L)
                        .build());
        when(decisionRepository.save(any(PlacementDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.submit(sampleRequest(), "key-auto", "operateur");

        ArgumentCaptor<String> policyCaptor = ArgumentCaptor.forClass(String.class);
        verify(placementEngineClient).place(any(), any(), policyCaptor.capture(), anyString());
        assertThat(policyCaptor.getValue())
                .as("aucune politique ne doit etre imposee quand aucune regle admin n'est active")
                .isNull();
    }

    @Test
    void submit_withActiveRule_forcesPolicyAsOverride() {
        Machine machine = Machine.builder()
                .id("node-1")
                .cpuAvailable(BigDecimal.valueOf(4))
                .memoryAvailableMb(2048)
                .healthy(true)
                .build();
        PlacementRule activeRule = PlacementRule.builder()
                .id(1L)
                .policy("lowest_latency")
                .isActive(true)
                .build();

        when(taskRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername("operateur")).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(ruleRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(activeRule));
        when(machineRepository.findByHealthyTrue()).thenReturn(List.of(machine));
        when(machineRepository.findById("node-1")).thenReturn(Optional.of(machine));
        when(placementEngineClient.place(any(), any(), any(), anyString())).thenReturn(
                PlacementEngineResult.builder()
                        .accepted(true)
                        .selectedNodeId("node-1")
                        .policy("lowest_latency")
                        .score(0.1)
                        .reason("Machine node-1 retenue selon la politique lowest_latency.")
                        .latencyMs(3L)
                        .build());
        when(decisionRepository.save(any(PlacementDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.submit(sampleRequest(), "key-forced", "operateur");

        ArgumentCaptor<String> policyCaptor = ArgumentCaptor.forClass(String.class);
        verify(placementEngineClient).place(any(), any(), policyCaptor.capture(), anyString());
        assertThat(policyCaptor.getValue())
                .as("la regle active doit forcer la politique, en priorite sur le choix automatique")
                .isEqualTo("lowest_latency");
    }

    @Test
    void cancel_whenNotOwner_throwsAccessDenied() {
        User owner = User.builder().id(2L).username("autre-operateur").build();
        Task task = Task.builder().id(UUID.randomUUID()).status("PENDING").submittedBy(owner).build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.cancel(task.getId(), "operateur"))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void cancel_whenStatusNotCancellable_throwsConflict() {
        Task task = Task.builder().id(UUID.randomUUID()).status("REJECTED").submittedBy(user).build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.cancel(task.getId(), "operateur"))
                .isInstanceOf(TaskNotCancellableException.class);
    }

    @Test
    void cancel_whenTaskMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.cancel(id, "operateur"))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void cancel_placedTask_restoresMachineResourcesAndSetsStatusCancelled() {
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .status("PLACED")
                .cpuRequired(BigDecimal.valueOf(2))
                .memoryRequiredMb(512)
                .submittedBy(user)
                .build();
        Machine machine = Machine.builder()
                .id("node-1")
                .cpuAvailable(BigDecimal.valueOf(2))
                .memoryAvailableMb(1536)
                .build();
        PlacementDecision decision = PlacementDecision.builder()
                .task(task)
                .selectedNodeId("node-1")
                .accepted(true)
                .build();

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(decisionRepository.findByTask(task)).thenReturn(Optional.of(decision));
        when(machineRepository.findById("node-1")).thenReturn(Optional.of(machine));

        taskService.cancel(task.getId(), "operateur");

        assertThat(task.getStatus()).isEqualTo("CANCELLED");
        assertThat(machine.getCpuAvailable()).isEqualByComparingTo(BigDecimal.valueOf(4));
        assertThat(machine.getMemoryAvailableMb()).isEqualTo(2048);
        verify(taskRepository).save(task);
    }
}
