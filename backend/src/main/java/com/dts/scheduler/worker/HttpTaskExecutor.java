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
public class HttpTaskExecutor implements TaskExecutor {

    private final SsrfValidator ssrfValidator;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER) // Prevent SSRF redirect bypass
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.HTTP_TASK;
    }

    @Override
    public TaskExecutionResult execute(TaskExecutionEvent event) {
        long startTime = System.currentTimeMillis();
        try {
            HttpPayloadDto payload = parsePayload(event.getPayload());
            String targetUrl = payload.getUrl();

            // Enforce SSRF validation at execution time
            ssrfValidator.validateSafeUrl(targetUrl);

            int timeout = event.getTimeoutSeconds() > 0 ? event.getTimeoutSeconds() : 30;
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(timeout));

            if (payload.getHeaders() != null) {
                payload.getHeaders().forEach((k, v) -> {
                    if (k != null && v != null && !k.equalsIgnoreCase("host")) {
                        reqBuilder.header(k, v);
                    }
                });
            }

            String method = payload.getMethod() != null ? payload.getMethod().toUpperCase() : "GET";
            HttpRequest.BodyPublisher bodyPublisher = (payload.getBody() != null && !payload.getBody().isBlank())
                    ? HttpRequest.BodyPublishers.ofString(payload.getBody())
                    : HttpRequest.BodyPublishers.noBody();

            reqBuilder.method(method, bodyPublisher);
            HttpRequest request = reqBuilder.build();

            log.info("Worker executing HTTP_TASK [executionId={}] method={} url={}",
                    event.getExecutionId(), method, targetUrl);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                String bodyPreview = response.body() != null && response.body().length() > 2000
                        ? response.body().substring(0, 2000) + "... [truncated]"
                        : response.body();
                return TaskExecutionResult.success(statusCode, bodyPreview, duration);
            } else {
                return TaskExecutionResult.failure(statusCode, "HTTP request failed with status code " + statusCode + ": " + response.body(), duration);
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("HTTP_TASK failed [executionId={}]: {}", event.getExecutionId(), e.getMessage());
            return TaskExecutionResult.failure(500, "Execution error: " + e.getMessage(), duration);
        }
    }

    private HttpPayloadDto parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, HttpPayloadDto.class);
        } catch (Exception e) {
            // Fallback: raw payload treated as GET URL
            return HttpPayloadDto.builder().url(rawPayload.trim()).method("GET").build();
        }
    }
}
