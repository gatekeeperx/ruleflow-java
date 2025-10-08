import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class RegexTest {

    @Test
    public void givenExpressionWithValidStringFieldWhenMatchingRegexMustReturnTrue() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' regex_strip(x, '^(0|1+0+)+') = '3223863467' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "item_a", "block", Set.of()
        );
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", "01110000003223863467"
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenExpressionWithSameStringValueWhenMatchingRegexMustReturnTrue() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' regex_strip(x, '^(0|1+0+)+') = '3223863467' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "item_a", "block", Set.of()
        );
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", "3223863467"
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenExpressionWithSameStringValueWhenNotMatchingRegexMustReturnFalse() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' regex_strip(x, '^4333') = '3223863467' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow", Set.of()
        );
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", "01110000003223863467"
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenEmailWhenStrippingDomainShouldMatchUserOnly() {
        String workflow = """
        workflow 'test'
            ruleset 'dummy'
                'item_a' regex_strip(x, '@.*') = 'user' return block
            default allow
        end
    """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "item_a", "block", Set.of()
        );
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "x", "user@nextmail.com"
        ));

        Assertions.assertEquals(expectedResult, result);
    }
}