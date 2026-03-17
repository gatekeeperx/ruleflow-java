package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;

import java.util.List;
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

        return function.apply(args);
    }
}
