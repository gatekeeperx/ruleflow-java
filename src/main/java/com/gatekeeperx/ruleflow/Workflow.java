package com.gatekeeperx.ruleflow;

import com.gatekeeperx.ruleflow.listeners.ErrorListener;
import com.gatekeeperx.ruleflow.visitors.GrammarVisitor;
import com.gatekeeperx.ruleflow.visitors.RulesetVisitor;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;

import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
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
        return new RulesetVisitor(request, Map.of()).visit(tree);
    }

    public WorkflowResult evaluate(Map<String, Object> request,
                                   Map<String, List<?>> lists,
                                   Map<String, RuleflowFunction> functions) {
        return new RulesetVisitor(request, lists, functions).visit(tree);
    }

    public WorkflowResult evaluate(Map<String, Object> request,
                                   Map<String, List<?>> lists,
                                   Map<String, RuleflowFunction> functions,
                                   Map<String, Object> resolvedFunctions) {
        return new RulesetVisitor(request, lists, functions, resolvedFunctions).visit(tree);
    }

    public String validateAndGetWorkflowName() {
        return new GrammarVisitor().visit(tree);
    }

    /**
     * Extracts every custom-function call site in the workflow with its arguments
     * already resolved against {@code payload}, each carrying a canonical key.
     * No function is invoked. Intended for asynchronous pre-resolution: the
     * caller resolves each pending call ahead of time and passes the results back
     * to {@link #evaluate(Map, Map, Map, Map)}.
     */
    public java.util.List<com.gatekeeperx.ruleflow.vo.PendingFunctionCall> extractFunctionCalls(
            Map<String, Object> payload, Map<String, List<?>> lists) {
        com.gatekeeperx.ruleflow.visitors.FunctionCallExtractorVisitor extractor =
            new com.gatekeeperx.ruleflow.visitors.FunctionCallExtractorVisitor(payload, lists);
        extractor.visit(tree);
        return extractor.getPendingCalls();
    }
}