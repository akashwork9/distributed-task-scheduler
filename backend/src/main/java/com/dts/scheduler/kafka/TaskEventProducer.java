package com.dts.scheduler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${dts.kafka.topics.task-requested:task.execution.requested}")
    private String taskRequestedTopic;

    @Value("${dts.kafka.topics.task-completed:task.execution.completed}")
    private String taskCompletedTopic;

    @Value("${dts.kafka.topics.task-failed:task.execution.failed}")
    private String taskFailedTopic;

    @Value("${dts.kafka.topics.task-retry:task.execution.retry}")
    private String taskRetryTopic;

    @Value("${dts.kafka.topics.task-dlq:task.execution.dlq}")
    private String taskDlqTopic;

    public void publishTaskRequested(TaskExecutionEvent event) {
        publish(taskRequestedTopic, String.valueOf(event.getTaskId()), event);
    }

    public void publishTaskCompleted(TaskExecutionEvent event) {
        publish(taskCompletedTopic, String.valueOf(event.getTaskId()), event);
    }

    public void publishTaskFailed(TaskExecutionEvent event) {
        publish(taskFailedTopic, String.valueOf(event.getTaskId()), event);
    }

    public void publishTaskRetry(TaskExecutionEvent event) {
        publish(taskRetryTopic, String.valueOf(event.getTaskId()), event);
    }

    public void publishTaskDlq(TaskExecutionEvent event) {
        publish(taskDlqTopic, String.valueOf(event.getTaskId()), event);
    }

    private void publish(String topic, String key, TaskExecutionEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published event to topic [{}] key [{}] executionId [{}] offset [{}]",
                            topic, key, event.getExecutionId(), result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event to topic [{}] key [{}] executionId [{}]: {}",
                            topic, key, event.getExecutionId(), ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed for TaskExecutionEvent executionId={}", event.getExecutionId(), e);
        }
    }
}
