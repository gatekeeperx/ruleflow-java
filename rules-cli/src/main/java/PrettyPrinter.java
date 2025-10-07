import com.gatekeeperx.ruleflow.RuleFlowLanguageBaseListener;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import org.antlr.v4.runtime.TokenStream;

public class PrettyPrinter extends RuleFlowLanguageBaseListener {
    private final TokenStream tokens;
    private int indentLevel = 0;

    public PrettyPrinter(TokenStream tokens) {
        this.tokens = tokens;
    }

    @Override
    public void enterWorkflow(RuleFlowLanguageParser.WorkflowContext ctx) {
        System.out.print("workflow ");
        indentLevel++;
    }

    @Override
    public void exitWorkflow(RuleFlowLanguageParser.WorkflowContext ctx) {
        indentLevel--;
    }

    @Override
    public void enterWorkflow_name(RuleFlowLanguageParser.Workflow_nameContext ctx) {
        System.out.println(ctx.getText());
    }

    @Override
    public void enterRulesets(RuleFlowLanguageParser.RulesetsContext ctx) {
        printIndent();
        System.out.println("ruleset " + ctx.name().getText());
        indentLevel++;
    }

    @Override
    public void exitRulesets(RuleFlowLanguageParser.RulesetsContext ctx) {
        indentLevel--;
    }

    @Override
    public void enterRules(RuleFlowLanguageParser.RulesContext ctx) {
        printIndent();
        String exprTokens = tokens.getText(ctx.getStart(), ctx.getStop());
        System.out.println(exprTokens);
        indentLevel++;
    }

    public void exitRules(RuleFlowLanguageParser.RulesContext ctx) {
        indentLevel--;
    }

    public void enterDefault_result(RuleFlowLanguageParser.Default_clauseContext ctx) {
        printIndent();
        System.out.println("default " + ctx.getText());
    }

    public void exitDefault_result(RuleFlowLanguageParser.Default_clauseContext ctx) {
        indentLevel--;
        printIndent();
        System.out.println("end");
    }

    protected void printIndent() {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  ");
        }
    }
}