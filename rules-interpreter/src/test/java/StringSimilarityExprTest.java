import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.Map;

class StringSimilarityExprTest {
    @Test
    void testStringDistanceExpr() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'sim' string_distance('ganchozojose572', 'JOSEGANCHOZO') = 27 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        System.out.println("string_distance result: " + result.getRule());
        Assertions.assertEquals("sim", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    void testPartialRatioExpr() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'sim' partial_ratio('ganchozojose572', 'JOSEGANCHOZO') > 0 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        System.out.println("partial_ratio result: " + result.getRule());
        Assertions.assertEquals("sim", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    void testTokenSortRatioExpr() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'sim' token_sort_ratio('ganchozojose572', 'JOSEGANCHOZO') = 27 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        System.out.println("token_sort_ratio result: " + result.getRule());
        Assertions.assertEquals("sim", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    void testTokenSetRatioExpr() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'sim' token_set_ratio('ganchozojose572', 'JOSEGANCHOZO') > 0 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        System.out.println("token_set_ratio result: " + result.getRule());
        Assertions.assertEquals("sim", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    void testStringSimilarityScoreExpr() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'sim' string_similarity_score('ganchozojose572', 'JOSEGANCHOZO') > 0 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        System.out.println("string_similarity_score result: " + result.getRule());
        Assertions.assertEquals("sim", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }
} 