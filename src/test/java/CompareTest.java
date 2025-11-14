import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class CompareTest {

    @Test
    public void givenStringWhenOperatingLtOperationMustGoOk() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'x_is_lesser' x < '7.53' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "x_is_lesser", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "7.52.2.19911"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenStringWhenOperatingLteOperationMustGoOk() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'x_is_lesser' x <= '7.53' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "x_is_lesser", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "7.52.2.19911"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenStringWhenOperatingGtOperationMustGoOk() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'x_greater' x > '7.53' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "x_greater", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "7.53.2.19911"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenStringWhenOperatingGteOperationMustGoOk() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'x_greater' x >= '7.53' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "x_greater", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "7.53.2.19911"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenStringWhenOperatingNeOperationMustGoOk() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'x_greater' x <> '7.53' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "x_greater", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "7.52"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateTimeWithZoneWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'dt_match' x = '2024-06-01T12:30Z' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "dt_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01T12:30Z"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenComparisonBetweenStringAndStringUsingContainsMustFail() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'comparison' x = 'some_string' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", List.of("mystring")));

        // Should return default result since the rule fails due to type mismatch
        Assertions.assertEquals("allow", result.getResult());
        Assertions.assertEquals("default", result.getRule());
        
        // Should have a warning about type comparison error
        Assertions.assertFalse(result.getWarnings().isEmpty(), "Should have warnings about type comparison");
        Assertions.assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.equals("There is a comparison between different dataTypes in rule comparison")),
            "Should contain warning about type comparison in rule 'comparison'");
    }
}