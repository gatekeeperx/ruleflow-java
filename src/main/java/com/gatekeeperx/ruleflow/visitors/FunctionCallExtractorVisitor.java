package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageBaseVisitor;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.evaluators.CustomFunctionCallContextEvaluator;
import com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey;
import com.gatekeeperx.ruleflow.vo.PendingFunctionCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks a workflow tree and, for every custom-function call site, evaluates its
 * arguments against the payload (using the same machinery as evaluation) to
 * produce a {@link PendingFunctionCall}. Does not invoke any function. Call
 * sites whose arguments cannot be resolved without invoking another function
 * (nested async) are skipped with a warning.
 */
public class FunctionCallExtractorVisitor extends RuleFlowLanguageBaseVisitor<Void> {

    private static final Logger logger = LoggerFactory.getLogger(FunctionCallExtractorVisitor.class);

    private final Visitor argEvaluator;
    private final Map<String, PendingFunctionCall> byKey = new LinkedHashMap<>();

    public FunctionCallExtractorVisitor(Map<String, ?> payload, Map<String, List<?>> lists) {
        // No functions map: any nested custom-function arg will throw and be skipped.
        this.argEvaluator = new Visitor(payload, lists != null ? lists : Map.of(), payload);
    }

    @Override
    public Void visitCustomFunctionCall(RuleFlowLanguageParser.CustomFunctionCallContext ctx) {
        String functionName = ctx.ID().getText();
        try {
            Map<String, Object> args = CustomFunctionCallContextEvaluator.buildArgs(ctx, argEvaluator);
            String key = RuleflowFunctionKey.of(functionName, args);
            byKey.putIfAbsent(key, new PendingFunctionCall(functionName, args, key));
        } catch (Exception e) {
            logger.warn("Skipping pre-resolution of '{}': arguments could not be resolved ({})",
                functionName, e.getMessage());
        }
        // Do not descend into the call's own arguments for further extraction.
        return null;
    }

    public List<PendingFunctionCall> getPendingCalls() {
        return new ArrayList<>(byKey.values());
    }
}
