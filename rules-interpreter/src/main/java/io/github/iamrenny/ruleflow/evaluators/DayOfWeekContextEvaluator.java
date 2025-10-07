package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class DayOfWeekContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.DayOfWeekContext> {
    private static final Logger logger = LoggerFactory.getLogger(DayOfWeekContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.DayOfWeekContext ctx, Visitor visitor) {
        ZonedDateTime valLeft = (ZonedDateTime) new DateValueContextEvaluator().evaluate(ctx.dateValue(), visitor);

        if (valLeft == null) {
            throw new IllegalArgumentException("Parameter value not supported: " + ctx.dateValue().getText());
        }

        Object result;
        switch (ctx.op.getType()) {
            case RuleFlowLanguageLexer.DAY_OF_WEEK:
                result = valLeft.getDayOfWeek().toString();
                break;
            default:
                throw new IllegalArgumentException("Operation not supported: " + ctx.op.getText());
        }
        logger.debug("DayOfWeek: dateValue={}, result={}", ctx.dateValue().getText(), result);
        return result;
    }
}