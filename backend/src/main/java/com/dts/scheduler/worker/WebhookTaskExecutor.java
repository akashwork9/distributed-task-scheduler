package com.dts.scheduler.worker;

import com.dts.scheduler.dto.task.HttpPayloadDto;
import com.dts.scheduler.entity.TaskType;
import com.dts.scheduler.kafka.TaskExecutionEvent;
import com.dts.scheduler.validation.SsrfValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookTaskExecutor implements TaskExecutor {

    private final SsrfValidator ssrfValidator;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.WEBHOOK_TASK;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            HttpPayloadDto payload = parsePayload(event.getPayload());
            String targetUrl = payload.getUrl();

            ssrfValidator.validateSafeUrl(targetUrl);

            int timeout = event.getTimeoutSeconds() > 0 ? event.getTimeoutSeconds() : 30;
            String bodyContent = (payload.getBody() != null && !payload.getBody().isBlank())
                    ? payload.getBody()
                    : "{\"event\":\"task.execution\",\"taskId\":" + event.getTaskId() + ",\"executionId\":\"" + event.getExecutionId() + "\"}";

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(timeout))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "DistributedTaskScheduler-Webhook/1.0")
                    .header("X-DTS-Event", "task.execution")
                    .header("X-DTS-Execution-Id", event.getExecutionId())
                    .header("X-DTS-Attempt", String.valueOf(event.getAttempt()))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyContent));

            if (payload.getHeaders() != null) {
                payload.getHeaders().forEach((k, v) -> {
                    if (k != null && v != null && !k.equalsIgnoreCase("host")) {
                        reqBuilder.header(k, v);
                    }
                });
            }

            log.info("Worker dispatching WEBHOOK_TASK [executionId={}] targetUrl={}", event.getExecutionId(), targetUrl);
            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return TaskExecutionResult.success(statusCode, "Webhook acknowledged with HTTP " + statusCode, duration);
            } else {
                return TaskExecutionResult.failure(statusCode, "Webhook delivery failed with HTTP " + statusCode + ": " + response.body(), duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("WEBHOOK_TASK failed [executionId={}]: {}", event.getExecutionId(), e.getMessage());
            return TaskExecutionResult.failure(500, "Webhook delivery error: " + e.getMessage(), duration);
        }
    }

    private HttpPayloadDto parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, HttpPayloadDto.class);
        } catch (Exception e) {
            return HttpPayloadDto.builder().url(rawPayload.trim()).build();
        }
    }
}
