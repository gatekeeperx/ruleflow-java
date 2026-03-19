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

        Object result;
        switch (ctx.op.getType()) {
            case RuleFlowLanguageLexer.ADD:
                if (canParseDouble(leftVal) && canParseDouble(rightVal)) {
                    result = Double.valueOf(leftVal.toString()) + Double.valueOf(rightVal.toString());
                } else {
                    result = leftVal.toString() + rightVal.toString();
                }
                break;
            case RuleFlowLanguageLexer.MINUS:
                result = Double.valueOf(leftVal.toString()) - Double.valueOf(rightVal.toString());
                break;
            default:
                throw new IllegalArgumentException("Operation not supported: " + ctx.op.getText());
        }
        logger.debug("MathAdd: left={}, right={}, op={}, result={}", leftVal, rightVal, ctx.op.getText(), result);
        return result;
    }

    private boolean canParseDouble(Object val) {
        if (val == null) return false;
        try {
            Double.parseDouble(val.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
