package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;

import com.gatekeeperx.ruleflow.utils.MapUtils;

import java.util.Map;

public class MemberAccessContextEvaluator
        implements ContextEvaluator<RuleFlowLanguageParser.MemberAccessContext> {

    @Override
    public Object evaluate(RuleFlowLanguageParser.MemberAccessContext ctx, Visitor visitor)
            throws PropertyNotFoundException, UnexpectedSymbolException {

        Object base = visitor.visit(ctx.base);
        String field = ctx.field.getText();

        if (base instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> map = (Map<String, ?>) base;
            if (!MapUtils.containsKeyIgnoreCase(map, field)) {
                throw new PropertyNotFoundException(field + " field cannot be found");
            }
            return MapUtils.getIgnoreCase(map, field);
        }
        throw new PropertyNotFoundException("Cannot access field '" + field + "' on " + base);
    }
}
