package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.MathAddContext;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MathAddContextEvaluator implements ContextEvaluator<MathAddContext> {
    private static final Logger logger = LoggerFactory.getLogger(MathAddContextEvaluator.class);

    @Override
    public Object evaluate(MathAddContext ctx, Visitor visitor) {
        Object leftVal = visitor.visit(ctx.left);
        Object rightVal = visitor.visit(ctx.right);

        Double left = Double.valueOf(leftVal.toString());
        Double right = Double.valueOf(rightVal.toString());
        Object result;
        switch (ctx.op.getType()) {
            case RuleFlowLanguageLexer.ADD:
                result = left + right;
                break;
            case RuleFlowLanguageLexer.MINUS:
                result = left - right;
                break;
            default:
                throw new IllegalArgumentException("Operation not supported: " + ctx.op.getText());
        }
        logger.debug("MathAdd: left={}, right={}, op={}, result={}", left, right, ctx.op.getText(), result);
        return result;
    }
}