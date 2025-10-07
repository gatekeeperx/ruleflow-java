package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import com.gatekeeperx.ruleflow.utils.DateTimeUtils;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateSubtractContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.DateSubtractContext> {
    private static final Logger logger = LoggerFactory.getLogger(DateSubtractContextEvaluator.class);
    @Override
    public Object evaluate(RuleFlowLanguageParser.DateSubtractContext ctx, Visitor visitor) {
        java.time.ZonedDateTime zdt = (ZonedDateTime) new DateValueContextEvaluator().evaluate(ctx.dateValue(), visitor);
        long amount = ((Double) visitor.visit(ctx.amount)).longValue();
        Object unitObj = ctx.unit.getText();
        String unit = unitObj.toString();
        switch (unit.toLowerCase()) {
            case "day":
                return zdt.minusDays(amount);
            case "hour":
                return zdt.minusHours(amount);
            case "minute":
                return zdt.minusMinutes(amount);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
} 