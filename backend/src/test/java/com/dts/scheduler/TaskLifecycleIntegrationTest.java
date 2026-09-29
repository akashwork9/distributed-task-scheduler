package com.dts.scheduler;

import com.dts.scheduler.dto.auth.AuthResponse;
import com.dts.scheduler.dto.auth.RegisterRequest;
import com.dts.scheduler.dto.task.CreateTaskRequest;
import com.dts.scheduler.dto.task.TaskResponse;
import com.dts.scheduler.dto.task.TriggerResponse;
import com.dts.scheduler.entity.ConcurrencyPolicy;
import com.dts.scheduler.entity.ExecutionStatus;
import com.dts.scheduler.entity.RetryPolicy;
import com.dts.scheduler.entity.ScheduleType;
import com.dts.scheduler.entity.TaskType;
import com.dts.scheduler.repository.TaskExecutionRepository;
import com.dts.scheduler.repository.TaskRepository;
import com.dts.scheduler.service.AuthService;
import com.dts.scheduler.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class TaskLifecycleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dts_test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("dts.scheduler.enabled", () -> "false"); // Disable poller in test to trigger manually
        registry.add("dts.worker.enabled", () -> "true");
    }

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserDetailsService userDetailsService;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        String testEmail = "integration-user@example.com";
        try {
            AuthResponse auth = authService.register(RegisterRequest.builder()
                    .name("Integration Tester")
                    .email(testEmail)
                    .password("secret123")
                    .build());
            testUserId = auth.getUser().getId();
        } catch (Exception e) {
            // Already registered
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @Test
    @DisplayName("End-to-End Task Lifecycle: Create -> Trigger -> Kafka Dispatch -> Worker Execution -> Success")
    void testEndToEndTaskExecution() {
        // 1. Create Internal Task
        CreateTaskRequest request = CreateTaskRequest.builder()
                .name("Integration Test Job")
                .description("Tests end-to-end Kafka execution flow")
                .taskType(TaskType.INTERNAL_TASK)
                .payload("SYSTEM_HEALTH_CHECK")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSeconds(60L)
                .concurrencyPolicy(ConcurrencyPolicy.ALLOW_CONCURRENT)
                .retryPolicy(RetryPolicy.EXPONENTIAL_BACKOFF)
                .maxRetries(3)
                .retryDelaySeconds(2)
                .backoffMultiplier(2.0)
                .timeoutSeconds(10)
                .enabled(true)
                .build();

        TaskResponse createdTask = taskService.createTask(request);
        assertNotNull(createdTask.getId());
        assertEquals("Integration Test Job", createdTask.getName());

        // 2. Trigger Task Immediately
        TriggerResponse triggerResponse = taskService.triggerTaskImmediately(createdTask.getId());
        assertNotNull(triggerResponse.getExecutionId());
        assertEquals("QUEUED", triggerResponse.getStatus());

        // 3. Await worker consumption via Kafka and database state update to SUCCESS
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            return taskExecutionRepository.findByExecutionId(triggerResponse.getExecutionId())
                    .map(exec -> exec.getStatus() == ExecutionStatus.SUCCESS)
                    .orElse(false);
        });

        // 4. Verify Completed State
        var execution = taskExecutionRepository.findByExecutionId(triggerResponse.getExecutionId()).orElseThrow();
        assertEquals(ExecutionStatus.SUCCESS, execution.getStatus());
        assertNotNull(execution.getCompletedAt());
        assertNotNull(execution.getWorkerId());
        assertTrue(execution.getResult().contains("System health OK"));
    }

    @Test
    @DisplayName("Idempotency: Duplicate execution claim is ignored and discarded")
    void testExecutionIdempotency() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .name("Idempotency Test Job")
                .taskType(TaskType.INTERNAL_TASK)
                .payload("SYSTEM_HEALTH_CHECK")
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSeconds(60L)
                .enabled(true)
                .build();

        TaskResponse task = taskService.createTask(request);
        TriggerResponse trigger = taskService.triggerTaskImmediately(task.getId());

        // Wait for execution to conclude
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            return taskExecutionRepository.findByExecutionId(trigger.getExecutionId())
                    .map(exec -> exec.getStatus() == ExecutionStatus.SUCCESS)
                    .orElse(false);
        });

        // Attempt second atomic claim for the same execution ID
        int rowsClaimed = taskExecutionRepository.claimExecution(
                trigger.getExecutionId(),
                ExecutionStatus.QUEUED,
                ExecutionStatus.RUNNING,
                java.time.Instant.now(),
                "duplicate-worker"
        );

        // Verification: 0 rows claimed due to atomic status check (Idempotent protection)
        assertEquals(0, rowsClaimed);
    }
}
