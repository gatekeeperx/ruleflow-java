import com.gatekeeperx.ruleflow.visitors.Visitor;
import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;
import java.util.Set;

public class RuleEvaluator {
    private final String inputRule;
    private final Map<String, ?> data;
    private final Map<String, Set<String>> lists;
    private final SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();

    public RuleEvaluator(String inputRule, Map<String, ?> data, Map<String, Set<String>> lists) {
        this.inputRule = inputRule;
        this.data = data;
        this.lists = lists;
    }

    public String evaluate() {
        var input = CharStreams.fromString(inputRule);
        var lexer = new RuleFlowLanguageLexer(input);
        var tokens = new CommonTokenStream(lexer);
        var parser = new RuleFlowLanguageParser(tokens);

        // Remove default error listeners and add custom listener
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrorListener);

        ParseTree ctx = parser.expr();
        ParserRuleContext parserContext = (ParserRuleContext) ctx;

        // If there is a syntax error, return the error message
        if (syntaxErrorListener.isFailed()) {
            return "error: " + syntaxErrorListener.getErrorMessage() +
                    "\n" + tokens.getText(parserContext.getStart(), parserContext.getStop());
        }

        var ruleEvaluator = new Visitor(data, lists, data);

        try {
            return ruleEvaluator.visit(ctx).toString();
        } catch (Exception ex) {
            System.out.println("Error while evaluating rule " + tokens.getText(parserContext.getStart(), parserContext.getStop()) + ": " + ex.getMessage());
            return "error: " + ex.getMessage() + "\n" + tokens.getText(parserContext.getStart(), parserContext.getStop());
        }
    }
}

class SyntaxErrorListener extends BaseErrorListener {
    private boolean failed = false;
    private String errorMessage;

    public boolean isFailed() {
        return failed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        errorMessage = "Error at line " + line + ":" + charPositionInLine + " - " + msg;
        failed = true;
    }
}