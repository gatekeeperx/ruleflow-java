package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.DateDiffContext;
import com.gatekeeperx.ruleflow.visitors.Visitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateDiffContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.DateDiffContext> {
    private static final Logger logger = LoggerFactory.getLogger(DateDiffContextEvaluator.class);
    @Override
    public Object evaluate(DateDiffContext ctx, Visitor visitor) {
        ZonedDateTime left = (ZonedDateTime) visitor.visit(ctx.left);
        ZonedDateTime right = (ZonedDateTime) visitor.visit(ctx.right);

        if (left == null || right == null) {
            logger.debug("DateDiff: left={}, right={}", left, right);
            return null;
        }

        long result;

        if (ctx.MINUTE() != null) {
            result = Duration.between(left, right).toMinutes();
        } else if (ctx.HOUR() != null) {
            result = Duration.between(left, right).toHours();
        } else if (ctx.DAY() != null) {
            result = Duration.between(left, right.truncatedTo(ChronoUnit.DAYS)).toDays();
        } else {
            throw new RuntimeException("Interval not supported in " + ctx.getText());
        }

        long abs = Math.abs(result);

        logger.debug("DateDiff: left={}, right={}, result={}", left, right, abs);
        return abs;
    }
}