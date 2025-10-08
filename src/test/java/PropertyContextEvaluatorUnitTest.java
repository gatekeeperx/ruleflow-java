import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;
import java.util.Set;

class PropertyContextEvaluatorUnitTest {

    @Test
    public void givenWorkflowWithUndefinedPropertyWhenEvaluatingMustReturnException() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review')
                default allow
            end
        """;
        Map<String, Object> request = Map.of("not_a_user_id", 10);
        WorkflowResult result = new Workflow(workflow).evaluate(request);

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow",
            Set.of("user_id field cannot be found")
        );

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenWorkflowWithPropertyWhenEvaluatingMustReturnRootValue() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new Workflow(workflow).evaluate(request);

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block"
        );

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenWorkflowWithRootAndInnerFieldWithExactNameMustCompareWithRootValue() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' request.orders.any { compare_order_id = .request.order_id } return block
                default allow
            end
        """;

        Map<String, Object> request = Map.of(
            "request", Map.of(
                "order_id", "A",
                "orders", List.of(
                    Map.of(
                        "order_id", "B",
                        "compare_order_id", "A"
                    )
                )
            )
        );

        WorkflowResult result = new Workflow(workflow).evaluate(request);

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block"
        );

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenWorkflowWithRootAndInnerFieldWhenNotAccessingFromRootMustTakeInnerField() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' request.orders.any { compare_order_id = order_id } return block
                default allow
            end
        """;

        Map<String, Object> request = Map.of(
            "order_id", "A",
            "request", Map.of(
                "orders", List.of(
                    Map.of(
                        "order_id", "B",
                        "compare_order_id", "A"
                    )
                )
            )
        );

        WorkflowResult result = new Workflow(workflow).evaluate(request);

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow"
        );

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenWorkflowWithNonExistingRootValueWhenAccessingWithRootAccessorMustFail() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' request.orders.any { compare_order_id = .request.order_id } return block
                default allow
            end
        """;

        Map<String, Object> request = Map.of(
            "request", Map.of(
                "orders", List.of(
                    Map.of(
                        "order_id", "B",
                        "compare_order_id", "A"
                    )
                )
            )
        );

        WorkflowResult result = new Workflow(workflow).evaluate(request);

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow", Set.of("order_id field cannot be found")
        );

        Assertions.assertEquals(expectedResult, result);
    }
}