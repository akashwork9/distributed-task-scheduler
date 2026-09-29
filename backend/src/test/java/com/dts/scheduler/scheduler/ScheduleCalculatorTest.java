package com.dts.scheduler.scheduler;

import com.dts.scheduler.entity.ScheduleType;
import com.dts.scheduler.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleCalculatorTest {

    private ScheduleCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ScheduleCalculator();
    }

    @Test
    @DisplayName("Should correctly calculate one-time schedule timestamp")
    void testOneTimeSchedule() {
        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant result = calculator.calculateInitialNextRunAt(
                ScheduleType.ONE_TIME, null, "UTC", scheduledAt, null
        );

        assertNotNull(result);
        assertEquals(scheduledAt, result);
    }

    @Test
    @DisplayName("Should correctly calculate interval schedule addition")
    void testIntervalSchedule() {
        Instant before = Instant.now();
        Instant result = calculator.calculateInitialNextRunAt(
                ScheduleType.INTERVAL, null, "UTC", null, 120L
        );

        assertNotNull(result);
        assertTrue(result.isAfter(before.plus(119, ChronoUnit.SECONDS)));
        assertTrue(result.isBefore(before.plus(122, ChronoUnit.SECONDS)));
    }

    @Test
    @DisplayName("Should correctly calculate CRON expression with standard 5-part and 6-part formats")
    void testCronSchedule() {
        Instant now = Instant.now();
        Instant result = calculator.calculateInitialNextRunAt(
                ScheduleType.CRON, "*/5 * * * *", "UTC", null, null
        );

        assertNotNull(result);
        assertTrue(result.isAfter(now));
    }

    @Test
    @DisplayName("One-time tasks should return null for subsequent nextRunAt")
    void testOneTimeNextRunReturnsNull() {
        Task task = Task.builder()
                .scheduleType(ScheduleType.ONE_TIME)
                .scheduledAt(Instant.now())
                .build();

        Instant next = calculator.calculateNextRunAt(task, Instant.now());
        assertNull(next);
    }
}
