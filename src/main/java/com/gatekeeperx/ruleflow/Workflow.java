package com.gatekeeperx.ruleflow;

import com.gatekeeperx.ruleflow.listeners.ErrorListener;
import com.gatekeeperx.ruleflow.visitors.GrammarVisitor;
import com.gatekeeperx.ruleflow.visitors.RulesetVisitor;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;

import java.util.Map;

public class Workflow {

    private final com.gatekeeperx.ruleflow.RuleFlowLanguageParser.ParseContext tree;

    public Workflow(String workflow) {
        CharStream input = CharStreams.fromString(workflow);
        com.gatekeeperx.ruleflow.RuleFlowLanguageLexer lexer = new com.gatekeeperx.ruleflow.RuleFlowLanguageLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        com.gatekeeperx.ruleflow.RuleFlowLanguageParser parser = new com.gatekeeperx.ruleflow.RuleFlowLanguageParser(tokens);
        parser.addErrorListener(new ErrorListener());
        this.tree = parser.parse();
    }

    public WorkflowResult evaluate(Map<String, Object> request, Map<String, List<?>> list) {
        return new RulesetVisitor(request, list).visit(tree);
    }

    public WorkflowResult evaluate(Map<String, Object> request) {
        return evaluate(request, Map.of());
    }

    public String validateAndGetWorkflowName() {
        return new GrammarVisitor().visit(tree);
    }
}