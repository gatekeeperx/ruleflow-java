package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NowContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.NowContext> {
    private static final Logger logger = LoggerFactory.getLogger(NowContextEvaluator.class);
    @Override
    public Object evaluate(RuleFlowLanguageParser.NowContext ctx, Visitor visitor) {
        java.time.ZonedDateTime result = java.time.ZonedDateTime.now();
        logger.debug("Now: result={}", result);
        return result;
    }
} 