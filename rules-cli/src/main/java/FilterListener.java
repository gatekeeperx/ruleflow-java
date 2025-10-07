import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import org.antlr.v4.runtime.TokenStream;

public class FilterListener extends PrettyPrinter {
    private final TokenStream tokens;
    private final String criteria;

    public FilterListener(TokenStream tokens, String criteria) {
        super(tokens);
        this.tokens = tokens;
        this.criteria = criteria;
    }

    @Override
    public void enterRules(RuleFlowLanguageParser.RulesContext ctx) {
        if (tokens.getText(ctx.getStart(), ctx.getStop()).contains(criteria)) {
            super.enterRules(ctx);
        }
    }

    @Override
    public void exitRules(RuleFlowLanguageParser.RulesContext ctx) {
        if (tokens.getText(ctx.getStart(), ctx.getStop()).contains(criteria)) {
            super.exitRules(ctx);
        }
    }
}