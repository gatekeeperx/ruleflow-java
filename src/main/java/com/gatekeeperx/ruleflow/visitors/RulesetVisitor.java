package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageBaseVisitor;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.errors.ActionParameterResolutionException;
import com.gatekeeperx.ruleflow.errors.TypeComparisonException;
import com.gatekeeperx.ruleflow.utils.Pair;
import com.gatekeeperx.ruleflow.vo.Action;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import com.gatekeeperx.ruleflow.vo.WorkflowResult.MatchedRuleListItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesetVisitor extends RuleFlowLanguageBaseVisitor<WorkflowResult> {
    private static final Logger logger = LoggerFactory.getLogger(RulesetVisitor.class);
    private final Map<String, ?> data;
    private final Map<String, List<?>> lists;
    private final Map<String, RuleflowFunction> functions;

    public RulesetVisitor(Map<String, ?> data, Map<String, List<?>> lists) {
        this(data, lists, Map.of());
    }

    public RulesetVisitor(Map<String, ?> data, Map<String, List<?>> lists,
                          Map<String, RuleflowFunction> functions) {
        this.data = data;
        this.lists = lists;
        this.functions = functions != null ? functions : Map.of();
    }

    @Override
    public WorkflowResult visitParse(RuleFlowLanguageParser.ParseContext ctx) {
        return visitWorkflow(ctx.workflow());
    }

    @Override
    public WorkflowResult visitWorkflow(RuleFlowLanguageParser.WorkflowContext ctx) {
        Visitor visitor = new Visitor(data, lists, data, functions);
        Set<String> warnings = new HashSet<>();
        List<WorkflowResult> matchedRules = new ArrayList<>();
        List<Action> accumulatedActions = new ArrayList<>();
        boolean error = false;
        boolean multiMatch = ctx.configuration() != null &&
            ctx.configuration().evaluation_mode() != null &&
            ctx.configuration().evaluation_mode().K_MULTI_MATCH() != null;

        for (RuleFlowLanguageParser.RulesetsContext ruleSet : ctx.rulesets()) {
            if (ruleSet.ruleset_condition() != null) {
                try {
                    Object result = visitor.visit(ruleSet.ruleset_condition().expr());
                    if (!(result instanceof Boolean) || !((Boolean) result)) {
                        continue;
                    }
                } catch (RuntimeException ex) {
                    if (ex.getCause() != null && ex.getCause() instanceof PropertyNotFoundException) {
                        logger.debug("Property not found in ruleset condition: {} {}", ctx.workflow_name().getText(), ruleSet.name().getText(), ex);
                        warnings.add(ex.getCause().getMessage());
                        continue;
                    } else if (ex.getCause() != null && ex.getCause() instanceof UnexpectedSymbolException) {
                        logger.warn("Unexpected symbol in ruleset condition: {} {}", ctx.workflow_name().getText(), ruleSet.name().getText(), ex);
                        warnings.add(ex.getCause().getMessage());
                        continue;
                    } else if (ex.getCause() != null && ex.getCause() instanceof ActionParameterResolutionException) {
                        logger.warn("Action parameter resolution failed in ruleset condition: {} {}", ctx.workflow_name().getText(), ruleSet.name().getText(), ex);
                        warnings.add(ex.getCause().getMessage());
                        continue;
                    } else if ((ex instanceof TypeComparisonException) || (ex.getCause() != null && ex.getCause() instanceof TypeComparisonException)) {
                        String rulesetName = removeSingleQuote(ruleSet.name().getText());
                        logger.warn("Type comparison error in ruleset condition {} {}", ctx.workflow_name().getText(), rulesetName, ex);
                        warnings.add("There is a comparison between different dataTypes in ruleset " + rulesetName);
                        continue;
                    } else {
                        logger.error("Error while evaluating ruleset condition {} {}",
                            ctx.workflow_name().getText(), ruleSet.name().getText(), ex);
                        warnings.add(ex.getMessage() != null ? ex.getMessage()
                            : "Unexpected Exception at " + ruleSet.getText());
                        error = true;
                        continue;
                    }
                }
            }
            for (RuleFlowLanguageParser.RulesContext rule : ruleSet.rules()) {
                try {
                    // expr? — null means always-true rule
                    Object visitedRule = rule.rule_body().expr() != null
                        ? visitor.visit(rule.rule_body().expr())
                        : Boolean.TRUE;

                    if (visitedRule instanceof Boolean && (Boolean) visitedRule) {
                        for (var setClause : rule.rule_body().set_clause()) {
                            Object value = visitor.visit(setClause.expr());
                            String rawName = setClause.variable.getText();
                            visitor.setVariable(rawName.substring(1), value);
                        }

                        if (rule.rule_body().K_THEN() != null) {
                            // THEN branch
                            Pair<List<Action>, Map<String, Map<String, String>>> resolved =
                                resolveActions(rule.rule_body().then_result);
                            if (rule.rule_body().K_CONTINUE() != null) {
                                // THEN + CONTINUE: accumulate actions, keep evaluating
                                accumulatedActions.addAll(resolved.getKey());
                                continue;
                            } else {
                                // THEN without CONTINUE: return with rule name as result
                                String exprResult = removeSingleQuote(rule.name().getText());
                                List<Action> allActions = mergeActions(accumulatedActions, resolved.getKey());
                                WorkflowResult wr = workflowResult(rule, ctx, ruleSet, exprResult, warnings, visitor, allActions);
                                if (multiMatch) {
                                    matchedRules.add(wr);
                                } else {
                                    return wr;
                                }
                            }
                        } else if (rule.rule_body().K_CONTINUE() != null) {
                            // CONTINUE only (no THEN): set vars and continue
                            continue;
                        } else {
                            // RETURN branch
                            Object exprResult;
                            if (rule.rule_body().result != null) {
                                if (rule.rule_body().result.expr() != null) {
                                    exprResult = visitor.visit(rule.rule_body().result.expr());
                                } else {
                                    exprResult = rule.rule_body().result.state().ID().getText();
                                }
                            } else {
                                // return_result absent — use rule name
                                exprResult = removeSingleQuote(rule.name().getText());
                            }
                            // Merge accumulated actions with this rule's own actions
                            List<Action> ruleActions = new ArrayList<>();
                            if (rule.rule_body().actions() != null) {
                                try {
                                    ruleActions = resolveActions(rule.rule_body().actions()).getKey();
                                } catch (ActionParameterResolutionException ex) {
                                    warnings.add(ex.getMessage());
                                }
                            }
                            List<Action> allActions = mergeActions(accumulatedActions, ruleActions);
                            WorkflowResult wr = workflowResult(rule, ctx, ruleSet, exprResult, warnings, visitor, allActions);
                            if (multiMatch) {
                                matchedRules.add(wr);
                            } else {
                                return wr;
                            }
                        }
                    }
                } catch (RuntimeException ex) {
                    if (ex.getCause() != null && ex.getCause() instanceof PropertyNotFoundException) {
                        logger.debug("Property not found: {} {}", ctx.workflow_name().getText(), rule.name().getText(), ex);
                        warnings.add(ex.getCause().getMessage());
                    } else if (ex.getCause() != null && ex.getCause() instanceof UnexpectedSymbolException) {
                        logger.warn("Unexpected symbol: {} {}", ctx.workflow_name().getText(), rule.name().getText(), ex);
                        warnings.add(ex.getCause().getMessage());
                    } else if (ex.getCause() != null && ex.getCause() instanceof ActionParameterResolutionException) {
                        logger.warn("Action parameter resolution failed: {} {}", ctx.workflow_name().getText(), rule.name().getText(), ex);
                        warnings.add(ex.getCause().getMessage());
                    } else if ((ex instanceof TypeComparisonException) || (ex.getCause() != null && ex.getCause() instanceof TypeComparisonException)) {
                        String ruleName = removeSingleQuote(rule.name().getText());
                        logger.warn("Type comparison error in rule {} {}", ctx.workflow_name().getText(), ruleName, ex);
                        warnings.add("There is a comparison between different dataTypes in rule " + ruleName);
                    } else {
                        logger.error("Error while evaluating rule {} {}",
                            ctx.workflow_name().getText(), rule.name().getText(), ex);
                        warnings.add(ex.getMessage() != null ? ex.getMessage()
                            : "Unexpected Exception at " + rule.getText());
                        error = true;
                    }
                }
            }
        }
        if (!matchedRules.isEmpty()) {
            WorkflowResult result = new WorkflowResult(
                ctx.workflow_name().getText().replace("'", ""),
                matchedRules.get(0).getRuleSet(),
                matchedRules.get(0).getRule(),
                matchedRules.get(0).getResult(),
                matchedRules.get(0).getActionsWithParams(),
                matchedRules.get(0).getActionCalls(),
                matchedRules.stream().map(it ->
                        new MatchedRuleListItem(it.getRuleSet(), it.getRule(),
                            it.getResult(), it.getActions(), it.getActionsWithParams(),
                            it.getActionCalls(), it.getVariables()))
                    .toList(),
                warnings,
                error
            );
            result.setVariables(new HashMap<>(visitor.getVariables()));
            return result;
        }

        return resolveDefaultResult(ctx, warnings, error, visitor, accumulatedActions);
    }

    private WorkflowResult resolveDefaultResult(
        RuleFlowLanguageParser.WorkflowContext ctx,
        Set<String> warnings,
        boolean error,
        Visitor evaluator,
        List<Action> accumulatedActions) {
        List<Action> actionsList = new ArrayList<>(accumulatedActions);
        Map<String, Map<String, String>> actionsMap = new HashMap<>();
        // populate map from accumulated actions
        accumulatedActions.forEach(a -> actionsMap.put(a.getName(), a.getParams()));

        if (ctx.default_clause().actions() != null) {
            Pair<List<Action>, Map<String, Map<String, String>>> resolvedActions = resolveActions(ctx.default_clause().actions());
            actionsList.addAll(resolvedActions.getKey());
            actionsMap.putAll(resolvedActions.getValue());
        }

        if (ctx.default_clause().return_result().expr() != null) {
            Object solvedExpr = evaluator.visit(ctx.default_clause().return_result().expr());
            WorkflowResult result = new WorkflowResult(
                ctx.workflow_name().getText().replace("'", ""),
                "default",
                "default",
                solvedExpr.toString(),
                warnings,
                actionsMap,
                error
            );
            result.setActionCalls(actionsList);
            result.setVariables(new HashMap<>(evaluator.getVariables()));
            return result;
        } else if (ctx.default_clause().return_result().state() != null) {
            WorkflowResult result = new WorkflowResult(
                removeSingleQuote(ctx.workflow_name().getText()),
                "default",
                "default",
                ctx.default_clause().return_result().state().ID().getText(),
                actionsMap.keySet(),
                warnings,
                actionsMap,
                null,
                actionsList,
                error
            );
            result.setVariables(new HashMap<>(evaluator.getVariables()));
            return result;
        } else {
            throw new RuntimeException("No default result found");
        }
    }

    private WorkflowResult workflowResult(
        RuleFlowLanguageParser.RulesContext rule,
        RuleFlowLanguageParser.WorkflowContext ctx,
        RuleFlowLanguageParser.RulesetsContext ruleSet,
        Object expr,
        Set<String> warnings,
        Visitor visitor,
        List<Action> allActions) {
        WorkflowResult result = new WorkflowResult(
            removeSingleQuote(ctx.workflow_name().getText()),
            removeSingleQuote(ruleSet.name().getText()),
            removeSingleQuote(rule.name().getText()),
            expr.toString(),
            warnings
        );
        result.setVariables(new HashMap<>(visitor.getVariables()));
        // Build actionsWithParams with copies to avoid mutating action param maps
        Map<String, Map<String, String>> actionsWithParams = new HashMap<>();
        for (Action a : allActions) {
            actionsWithParams.merge(a.getName(), new HashMap<>(a.getParams()), (existing, replacement) -> {
                existing.putAll(replacement);
                return existing;
            });
        }
        result.setActionCalls(allActions, false);
        result.setActionsWithParams(actionsWithParams, false);
        return result;
    }

    private List<Action> mergeActions(List<Action> accumulated, List<Action> ruleActions) {
        List<Action> merged = new ArrayList<>(accumulated);
        merged.addAll(ruleActions);
        return merged;
    }

    private Pair<List<Action>, Map<String, Map<String, String>>> resolveActions(RuleFlowLanguageParser.ActionsContext rule) {
        Visitor visitor = new Visitor(data, lists, data);
        List<com.gatekeeperx.ruleflow.utils.Pair<String, Map<String, String>>> actions = new ActionsVisitor(visitor).visit(rule);
        List<Action> actionsList = actions.stream().map(action -> new Action(action.getKey(), new HashMap<>(action.getValue()))).collect(Collectors.toList());
        Map<String, Map<String, String>> actionsMap = actions.stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashMap<>(entry.getValue()),
                (existing, replacement) -> {
                    existing.putAll(replacement);
                    return existing;
                }));
        return new Pair<>(actionsList, actionsMap);
    }

    private String removeSingleQuote(String text) {
        return text.replace("'", "");
    }
}
