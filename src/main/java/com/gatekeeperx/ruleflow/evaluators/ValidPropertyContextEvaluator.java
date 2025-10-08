package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.ValidPropertyContext;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ValidPropertyContextEvaluator implements ContextEvaluator<ValidPropertyContext> {
    private static final Logger logger = LoggerFactory.getLogger(ValidPropertyContextEvaluator.class);

    @Override
    public Object evaluate(ValidPropertyContext ctx, Visitor visitor) throws PropertyNotFoundException {
        String property = ctx.getText();
        Object result = visitor.getData().get(property);
        logger.debug("ValidProperty: property={}, result={}", property, result);
        if (ctx.property != null) {
            if (result == null) {
                throw new PropertyNotFoundException(ctx.ID(0).getText() + " field cannot be found");
            }
            return result;
        } else if (ctx.nestedProperty != null) {
            Map<String, ?> data = (ctx.root != null) ? visitor.getRoot() : visitor.getData();
            return getNestedValue(ctx, data);
        } else {
            throw new PropertyNotFoundException(ctx.getText() + " field cannot be found");
        }
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(ValidPropertyContext ctx, Map<String, ?> data) throws PropertyNotFoundException {
        Map<String, ?> currentData = data;
        for (var id : ctx.ID()) {
            Object value = currentData.get(id.getText());
            if (value instanceof Map<?, ?>) {
                currentData = (Map<String, ?>) value;
            } else {
                if (value == null) {
                    throw new PropertyNotFoundException(id.getText() + " field cannot be found");
                }
                return value;
            }
        }
        throw new PropertyNotFoundException(ctx.getText() + " cannot be found");
    }
}