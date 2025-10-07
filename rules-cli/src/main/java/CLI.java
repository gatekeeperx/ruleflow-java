import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatekeeperx.ruleflow.listeners.ErrorListener;
import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.RulesetVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashMap;
import java.util.Map;

public class CLI {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String evaluate(String inputData, String workflow) {
        try {
            // Convert input JSON data to Map<String, Object>
            JsonNode inputJson = objectMapper.readTree(inputData);
            Map<String, Object> dataMap = objectMapper.convertValue(inputJson, Map.class);

            // Setup ANTLR lexer, parser, and visitor
            var input = CharStreams.fromString(workflow);
            var lexer = new RuleFlowLanguageLexer(input);
            var tokens = new CommonTokenStream(lexer);
            var parser = new RuleFlowLanguageParser(tokens);
            parser.addErrorListener(new ErrorListener());

            ParseTree tree = parser.parse();
            var visitor = new RulesetVisitor(dataMap, new HashMap<>());

            // Visit the tree and return the evaluation result
            return visitor.visit(tree).toString();
        } catch (Exception e) {
            return "Evaluation error: " + e.getMessage();
        }
    }

    private String runRule(String inputData, String rule) {
        try {
            JsonNode inputJson = objectMapper.readTree(inputData);
            Map<String, Object> dataMap = objectMapper.convertValue(inputJson, Map.class);
            return new RuleEvaluator(rule, dataMap, Map.of()).evaluate();
        } catch (Exception e) {
            return "Run error: " + e.getMessage();
        }
    }
}