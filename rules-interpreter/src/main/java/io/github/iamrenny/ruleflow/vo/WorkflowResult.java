package io.github.iamrenny.ruleflow.vo;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class WorkflowResult {
    private String workflow;
    private String ruleSet;
    private String rule;
    private String result;
    private Set<String> actions;
    private Set<String> warnings;
    @Deprecated
    private Map<String, Map<String, String>> actionsWithParams;
    private List<Action> actionCalls;
    private WorkflowInfo workflowInfo;
    private boolean error;
    private List<MatchedRuleListItem> matchedRules;

    public WorkflowResult(String workflow, String ruleSet, String rule, String result, Set<String> actions, Set<String> warnings, Map<String, Map<String, String>> actionsWithParams, WorkflowInfo workflowInfo, List<Action> actionsList, boolean error) {
        this.workflow = workflow;
        this.ruleSet = ruleSet;
        this.rule = rule;
        this.result = result;
        this.actions = actions;
        this.warnings = warnings;
        this.actionsWithParams = actionsWithParams;
        this.actionCalls = actionsList != null ? actionsList : new ArrayList<>();
        this.workflowInfo = workflowInfo;
        this.error = error;
    }

    public WorkflowResult(String workflow, String result) {
        this(workflow, null, null, result, null, null, Map.of(), null, new ArrayList<>(), false);
    }

    public WorkflowResult(String workflow, String ruleset, String rule, String result, Set<String> warnings) {
        this.workflow = workflow;
        this.ruleSet = ruleset;
        this.rule = rule;
        this.result = result;
        this.warnings = warnings;
        this.actionsWithParams = new HashMap<>();
        this.actionCalls = new ArrayList<>();
        this.actions = new HashSet<>();
        this.error = false;
    }

    public WorkflowResult(String workflow, String ruleset, String rule, String result,  Map<String, Map<String, String>> actionsWithParams) {
        this.workflow = workflow;
        this.ruleSet = ruleset;
        this.rule = rule;
        this.result = result;
        this.actionsWithParams = actionsWithParams;
        this.actionCalls = convertActionsWithParamsToActionCalls(actionsWithParams);
        this.actions = actionsWithParams.keySet();
        this.warnings = new HashSet<>();
        this.error = false;
    }

    public WorkflowResult(String workflow, String ruleset, String rule, String result) {
        this.workflow = workflow;
        this.ruleSet = ruleset;
        this.rule = rule;
        this.result = result;
        this.actions = Set.of();
        this.warnings = Set.of();
        this.actionsWithParams = new HashMap<>();
        this.actionCalls = new ArrayList<>();
    }

    public WorkflowResult(String workflow, String ruleset, String rule, String result, Set<String> warnings, Map<String, Map<String,String>> actionsWithParams, boolean error) {
        this.workflow = workflow;
        this.ruleSet = ruleset;
        this.rule = rule;
        this.result = result;
        this.warnings = warnings;
        this.actions = Set.of();
        this.actionsWithParams = actionsWithParams;
        this.actionCalls = new ArrayList<>();
        this.error = error;
    }

    public WorkflowResult(String workflow, String ruleset, String rule, String result,  Map<String, Map<String,String>> actionsWithParams, List<MatchedRuleListItem> items, Set<String> warnings, boolean error) {
        this.workflow = workflow;
        this.ruleSet = ruleset;
        this.rule = rule;
        this.result = result;
        this.warnings = warnings;
        this.actionsWithParams = actionsWithParams;
        this.actionCalls = new ArrayList<>();
        this.error = error;
        this.matchedRules = items;
    }

    public WorkflowResult(String workflow, String ruleset, String rule, String result, Map<String, Map<String,String>> actionsWithParams, List<Action> actionCalls, List<MatchedRuleListItem> items, Set<String> warnings, boolean error) {
        this.workflow = workflow;
        this.ruleSet = ruleset;
        this.rule = rule;
        this.result = result;
        this.warnings = warnings;
        this.actionsWithParams = actionsWithParams;
        this.actionCalls = actionCalls != null ? new ArrayList<>(actionCalls) : new ArrayList<>();
        this.error = error;
        this.matchedRules = items;
    }
    public String getWorkflow() {
        return workflow;
    }

    public String getRuleSet() {
        return ruleSet;
    }

    public String getRule() {
        return rule;
    }

    public String getResult() {
        return result;
    }

    public Set<String> getActions() {
        if(actionsWithParams == null) {
            return null;
        }
            Map<String, Map<String, String>> actionsMap = actionsWithParams.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                    (existing, replacement) -> {
                        existing.putAll(replacement); // merging the two maps
                        return existing;
                    }));
            return actionsMap.keySet();
    }

    public Set<String> getWarnings() {
        return warnings;
    }

    @Deprecated
    public Map<String, Map<String, String>> getActionsWithParams() {
        return actionsWithParams;
    }

    public List<Action> getActionCalls() {
        return actionCalls;
    }

    public WorkflowInfo getWorkflowInfo() {
        return workflowInfo;
    }

    public boolean isError() {
        return error;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public void setRuleSet(String ruleSet) {
        this.ruleSet = ruleSet;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setWarnings(Set<String> warnings) {
        this.warnings = warnings;
    }

    @Deprecated
    public void setActionsWithParams(Map<String, Map<String, String>> actionsWithParams) {
        this.actionsWithParams = actionsWithParams;
        this.actionCalls = convertActionsWithParamsToActionCalls(actionsWithParams);
        this.actions = actionsWithParams.keySet();
    }

    @Deprecated
    public void setActionsWithParams(Map<String, Map<String, String>> actionsWithParams, boolean updateActionCalls) {
        this.actionsWithParams = actionsWithParams;
        if (updateActionCalls) {
            this.actionCalls = convertActionsWithParamsToActionCalls(actionsWithParams);
        }
        this.actions = actionsWithParams.keySet();
    }

    public void setActionCalls(List<Action> actionCalls) {
        this.actionCalls = actionCalls;
        this.actionsWithParams = convertActionCallsToActionsWithParams(actionCalls);
        this.actions = actionCalls.stream().map(Action::getName).collect(Collectors.toSet());
    }

    public void setActionCalls(List<Action> actionCalls, boolean updateActionsWithParams) {
        this.actionCalls = actionCalls;
        if (updateActionsWithParams) {
            this.actionsWithParams = convertActionCallsToActionsWithParams(actionCalls);
        }
        this.actions = actionCalls.stream().map(Action::getName).collect(Collectors.toSet());
    }

    public void setWorkflowInfo(WorkflowInfo workflowInfo) {
        this.workflowInfo = workflowInfo;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public List<MatchedRuleListItem> getMatchedRules() {
        return matchedRules;
    }

    public void setMatchedRules(List<MatchedRuleListItem> matchedRules) {
        this.matchedRules = matchedRules;
    }

    private static List<Action> convertActionsWithParamsToActionCalls(Map<String, Map<String, String>> actionsWithParams) {
        if (actionsWithParams == null) {
            return new ArrayList<>();
        }
        List<Action> actionCalls = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : actionsWithParams.entrySet()) {
            actionCalls.add(new Action(entry.getKey(), entry.getValue()));
        }
        return actionCalls;
    }

    private static Map<String, Map<String, String>> convertActionCallsToActionsWithParams(List<Action> actionCalls) {
        if (actionCalls == null) {
            return new HashMap<>();
        }
        Map<String, Map<String, String>> actionsWithParams = new HashMap<>();
        for (Action action : actionCalls) {
            actionsWithParams.merge(action.getName(), action.getParams(), (existing, replacement) -> {
                existing.putAll(replacement);
                return existing;
            });
        }
        return actionsWithParams;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WorkflowResult that)) {
            return false;
        }
      return isError() == that.isError() && Objects.equals(getWorkflow(), that.getWorkflow())
            && Objects.equals(getRuleSet(), that.getRuleSet()) && Objects.equals(
            getRule(), that.getRule()) && Objects.equals(getResult(), that.getResult())
            && Objects.equals(getActions(), that.getActions()) && Objects.equals(
            getWarnings(), that.getWarnings()) && Objects.equals(getActionsWithParams(),
            that.getActionsWithParams()) && Objects.equals(getActionCalls(),
            that.getActionCalls()) && Objects.equals(getWorkflowInfo(),
            that.getWorkflowInfo()) && Objects.equals(getMatchedRules(),
            that.getMatchedRules());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getWorkflow(), getRuleSet(), getRule(), getResult(), getActions(),
            getWarnings(), getActionsWithParams(), getActionCalls(), getWorkflowInfo(), isError(), getMatchedRules());
    }

    @Override
    public String toString() {
        return "WorkflowResult{" +
            "workflow='" + workflow + '\'' +
            ", ruleSet='" + ruleSet + '\'' +
            ", rule='" + rule + '\'' +
            ", result='" + result + '\'' +
            ", actions=" + actions +
            ", warnings=" + warnings +
            ", actionsWithParams=" + actionsWithParams +
            ", actionCalls=" + actionCalls +
            ", workflowInfo=" + workflowInfo +
            ", error=" + error +
            ", matchedRules=" + matchedRules +
            '}';
    }

    public static class MatchedRuleListItem {
        private String ruleSet;
        private String rule;
        private String result;
        private Set<String> actions;
        @Deprecated
        private Map<String, Map<String, String>> actionsWithParams;
        private List<Action> actionCalls;

        public MatchedRuleListItem(String ruleSet, String rule, String result,
            Set<String> actions, Map<String, Map<String, String>> actionsWithParams) {
            this.ruleSet = ruleSet;
            this.rule = rule;
            this.result = result;
            this.actions = actions;
            this.actionsWithParams = actionsWithParams;
            this.actionCalls = convertActionsWithParamsToActionCalls(actionsWithParams);
        }

        public MatchedRuleListItem(String ruleSet, String rule, String result,
            Set<String> actions, Map<String, Map<String, String>> actionsWithParams, List<Action> actionCalls) {
            this.ruleSet = ruleSet;
            this.rule = rule;
            this.result = result;
            this.actions = actions;
            this.actionsWithParams = actionsWithParams;
            this.actionCalls = actionCalls != null ? new ArrayList<>(actionCalls) : new ArrayList<>();
        }

        public String getRuleSet() {
            return ruleSet;
        }

        public void setRuleSet(String ruleSet) {
            this.ruleSet = ruleSet;
        }

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public Set<String> getActions() {
            return actions;
        }

        public void setActions(Set<String> actions) {
            this.actions = actions;
        }

        @Deprecated
        public Map<String, Map<String, String>> getActionsWithParams() {
            return actionsWithParams;
        }

        @Deprecated
        public void setActionsWithParams(
            Map<String, Map<String, String>> actionsWithParams) {
            this.actionsWithParams = actionsWithParams;
            this.actionCalls = WorkflowResult.convertActionsWithParamsToActionCalls(actionsWithParams);
        }

    }
}