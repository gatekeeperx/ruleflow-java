import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class RulesetConditionTest {
    @Test
    void rulesetConditionTrue_shouldEvaluateRules() {
        String workflow = """
            workflow 'test'
                ruleset 'main' 1 = 1 then
                    'rule1' (1 = 1 return yes)
                default return no
            end
        """;
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of());
        assertEquals("yes", result.getResult());
    }

    @Test
    void rulesetConditionFalse_shouldSkipRules() {
        String workflow = """
            workflow 'test'
                ruleset 'main' 1 = 2 then
                    'rule1' (1 = 1 return yes)
                default return no
            end
        """;
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of());
        assertEquals("no", result.getResult());
    }

    @Test
    void rulesetNoCondition_shouldEvaluateRules() {
        String workflow = """
            workflow 'test'
                ruleset 'main'
                    'rule1' (1 = 1 return yes)
                default return no
            end
        """;
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of());
        assertEquals("yes", result.getResult());
    }

    @Test
    void rulesetConditionWithMissingData_shouldSkipFirstRulesetAndEvaluateSecond() {
        String workflow = """
            workflow 'test'
                ruleset 'first' user.age > 18 then
                    'rule1' (1 = 1 return first_ruleset)
                ruleset 'second' user.name = 'John' then
                    'rule2' (1 = 1 return second_ruleset)
                default return no
            end
        """;

        Map<String, Object> payload = Map.of("user", Map.of("name", "John"));
        
        WorkflowResult result = new Workflow(workflow).evaluate(payload);
        assertEquals("second_ruleset", result.getResult());
        assertEquals("second", result.getRuleSet());

        assertTrue(result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("age field cannot be found")), 
            "Should contain warning about missing age field");
    }
} 