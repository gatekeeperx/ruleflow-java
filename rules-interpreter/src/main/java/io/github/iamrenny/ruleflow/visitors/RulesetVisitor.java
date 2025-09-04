package io.github.iamrenny.ruleflow.visitors;

import io.github.iamrenny.ruleflow.RuleFlowLanguageBaseVisitor;
import io.github.iamrenny.ruleflow.RuleFlowLanguageParser;
import io.github.iamrenny.ruleflow.errors.PropertyNotFoundException;
import io.github.iamrenny.ruleflow.errors.UnexpectedSymbolException;
import io.github.iamrenny.ruleflow.errors.ActionParameterResolutionException;
import io.github.iamrenny.ruleflow.utils.Pair;
import io.github.iamrenny.ruleflow.vo.Action;
import io.github.iamrenny.ruleflow.vo.WorkflowResult;
import io.github.iamrenny.ruleflow.vo.WorkflowResult.MatchedRuleListItem;
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

    public RulesetVisitor(Map<String, ?> data, Map<String, List<?>> lists) {
        this.data = data;
        this.lists = lists;
    }

    @Override
    public WorkflowResult visitParse(RuleFlowLanguageParser.ParseContext ctx) {
        return visitWorkflow(ctx.workflow());
    }

    @Override
    public WorkflowResult visitWorkflow(RuleFlowLanguageParser.WorkflowContext ctx) {
        Visitor visitor = new Visitor(data, lists, data);
        Set<String> warnings = new HashSet<>();
        List<WorkflowResult> matchedRules = new ArrayList<>();
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
                    Object visitedRule = visitor.visit(rule.rule_body().expr());
                    if (visitedRule instanceof Boolean && (Boolean) visitedRule) {
                        Object exprResult;
                        if (rule.rule_body().return_result().expr() != null) {
                            exprResult = visitor.visit(rule.rule_body().return_result().expr());
                        } else {
                            exprResult = rule.rule_body().return_result().state().ID().getText();
                        }
                        if(multiMatch) {
                            matchedRules.add(workflowResult(rule, ctx, ruleSet, exprResult, warnings));
                        } else {
                            return workflowResult(rule, ctx, ruleSet, exprResult, warnings);
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
        if(!matchedRules.isEmpty()) {
            return new WorkflowResult(
                ctx.workflow_name().getText().replace("'", ""),
                matchedRules.get(0).getRuleSet(),
                matchedRules.get(0).getRule(),
                matchedRules.get(0).getResult(),
                matchedRules.get(0).getActionsWithParams(),
                matchedRules.stream().map(it ->
                        new MatchedRuleListItem(it.getRuleSet(), it.getRule(),
                            it.getResult(), it.getActions(), it.getActionsWithParams()))
                    .toList(),
                warnings,
                error
            );
        }

        return resolveDefaultResult(ctx, warnings, error,  visitor);
    }

    private WorkflowResult resolveDefaultResult(
        RuleFlowLanguageParser.WorkflowContext ctx,
        Set<String> warnings,
        boolean error,
        Visitor evaluator) {
        List<Action> actionsList = new ArrayList<>();
        Map<String, Map<String, String>> actionsMap = new HashMap<>();

        if (ctx.default_clause().actions() != null) {
            Pair<List<Action>, Map<String, Map<String, String>>> resolvedActions = resolveActions(ctx.default_clause().actions());
            actionsList = resolvedActions.getKey();
            actionsMap = resolvedActions.getValue();
        }

        if (ctx.default_clause().return_result().expr() != null) {
            Object solvedExpr = evaluator.visit(ctx.default_clause().return_result().expr());
            return new WorkflowResult(
                ctx.workflow_name().getText().replace("'", ""),
                "default",
                "default",
                solvedExpr.toString(),
                warnings,
                actionsMap,
                error
            );
        } else if (ctx.default_clause().return_result().state() != null) {
            return new WorkflowResult(
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
        } else {
            throw new RuntimeException("No default result found");
        }
    }

    private WorkflowResult workflowResult(
        RuleFlowLanguageParser.RulesContext rule,
        RuleFlowLanguageParser.WorkflowContext ctx,
        RuleFlowLanguageParser.RulesetsContext ruleSet,
        Object expr,
        Set<String> warnings) {
        WorkflowResult result = new WorkflowResult(
            removeSingleQuote(ctx.workflow_name().getText()),
            removeSingleQuote(ruleSet.name().getText()),
            removeSingleQuote(rule.name().getText()),
            expr.toString(),
            warnings
        );
        if (rule.rule_body().actions() == null) {
            return result;
        } else {
            Pair<List<Action>, Map<String, Map<String, String>>> resolvedActions = resolveActions(rule.rule_body().actions());
            result.setActionsWithParams(resolvedActions.getValue());
            return result;
        }
    }

    private Pair<List<Action>, Map<String, Map<String, String>>> resolveActions(RuleFlowLanguageParser.ActionsContext rule) {
        Visitor visitor = new Visitor(data, lists, data);
        List<io.github.iamrenny.ruleflow.utils.Pair<String, Map<String, String>>> actions = new ActionsVisitor(visitor).visit(rule);
        List<Action> actionsList = actions.stream().map(action -> new Action(action.getKey(), action.getValue())).collect(Collectors.toList());
        Map<String, Map<String, String>> actionsMap = actions.stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (existing, replacement) -> {
                    existing.putAll(replacement); // merging the two maps
                    return existing;
                }));
        return new Pair<>(actionsList, actionsMap);
    }

    private String removeSingleQuote(String text) {
        return text.replace("'", "");
    }
}