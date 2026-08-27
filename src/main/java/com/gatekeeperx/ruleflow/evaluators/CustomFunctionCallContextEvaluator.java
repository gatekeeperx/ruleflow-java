package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomFunctionCallContextEvaluator
        implements ContextEvaluator<RuleFlowLanguageParser.CustomFunctionCallContext> {

    @Override
    public Object evaluate(RuleFlowLanguageParser.CustomFunctionCallContext ctx, Visitor visitor)
            throws PropertyNotFoundException, UnexpectedSymbolException {

        String functionName = ctx.ID().getText();
        RuleflowFunction function = visitor.getFunctions().get(functionName);

        Map<String, Object> args = buildArgs(ctx, visitor);

        // Pre-resolution: if this call was resolved asynchronously ahead of time,
        // return the injected result and never invoke the function.
        String resolvedKey = com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey.of(functionName, args);
        Map<String, Object> resolvedFunctions = visitor.getResolvedFunctions();
        if (resolvedFunctions.containsKey(resolvedKey)) {
            return resolvedFunctions.get(resolvedKey);
        }

        if (function == null) {
            throw new UnexpectedSymbolException("Custom function '" + functionName + "' is not defined");
        }

        List<Object> cacheKey = new ArrayList<>();
        cacheKey.add(functionName);
        cacheKey.add(args);

        Map<List<Object>, Object> cache = visitor.getFunctionCallCache();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        try {
            Object result = function.apply(args);
            cache.put(cacheKey, result);
            return result;
        } catch (Exception e) {
            throw new UnexpectedSymbolException("Custom function '" + functionName + "' failed: " + e.getMessage());
        }
    }

    /**
     * Evaluates a custom-function call's arguments against the current visitor's
     * data, producing the resolved argument map. Named arguments keep their name;
     * positional arguments are keyed by their zero-based index as a string.
     * Shared with the pre-resolution extractor so extract-time and evaluate-time
     * argument resolution are identical.
     */
    public static Map<String, Object> buildArgs(
            RuleFlowLanguageParser.CustomFunctionCallContext ctx, Visitor visitor)
            throws PropertyNotFoundException, UnexpectedSymbolException {
        Map<String, Object> args = new LinkedHashMap<>();
        int positionalIndex = 0;
        for (RuleFlowLanguageParser.FuncCallArgContext argCtx : ctx.funcCallArg()) {
            if (argCtx.argName != null) {
                args.put(argCtx.argName.getText(), visitor.visit(argCtx.argValue));
            } else {
                args.put(String.valueOf(positionalIndex++), visitor.visit(argCtx.argValue));
            }
        }
        return args;
    }
}
