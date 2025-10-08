import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class ReturnExprTest {

    @Test
    public void givenRuleWhenReturningValueMustReturnX() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y * z  = 15 return expr(x)
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "15");
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", 15,
            "y", 22,
            "z", 0
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenExpressionWithValidExpressionWhenReturningMustCalculateValue() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' bucket = '4' return expr(courier_model_response.score * city_features.percentile_q1)
                default 0
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "2.0", Set.of());
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "bucket", "4",
            "courier_model_response", Map.of("score", 0.5),
            "city_features", Map.of("percentile_q1", 4)
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenRuleWhenReturningExprMustReturnCorrectValue() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y * z  = 15 return expr(x + y)
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "37.0");
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", 15,
            "y", 22,
            "z", 0
        ));

        Assertions.assertEquals(expectedResult, result);
    }
}