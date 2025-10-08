package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryAndContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.BinaryAndContext> {
    private static final Logger logger = LoggerFactory.getLogger(BinaryAndContextEvaluator.class);

    @Override
    public Boolean evaluate(RuleFlowLanguageParser.BinaryAndContext ctx, Visitor visitor) {
        boolean b = (Boolean) visitor.visit(ctx.left) && (Boolean) visitor.visit(ctx.right);
        logger.debug("BinaryAnd: left={}, right={}, result={}", ctx.left, ctx.right, b);
        return b;
    }
}