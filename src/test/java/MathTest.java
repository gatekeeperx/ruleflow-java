import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class MathTest {

    @Test
    public void given_math_expression_when_multiplication_must_be_resolved_first() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y * z = 15 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "dummy", "item_a", "block"),
            ruleEngine.evaluate(Map.of("x", 15, "y", 22, "z", 0))
        );
    }

    @Test
    public void given_math_expression_with_constant_multiplication_must_be_resolved_first() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y * 0 = 15 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "dummy", "item_a", "block"),
            ruleEngine.evaluate(Map.of("x", 15, "y", 22))
        );
    }

    @Test
    public void given_math_expression_with_parenthesis_inner_expression_must_be_resolved_first() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' (x + y) * z = 80 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "dummy", "item_a", "block"),
            ruleEngine.evaluate(Map.of("x", 15, "y", 25, "z", 2))
        );
    }

    @Test
    public void given_math_expression_with_multiplication_must_be_resolved_first() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y * z = 65 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "dummy", "item_a", "block"),
            ruleEngine.evaluate(Map.of("x", 15, "y", 25, "z", 2))
        );
    }

    @Test
    public void given_complex_math_expression_must_be_resolved_correctly() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y - 3 * z = -47.54 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "dummy", "item_a", "block"),
            ruleEngine.evaluate(Map.of("x", 3.43, "y", 0, "z", 16.99))
        );
    }

    @Test
    public void given_division_expression_must_be_resolved_correctly() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x / y = 2 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "dummy", "item_a", "block"),
            ruleEngine.evaluate(Map.of("x", 4, "y", 2))
        );
    }

    @Test
    public void given_division_by_zero_must_result_in_warning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x / y = 2 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "default", "default", "allow", Set.of()),
            ruleEngine.evaluate(Map.of("x", 4, "y", 0))
        );
    }

    @Test
    public void given_nonexistent_field_in_expression_must_return_warning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x / y = 2 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "default", "default", "allow", Set.of("y field cannot be found")),
            ruleEngine.evaluate(Map.of("x", 4))
        );
    }

    @Test
    public void given_modulo_expression_must_be_resolved_correctly() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x % y = 9 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "dummy", "item_a", "block"),
            ruleEngine.evaluate(Map.of("x", 99, "y", 10))
        );
    }

    @Test
    public void given_modulo_expression_when_result_is_not_equal_must_return_default() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x % y = 9 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "default", "default", "allow"),
            ruleEngine.evaluate(Map.of("x", 99, "y", 100))
        );
    }

    @Test
    public void given_modulo_expression_with_nonexistent_right_value_must_return_warning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x % y = 9 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "default", "default", "allow", Set.of("y field cannot be found")),
            ruleEngine.evaluate(Map.of("x", 9))
        );
    }

    @Test
    public void given_modulo_expression_with_nonexistent_left_value_must_return_warning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x % y = 9 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        Assertions.assertEquals(
            new WorkflowResult("test", "default", "default", "allow", Set.of("x field cannot be found")),
            ruleEngine.evaluate(Map.of("y", 10))
        );
    }
}