package com.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orchestration.backend.entity.Machine;
import com.orchestration.backend.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlacementEngineClient {

    @Value("${placement.service.url}")
    private String placementServiceUrl;

    private final ObjectMapper objectMapper =  new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PlacementEngineResult place(Task task, List<Machine> machines, String policy) {
        try {
            // Construire le body JSON
            Map<String, Object> body = Map.of(
                    "task", Map.of(
                            "id", task.getId().toString(),
                            "cpu_required", task.getCpuRequired(),
                            "memory_required_mb", task.getMemoryRequiredMb(),
                            "priority", task.getPriority()
                    ),
                    "nodes", machines.stream().map(m -> Map.of(
                            "id", m.getId(),
                            "level", m.getLevel(),
                            "cpu_available", m.getCpuAvailable(),
                            "memory_available_mb", m.getMemoryAvailableMb(),
                            "latency_ms", m.getLatencyMs(),
                            "healthy", m.getHealthy()
                    )).toList(),
                    "policy", policy
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(placementServiceUrl + "/place"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;

            log.info("Appel placement service - status: {} - latence: {}ms",
                    response.statusCode(), latency);

            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);

            return PlacementEngineResult.builder()
                    .accepted((Boolean) result.get("accepted"))
                    .selectedNodeId((String) result.get("selected_node_id"))
                    .policy((String) result.get("policy"))
                    .score(result.get("score") != null ?
                            Double.parseDouble(result.get("score").toString()) : null)
                    .reason((String) result.get("reason"))
                    .latencyMs(latency)
                    .build();

        } catch (Exception e) {
            log.error("Erreur appel placement service: {}", e.getMessage());
            return PlacementEngineResult.builder()
                    .accepted(false)
                    .selectedNodeId(null)
                    .policy(policy)
                    .score(null)
                    .reason("Erreur interne du moteur de placement : " + e.getMessage())
                    .latencyMs(0L)
                    .build();
        }
    }
}