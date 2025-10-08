package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.visitors.Visitor;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateAddContextEvaluator implements ContextEvaluator<com.gatekeeperx.ruleflow.RuleFlowLanguageParser.DateAddContext> {
    private static final Logger logger = LoggerFactory.getLogger(DateAddContextEvaluator.class);
    @Override
    public Object evaluate(com.gatekeeperx.ruleflow.RuleFlowLanguageParser.DateAddContext ctx, Visitor visitor) {
        java.time.ZonedDateTime zdt = (ZonedDateTime) new DateValueContextEvaluator().evaluate(ctx.date, visitor);
        Object unitObj = ctx.unit.getText();
        long amount = ((Double) visitor.visit(ctx.amount)).longValue();
        String unit = unitObj.toString();
        switch (unit.toLowerCase()) {
            case "day":
                return zdt.plusDays(amount);
            case "hour":
                return zdt.plusHours(amount);
            case "minute":
                return zdt.plusMinutes(amount);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
} 