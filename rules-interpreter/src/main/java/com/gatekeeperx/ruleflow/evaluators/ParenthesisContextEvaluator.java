package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.ParenthesisContext;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParenthesisContextEvaluator implements ContextEvaluator<ParenthesisContext> {
    private static final Logger logger = LoggerFactory.getLogger(ParenthesisContextEvaluator.class);

    @Override
    public Object evaluate(ParenthesisContext ctx, Visitor visitor) {
        Object result = visitor.visit(ctx.expr());
        logger.debug("Parenthesis: expr={}, result={}", ctx.expr().getText(), result);
        return result;
    }
}