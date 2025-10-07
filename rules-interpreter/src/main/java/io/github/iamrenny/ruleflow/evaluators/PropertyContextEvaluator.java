package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.PropertyContext;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyContextEvaluator implements ContextEvaluator<PropertyContext> {
    private static final Logger logger = LoggerFactory.getLogger(PropertyContextEvaluator.class);

    @Override
    public Object evaluate(PropertyContext ctx, Visitor visitor) throws PropertyNotFoundException {
        ValidPropertyContextEvaluator validPropertyCondition = new ValidPropertyContextEvaluator();
        return validPropertyCondition.evaluate(ctx.validProperty(), visitor);
    }
}