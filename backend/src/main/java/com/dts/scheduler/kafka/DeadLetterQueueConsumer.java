package com.dts.scheduler.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueConsumer {

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "${dts.kafka.topics.task-dlq:task.execution.dlq}",
            groupId = "dts-dlq-monitor-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDeadLetterEvent(@Payload String payloadString, Acknowledgment acknowledgment) {
        try {
            TaskExecutionEvent event = objectMapper.readValue(payloadString, TaskExecutionEvent.class);
            log.error("ALERT: Dead Letter Queue received permanently failed task: executionId={} taskId={} attempts={}",
                    event.getExecutionId(), event.getTaskId(), event.getAttempt());
            meterRegistry.counter("dts_dlq_events_received_total").increment();
        } catch (Exception e) {
            log.error("Failed to parse DLQ message: {}", payloadString, e);
        } finally {
            acknowledgment.acknowledge();
        }
    }
}
