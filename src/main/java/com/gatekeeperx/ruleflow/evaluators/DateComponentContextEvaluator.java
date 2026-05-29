package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

public class DateComponentContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.DateComponentContext> {
    private static final Logger logger = LoggerFactory.getLogger(DateComponentContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.DateComponentContext ctx, Visitor visitor) {
        ZonedDateTime value = (ZonedDateTime) new DateValueContextEvaluator().evaluate(ctx.left, visitor);

        if (value == null) {
            throw new IllegalArgumentException("Parameter value not supported: " + ctx.left.getText());
        }

        long result;
        switch (ctx.op.getType()) {
            case RuleFlowLanguageLexer.K_YEAR:
                result = value.getYear();
                break;
            case RuleFlowLanguageLexer.K_MONTH:
                result = value.getMonthValue();
                break;
            case RuleFlowLanguageLexer.DAY:
                result = value.getDayOfMonth();
                break;
            case RuleFlowLanguageLexer.HOUR:
                result = value.getHour();
                break;
            case RuleFlowLanguageLexer.MINUTE:
                result = value.getMinute();
                break;
            default:
                throw new IllegalArgumentException("Operation not supported: " + ctx.op.getText());
        }
        logger.debug("DateComponent: op={}, dateValue={}, result={}", ctx.op.getText(), ctx.left.getText(), result);
        return result;
    }
}
