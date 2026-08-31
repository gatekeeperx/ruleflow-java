import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class BinaryOrShortCircuitTest {

    @Test
    public void givenOrExpressionWhenLeftIsTrueAndRightReferencesMissingPropertyThenMustShortCircuitToTrue() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x = true OR features.missing_feature >= 4 return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "block", Set.of());
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", true,
            "features", Map.of()
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenOrExpressionWhenLeftIsFalseAndRightReferencesMissingPropertyThenMustSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x = true OR features.missing_feature >= 4 return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", false,
            "features", Map.of()
        ));

        Assertions.assertEquals("allow", result.getResult());
        Assertions.assertEquals(Set.of("missing_feature field cannot be found"), result.getWarnings());
    }
}
