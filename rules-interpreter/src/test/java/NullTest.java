import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class NullTest {

    @Test
    public void givenExpressionAndNullValueWhenOperatingShouldSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y * z  = 15 return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow",
           Set.of("x field cannot be found")
        );

        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "y", 22,
            "z", 0
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenExpressionWithNullValueWhenOperatingShouldNotSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x = null AND x + y * z  = 15 return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
                "test", "default", "default", "allow",
                Set.of("x field cannot be found")
        );

        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "y", 22,
            "z", 0
        ));

        Assertions.assertEquals(expectedResult, result);
    }
}