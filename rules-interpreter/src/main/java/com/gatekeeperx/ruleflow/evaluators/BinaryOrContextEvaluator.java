package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryOrContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.BinaryOrContext> {
    private static final Logger logger = LoggerFactory.getLogger(BinaryOrContextEvaluator.class);

    @Override
    public Boolean evaluate(RuleFlowLanguageParser.BinaryOrContext ctx, Visitor visitor) {
        boolean res = (Boolean) visitor.visit(ctx.left) || (Boolean) visitor.visit(ctx.right);
        logger.debug("BinaryOr: left={}, right={} result={}", visitor.visit(ctx.left), visitor.visit(ctx.right), res);
        return res;
    }
}