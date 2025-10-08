package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.UnaryContext;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnaryContextEvaluator implements ContextEvaluator<UnaryContext> {
    private static final Logger logger = LoggerFactory.getLogger(UnaryContextEvaluator.class);

    @Override
    public Object evaluate(UnaryContext ctx, Visitor visitor) {
        Object valLeft = visitor.visit(ctx.left);
        Double left = toDoubleOrNull(valLeft.toString());

        if (left == null) {
            throw new IllegalArgumentException("Parameter value not supported: " + valLeft);
        }

        Object result;
        switch (ctx.op.getType()) {
            case RuleFlowLanguageLexer.ABS:
                result = Math.abs(left);
                break;
            default:
                throw new IllegalArgumentException("Operation not supported: " + ctx.op.getText());
        }
        logger.debug("Unary: op={}, left={}, result={}", ctx.op.getText(), left, result);
        return result;
    }

    private Double toDoubleOrNull(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}