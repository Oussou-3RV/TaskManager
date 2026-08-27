package com.orchestration.backend.integration;

import com.orchestration.backend.dto.request.LoginRequest;
import com.orchestration.backend.dto.request.PlacementRuleRequest;
import com.orchestration.backend.dto.request.TaskRequest;
import com.jayway.jsonpath.JsonPath;
import com.orchestration.backend.dto.response.AuthResponse;
import com.orchestration.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demarre le vrai contexte Spring (securite, controllers, JPA) contre un
 * Postgres jetable (Testcontainers) migre par Flyway avec les donnees
 * d'amorcage (V2__seed.sql : admin/operateur/auditeur, mot de passe
 * "password123"). Le moteur de placement Python n'est pas demarre ici : les
 * appels echouent proprement (PlacementEngineClient les catch) et la tache
 * finit REJECTED, ce qui ne genes ni les tests d'autorisation ni d'idempotence.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TaskApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orchestration")
            .withUsername("admin")
            .withPassword("secret");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    private String login(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", request, AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private TaskRequest sampleTaskRequest() {
        TaskRequest request = new TaskRequest();
        request.setCpuRequired(BigDecimal.valueOf(1));
        request.setMemoryRequiredMb(256);
        request.setPriority("normal");
        return request;
    }

    // ---------- Authentification ----------

    @ParameterizedTest
    @ValueSource(strings = {"admin", "operateur", "auditeur"})
    void login_withDemoAccount_returns200AndToken(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("password123");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getUsername()).isEqualTo(username);
    }

    @Test
    void login_withWrongPassword_returns401() {
        LoginRequest request = new LoginRequest();
        request.setUsername("operateur");
        request.setPassword("mauvais-mot-de-passe");

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- Autorisation par role ----------

    @Test
    void submitTask_asOperator_returns202() {
        String token = login("operateur", "password123");
        HttpHeaders headers = authHeaders(token);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        HttpEntity<TaskRequest> entity = new HttpEntity<>(sampleTaskRequest(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/tasks", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    @Test
    void submitTask_asAuditor_returns403() {
        String token = login("auditeur", "password123");
        HttpHeaders headers = authHeaders(token);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        HttpEntity<TaskRequest> entity = new HttpEntity<>(sampleTaskRequest(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/tasks", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void submitTask_withoutToken_returns401NotForbidden() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        HttpEntity<TaskRequest> entity = new HttpEntity<>(sampleTaskRequest(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/tasks", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createRule_asAdmin_returns200() {
        String token = login("admin", "password123");
        HttpEntity<PlacementRuleRequest> entity = new HttpEntity<>(sampleRuleRequest(), authHeaders(token));

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/rules", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createRule_asOperator_returns403() {
        String token = login("operateur", "password123");
        HttpEntity<PlacementRuleRequest> entity = new HttpEntity<>(sampleRuleRequest(), authHeaders(token));

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/rules", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getAudit_asAuditor_returns200() {
        String token = login("auditeur", "password123");
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/audit", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAudit_asOperator_returns403() {
        String token = login("operateur", "password123");
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/audit", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private PlacementRuleRequest sampleRuleRequest() {
        PlacementRuleRequest request = new PlacementRuleRequest();
        request.setName("Regle test " + UUID.randomUUID());
        request.setPolicy("balanced");
        request.setIsActive(false);
        return request;
    }

    // ---------- Idempotence de bout en bout ----------

    @Test
    void submitTask_sameIdempotencyKey_doesNotDuplicateTask() {
        String token = login("operateur", "password123");
        String idempotencyKey = UUID.randomUUID().toString();
        HttpHeaders headers = authHeaders(token);
        headers.set("Idempotency-Key", idempotencyKey);
        HttpEntity<TaskRequest> entity = new HttpEntity<>(sampleTaskRequest(), headers);

        long countBefore = taskRepository.count();

        ResponseEntity<String> first = restTemplate.postForEntity(
                baseUrl + "/api/v1/tasks", entity, String.class);
        ResponseEntity<String> second = restTemplate.postForEntity(
                baseUrl + "/api/v1/tasks", entity, String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String firstTaskId = JsonPath.read(first.getBody(), "$.taskId");
        String secondTaskId = JsonPath.read(second.getBody(), "$.taskId");
        assertThat(firstTaskId).isEqualTo(secondTaskId);
        assertThat(taskRepository.count()).isEqualTo(countBefore + 1);
    }

    // ---------- Annulation ----------

    @Test
    void cancelTask_asNonOwner_returns403() {
        String operatorToken = login("operateur", "password123");
        HttpHeaders submitHeaders = authHeaders(operatorToken);
        submitHeaders.set("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<String> submitted = restTemplate.postForEntity(
                baseUrl + "/api/v1/tasks", new HttpEntity<>(sampleTaskRequest(), submitHeaders),
                String.class);
        String taskId = JsonPath.read(submitted.getBody(), "$.taskId");

        String adminToken = login("admin", "password123");
        HttpEntity<Void> deleteEntity = new HttpEntity<>(authHeaders(adminToken));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId, HttpMethod.DELETE, deleteEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cancelTask_alreadyCancelled_returns409() {
        String token = login("operateur", "password123");
        HttpHeaders headers = authHeaders(token);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<String> submitted = restTemplate.postForEntity(
                baseUrl + "/api/v1/tasks", new HttpEntity<>(sampleTaskRequest(), headers),
                String.class);
        String taskId = JsonPath.read(submitted.getBody(), "$.taskId");
        HttpEntity<Void> deleteEntity = new HttpEntity<>(authHeaders(token));

        ResponseEntity<String> firstCancel = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId, HttpMethod.DELETE, deleteEntity, String.class);
        ResponseEntity<String> secondCancel = restTemplate.exchange(
                baseUrl + "/api/v1/tasks/" + taskId, HttpMethod.DELETE, deleteEntity, String.class);

        assertThat(firstCancel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(secondCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
