import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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

    // An explicit null value in the payload is intercepted by property resolution before it
    // ever reaches the comparator (same as a fully absent field, see NullTest), so it can't
    // reach compareNull either. Both operators degrade to "field cannot be found" + no match.

    @Test
    public void givenExplicitNullValueWhenComparedEqualNullThenMustNotMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' testField = null return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
                "test", "default", "default", "allow",
                Set.of("testField field cannot be found")
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("testField", null);
        WorkflowResult result = ruleEngine.evaluate(payload);

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenExplicitNullValueWhenComparedNotEqualNullThenMustNotMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' testField <> null return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
                "test", "default", "default", "allow",
                Set.of("testField field cannot be found")
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("testField", null);
        WorkflowResult result = ruleEngine.evaluate(payload);

        Assertions.assertEquals(expectedResult, result);
    }
}
