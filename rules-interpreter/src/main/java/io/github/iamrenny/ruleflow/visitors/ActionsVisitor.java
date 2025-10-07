package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.utils.Pair;
import com.gatekeeperx.ruleflow.errors.ActionParameterResolutionException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ActionsVisitor {

    private final Visitor visitor;

    public ActionsVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public List<Pair<String, Map<String, String>>> visit(RuleFlowLanguageParser.ActionsContext context) {
        return context.action().stream()
            .map(action -> {
            String name;
            if (action.K_ACTION() != null) {
                name = action.param_value.getText().replace("'", "");
            } else if ("manual_review".equals(action.action_id.getText())) {
                name = action.action_id.getText().replace("'", "");
            } else if ("apply_restriction".equals(action.action_id.getText())) {
                name = action.action_id.getText().replace("'", "");
            } else if (action.action_id.getText() != null) {
                name = action.action_id.getText().replace("'", "");
            } else {
                throw new IllegalArgumentException("Cannot find action name or identifier in " + action.getText());
            }
            return new Pair<>(name, new ActionVisitor(visitor).visit(action));
        })
        .collect(Collectors.toList());
    }
}

class ActionVisitor {

    private final Visitor visitor;

    public ActionVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public Map<String, String> visit(RuleFlowLanguageParser.ActionContext action) {
        if (action.action_params() != null) {
            return action.action_params().param_pairs().param_pair().stream()
                .collect(Collectors.toMap(
                    a -> a.field_name.getText().replace("'", ""),
                    a -> evaluateActionParamValue(a.field_value)
            ));
        } else {
            return Map.of();
        }
    }

    private String evaluateActionParamValue(RuleFlowLanguageParser.ActionParamValueContext fieldValue) {
        if (fieldValue.validValue() != null) {
            // Handle string literals, numbers, booleans, etc.
            return fieldValue.validValue().getText().replace("'", "");
        } else if (fieldValue.validProperty() != null) {
            // Handle property references - resolve the actual value from the request data
            try {
                Object resolvedValue = visitor.visit(fieldValue.validProperty());
                return resolvedValue != null ? resolvedValue.toString() : "null";
            } catch (Exception e) {
                // If property resolution fails, throw a specific exception
                throw new ActionParameterResolutionException(
                    "Failed to resolve property '" + fieldValue.validProperty().getText() + "' in action parameter: " + e.getMessage()
                );
            }
        } else {
            throw new IllegalArgumentException("Unsupported action parameter value type: " + fieldValue.getText());
        }
    }
}