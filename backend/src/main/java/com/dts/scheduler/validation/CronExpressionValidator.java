package com.dts.scheduler.validation;

import com.dts.scheduler.exception.BadRequestException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

@Component
public class CronExpressionValidator {

    public void validateCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new BadRequestException("Cron expression cannot be empty");
        }

        // Support both 5-field UNIX ("* * * * *") and 6-field ("0 * * * * *")
        String expression = cronExpression.trim();
        String[] parts = expression.split("\\s+");
        if (parts.length == 5) {
            // Prepend second field '0' for Spring's 6-field parser
            expression = "0 " + expression;
        }

        if (!CronExpression.isValidExpression(expression)) {
            throw new BadRequestException("Invalid cron expression format: " + cronExpression);
        }
    }

    public void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return;
        }
        try {
            ZoneId.of(timezone.trim());
        } catch (ZoneRulesException | IllegalArgumentException e) {
            throw new BadRequestException("Invalid timezone identifier: " + timezone);
        }
    }
}
