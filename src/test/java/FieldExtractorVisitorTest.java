import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.ParseContext;
import com.gatekeeperx.ruleflow.visitors.FieldExtractorVisitor;
import java.util.Set;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

class FieldExtractorVisitorTest {

  private ParseContext parse(String input) {
    CharStream charStream = CharStreams.fromString(input);
    RuleFlowLanguageLexer lexer = new RuleFlowLanguageLexer(charStream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    RuleFlowLanguageParser parser = new RuleFlowLanguageParser(tokens);
    return parser.parse();
  }

  @Test
  void testFieldExtraction() {
    String workflow = """
        WORKFLOW 'TestWorkflow'
            RULESET 'Main'
                'CheckAmount'
                
                    (amount > 1000) AND (features.gmv_user_1d > 50)
                    THEN
                    action('notify', {'type': 'email'})
               

                'CheckList'
                
                    country IN list('cardBins')
                    THEN
                    action('block', {'reason': 'geo'})
                
            DEFAULT
            RETURN 'approved'
            WITH action('mark_approved')
        END
        """;

    FieldExtractorVisitor visitor = new FieldExtractorVisitor();
    ParseTree tree = parse(workflow);
    visitor.visit(tree);

    Set<String> inputs = visitor.getInputFields();
    Set<String> features = visitor.getFeatureFields();
    Set<String> lists = visitor.getListNames();

    assertEquals(Set.of("amount", "country"), inputs);
    assertEquals(Set.of("gmv_user_1d"), features);
    assertEquals(Set.of("cardBins"), lists);
  }

  @Test
  void testEvalInListExtractsListName() {
    String workflow = """
        WORKFLOW 'TestWorkflow'
            RULESET 'Main'
                'CheckBlacklist'
                    evalInList('blacklistedUsers', elem.userId == userId)
                    THEN
                    action('block')

            DEFAULT
            RETURN 'approved'
        END
        """;

    FieldExtractorVisitor visitor = new FieldExtractorVisitor();
    ParseTree tree = parse(workflow);
    visitor.visit(tree);

    Set<String> inputs = visitor.getInputFields();
    Set<String> lists = visitor.getListNames();

    assertEquals(Set.of("blacklistedUsers"), lists);
    assertEquals(Set.of("elem.userId", "userId"), inputs);
  }
}