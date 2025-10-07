package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.ValueContext;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import com.gatekeeperx.ruleflow.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;


public class ValueContextEvaluator implements ContextEvaluator<ValueContext> {
    private static final Logger logger = LoggerFactory.getLogger(ValueContextEvaluator.class);

    @Override
    public Object evaluate(ValueContext ctx, Visitor visitor) {
        Object result;
        if (ctx.validValue().string != null) {
            result = (ctx.validValue().string.getText()).replace("'", "");
        } else if (ctx.validValue().number != null) {
            result = Double.valueOf(ctx.validValue().number.getText());
        } else if (ctx.validValue().nullValue != null) {
            result = ctx.validValue().nullValue;
        } else if (ctx.validValue().currentDate != null) {
            result = LocalDateTime.now();
        } else if (ctx.validValue().booleanLiteral != null) {
            result = Boolean.parseBoolean(ctx.validValue().booleanLiteral.getText());
        } else {
            throw new IllegalArgumentException("Value not supported: " + ctx.getText());
        }
        logger.debug("Value: ctx={}, result={}", ctx.getText(), result);
        return result;
    }
}