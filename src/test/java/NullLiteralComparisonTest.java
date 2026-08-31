import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class NullLiteralComparisonTest {

    @Test
    public void givenPresentNonNullValueWhenComparedNotEqualNullThenMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' testField <> null return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "block", Set.of());
        WorkflowResult result = ruleEngine.evaluate(Map.of("testField", "some-value"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenPresentNonNullValueWhenComparedEqualNullThenMustNotMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' testField = null return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of("testField", "some-value"));

        Assertions.assertEquals("allow", result.getResult());
        Assertions.assertEquals(Set.of(), result.getWarnings());
    }
}
