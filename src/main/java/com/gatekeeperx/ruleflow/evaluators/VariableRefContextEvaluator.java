package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.VariableRefContext;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.visitors.Visitor;

public class VariableRefContextEvaluator implements ContextEvaluator<VariableRefContext> {

    @Override
    public Object evaluate(VariableRefContext ctx, Visitor visitor) throws PropertyNotFoundException {
        String name = ctx.VARIABLE().getText().substring(1); // strip leading "$"
        if (!visitor.getVariables().containsKey(name)) {
            throw new PropertyNotFoundException("Variable $" + name + " is not defined");
        }
        return visitor.getVariables().get(name);
    }
}
