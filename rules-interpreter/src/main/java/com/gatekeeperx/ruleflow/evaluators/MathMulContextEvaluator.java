package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MathMulContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.MathMulContext> {
    private static final Logger logger = LoggerFactory.getLogger(MathMulContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.MathMulContext ctx, Visitor visitor) {
        Object leftVal = visitor.visit(ctx.left);
        Object rightVal = visitor.visit(ctx.right);

        Double left = Double.valueOf(leftVal.toString());
        Double right = Double.valueOf(rightVal.toString());
        Object result;
        switch (ctx.op.getType()) {
            case RuleFlowLanguageLexer.MULTIPLY:
                result = left * right;
                break;
            case RuleFlowLanguageLexer.DIVIDE:
                result = right == 0 ? 0.00d : left / right;
                break;
            case RuleFlowLanguageLexer.MODULO:
                result = left % right;
                break;
            default:
                throw new IllegalArgumentException("Operation not supported: " + ctx.op.getText());
        }
        logger.debug("MathMul: left={}, right={}, op={}, result={}", left, right, ctx.op.getText(), result);
        return result;
    }
}