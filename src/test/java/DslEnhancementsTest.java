import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DslEnhancementsTest {

    // -------------------------------------------------------------------------
    // String concatenation via +
    // -------------------------------------------------------------------------

    @Test
    void testStringConcatWithPlus() {
        String workflow = """
            workflow 'test'
                ruleset 'check'
                    'match' firstName + ' ' + lastName == 'John Doe' return approved
                default denied
            end
            """;
        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("firstName", "John", "lastName", "Doe"), Map.of());
        assertEquals("approved", result.getResult());
    }

    @Test
    void testStringConcatNoMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'check'
                    'match' firstName + ' ' + lastName == 'Jane Smith' return approved
                default denied
            end
            """;
        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("firstName", "John", "lastName", "Doe"), Map.of());
        assertEquals("denied", result.getResult());
    }

    // -------------------------------------------------------------------------
    // THEN + CONTINUE (accumulate actions)
    // -------------------------------------------------------------------------

    @Test
    void testThenContinueAccumulatesActions() {
        String workflow = """
            workflow 'test'
                ruleset 'tagging'
                    'flag' amount > 1000 then flag_for_review continue

                ruleset 'decision'
                    'block' is_blocked == true return block and notify_ops

                default allow
            end
            """;
        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("amount", 2000, "is_blocked", true), Map.of());

        assertEquals("block", result.getResult());
        // Both accumulated action and rule's own action should be present
        assertTrue(result.getActions().contains("flag_for_review"));
        assertTrue(result.getActions().contains("notify_ops"));
    }

    @Test
    void testThenContinueFallsToDefault() {
        String workflow = """
            workflow 'test'
                ruleset 'tagging'
                    'flag' amount > 100 then tag_high_value continue

                default allow
            end
            """;
        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("amount", 500), Map.of());

        assertEquals("allow", result.getResult());
        // Accumulated action should appear in default result
        assertTrue(result.getActions().contains("tag_high_value"));
    }

    // -------------------------------------------------------------------------
    // Always-true rule (no condition)
    // -------------------------------------------------------------------------

    @Test
    void testAlwaysTrueRule() {
        String workflow = """
            workflow 'test'
                ruleset 'decision'
                    'low' return low_risk
                default allow
            end
            """;
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of(), Map.of());
        assertEquals("low_risk", result.getResult());
        assertEquals("low", result.getRule());
    }

    @Test
    void testAlwaysTruleRuleWithContinue() {
        String workflow = """
            workflow 'test'
                ruleset 'setup'
                    'init' set $score = 10 continue

                ruleset 'decision'
                    'high' $score > 5 return high
                    'low' return low

                default allow
            end
            """;
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of(), Map.of());
        assertEquals("high", result.getResult());
    }

    // -------------------------------------------------------------------------
    // Variables snapshot per matched rule (multi-match)
    // -------------------------------------------------------------------------

    @Test
    void testVariablesInMatchedRules() {
        String workflow = """
            workflow 'test' evaluation_mode multi_match
                ruleset 'scoring'
                    'score' amount > 0 set $score = amount * 2 continue

                ruleset 'tags'
                    'high' $score > 100 return high
                    'any'  $score > 0   return tagged

                default allow
            end
            """;
        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("amount", 100), Map.of());

        assertEquals(2, result.getMatchedRules().size());
        // Both matched rules should have the variables snapshot
        result.getMatchedRules().forEach(r ->
            assertEquals(200.0, ((Number) r.getVariables().get("score")).doubleValue(), 0.001));
    }
}
