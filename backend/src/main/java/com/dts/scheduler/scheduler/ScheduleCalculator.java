package com.dts.scheduler.scheduler;

import com.dts.scheduler.entity.ScheduleType;
import com.dts.scheduler.entity.Task;
import com.dts.scheduler.exception.BadRequestException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class ScheduleCalculator {

    public Instant calculateInitialNextRunAt(ScheduleType scheduleType, String cronExpression, String timezoneStr, Instant scheduledAt, Long intervalSeconds) {
        Instant now = Instant.now();
        ZoneId zoneId = parseZoneId(timezoneStr);

        return switch (scheduleType) {
            case ONE_TIME -> {
                if (scheduledAt == null) {
                    yield now;
                }
                yield scheduledAt;
            }
            case INTERVAL -> {
                if (intervalSeconds == null || intervalSeconds <= 0) {
                    throw new BadRequestException("Interval seconds must be greater than 0");
                }
                yield now.plusSeconds(intervalSeconds);
            }
            case CRON -> calculateNextCronInstant(cronExpression, zoneId, now);
        };
    }

    public Instant calculateNextRunAt(Task task, Instant baseTime) {
        if (task.getScheduleType() == ScheduleType.ONE_TIME) {
            return null; // One-time task does not run again
        }

        if (task.getScheduleType() == ScheduleType.INTERVAL) {
            long interval = task.getIntervalSeconds() != null ? task.getIntervalSeconds() : 60L;
            Instant base = baseTime != null ? baseTime : Instant.now();
            return base.plusSeconds(interval);
        }

        if (task.getScheduleType() == ScheduleType.CRON) {
            ZoneId zoneId = parseZoneId(task.getTimezone());
            Instant base = baseTime != null ? baseTime : Instant.now();
            return calculateNextCronInstant(task.getCronExpression(), zoneId, base);
        }

        return null;
    }

    private Instant calculateNextCronInstant(String cronExpression, ZoneId zoneId, Instant fromInstant) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new BadRequestException("Cron expression is required for CRON schedule type");
        }

        String expression = cronExpression.trim();
        String[] parts = expression.split("\\s+");
        if (parts.length == 5) {
            expression = "0 " + expression; // convert 5-part UNIX cron to 6-part Spring cron
        }

        CronExpression cron = CronExpression.parse(expression);
        ZonedDateTime zdt = fromInstant.atZone(zoneId);
        ZonedDateTime nextZdt = cron.next(zdt);
        return nextZdt != null ? nextZdt.toInstant() : null;
    }

    private ZoneId parseZoneId(String timezoneStr) {
        try {
            return (timezoneStr != null && !timezoneStr.isBlank()) ? ZoneId.of(timezoneStr.trim()) : ZoneId.of("UTC");
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }
}
