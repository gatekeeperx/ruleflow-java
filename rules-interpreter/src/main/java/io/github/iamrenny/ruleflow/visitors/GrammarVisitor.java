package com.gatekeeperx.ruleflow.visitors;


import com.gatekeeperx.ruleflow.RuleFlowLanguageBaseVisitor;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;

public class GrammarVisitor extends RuleFlowLanguageBaseVisitor<String> {

    @Override
    public String visitParse(RuleFlowLanguageParser.ParseContext ctx) {
        return removeSingleQuote(ctx.workflow().workflow_name().getText());
    }

    private String removeSingleQuote(String text) {
        return text.replace("'", "");
    }
}