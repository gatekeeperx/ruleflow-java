package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullCheckContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.NullCheckContext> {
    private static final Logger logger = LoggerFactory.getLogger(NullCheckContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.NullCheckContext ctx, Visitor visitor)
            throws PropertyNotFoundException {
        Object value;
        try {
            value = visitor.visit(ctx.value);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof PropertyNotFoundException) {
                value = null;
            } else {
                throw e;
            }
        }

        boolean result = switch (ctx.check.getType()) {
            case RuleFlowLanguageParser.K_NULL -> value == null;
            case RuleFlowLanguageParser.K_EMPTY -> value == null ||
                    (value instanceof String s && s.isEmpty());
            case RuleFlowLanguageParser.K_BLANK -> value == null ||
                    (value instanceof String s && s.trim().isEmpty());
            default -> false;
        };

        boolean negated = ctx.not != null;
        if (negated) {
            result = !result;
        }

        logger.debug("NullCheck: value={}, check={}, negated={}, result={}", value, ctx.check.getText(), negated, result);
        return result;
    }
}
