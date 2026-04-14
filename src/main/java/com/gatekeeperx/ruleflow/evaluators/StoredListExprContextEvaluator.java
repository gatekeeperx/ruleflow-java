package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;

import java.util.List;

public class StoredListExprContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.StoredListExprContext> {

    @Override
    public Object evaluate(RuleFlowLanguageParser.StoredListExprContext ctx, Visitor visitor)
            throws PropertyNotFoundException, UnexpectedSymbolException {
        String listName = stripQuotes(ctx.listName.getText());
        List<?> list = visitor.getLists().get(listName);
        return list != null ? list : List.of();
    }

    private String stripQuotes(String quotedString) {
        if (quotedString.startsWith("'") && quotedString.endsWith("'")) {
            return quotedString.substring(1, quotedString.length() - 1);
        }
        return quotedString;
    }
}
