package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomFunctionCallContextEvaluator
        implements ContextEvaluator<RuleFlowLanguageParser.CustomFunctionCallContext> {

    @Override
    public Object evaluate(RuleFlowLanguageParser.CustomFunctionCallContext ctx, Visitor visitor)
            throws PropertyNotFoundException, UnexpectedSymbolException {

        String functionName = ctx.ID().getText();
        RuleflowFunction function = visitor.getFunctions().get(functionName);
        if (function == null) {
            throw new UnexpectedSymbolException("Custom function '" + functionName + "' is not defined");
        }

        List<Object> args = ctx.expr().stream()
                .map(visitor::visit)
                .collect(Collectors.toList());

        List<Object> cacheKey = new ArrayList<>();
        cacheKey.add(functionName);
        cacheKey.addAll(args);

        Map<List<Object>, Object> cache = visitor.getFunctionCallCache();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        Object result = function.apply(args);
        cache.put(cacheKey, result);
        return result;
    }
}
