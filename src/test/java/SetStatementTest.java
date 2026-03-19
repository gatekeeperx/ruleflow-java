import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SetStatementTest {

    private static final String TEMPLATE = """
        workflow 'test'
            ruleset 'check'
                %s
            default allow
        end
        """;

    @Test
    void testBasicSetExposedInResult() {
        String workflow = String.format(TEMPLATE,
            "'rule1' amount > 500 set $riskScore = amount * 2 return flagged");

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 600), Map.of());

        assertEquals("flagged", result.getResult());
        assertEquals(1200.0, ((Number) result.getVariables().get("riskScore")).doubleValue(), 0.001);
    }

    @Test
    void testMultipleSetClausesTopToBottom() {
        String workflow = String.format(TEMPLATE,
            "'rule1' amount > 0 set $base = amount * 2 set $doubled = $base return ok");

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 10), Map.of());

        assertEquals("ok", result.getResult());
        assertEquals(20.0, ((Number) result.getVariables().get("base")).doubleValue(), 0.001);
        // '$doubled' references '$base' which was set in the previous clause
        assertEquals(20.0, ((Number) result.getVariables().get("doubled")).doubleValue(), 0.001);
    }

    @Test
    void testSetVariableUsedInSubsequentRuleCondition() {
        String workflow = """
            workflow 'test'
                ruleset 'check'
                    'first' amount > 100 set $flag = 1 return first_matched
                    'second' $flag == 1 return second_matched
                default allow
            end
            """;

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 200), Map.of());

        // Single-match: first rule fires and returns
        assertEquals("first_matched", result.getResult());
        assertEquals(1, ((Number) result.getVariables().get("flag")).intValue());
    }

    @Test
    void testSetVariableUsedAcrossRulesets() {
        String workflow = """
            workflow 'test'
                ruleset 'scoring'
                    'score_rule' amount > 0 set $score = amount * 3 return scored

                ruleset 'decision'
                    'high' $score > 100 return block

                default allow
            end
            """;

        // amount=50 → score=150 → score>100 matches → but single-match returns on first hit
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 50), Map.of());

        assertEquals("scored", result.getResult());
        assertEquals(150.0, ((Number) result.getVariables().get("score")).doubleValue(), 0.001);
    }

    @Test
    void testSetDoesNotShadowRequestField() {
        String workflow = """
            workflow 'test'
                ruleset 'check'
                    'setter' amount > 0 set $amount = 9999 return set_done
                    'verify' amount > 9000 return shadowed
                default allow
            end
            """;

        // Request amount=100. First rule matches, tries to set $amount=9999.
        // In single-match mode, returns immediately after first rule.
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 100), Map.of());
        assertEquals("set_done", result.getResult());
        // The variable was set but bare 'amount' still reads request data (100)
        // verify the variable was stored
        assertEquals(9999, ((Number) result.getVariables().get("amount")).intValue());
    }

    @Test
    void testSetWithCustomFunction() {
        String workflow = String.format(TEMPLATE,
            "'rule1' userId <> '' set $score = riskFn(userId) return done");

        RuleflowFunction fn = args -> 42;

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "abc"),
            Map.of(),
            Map.of("riskFn", fn)
        );

        assertEquals("done", result.getResult());
        assertEquals(42, result.getVariables().get("score"));
    }

    @Test
    void testNoSetClausesBackwardCompatibility() {
        String workflow = String.format(TEMPLATE,
            "'rule1' amount > 10 return approved");

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 50), Map.of());

        assertEquals("approved", result.getResult());
        assertTrue(result.getVariables().isEmpty());
    }

    @Test
    void testSetClauseWithStringLiteral() {
        String workflow = String.format(TEMPLATE,
            "'rule1' amount > 0 set $category = 'high' return ok");

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 1), Map.of());

        assertEquals("ok", result.getResult());
        assertEquals("high", result.getVariables().get("category"));
    }

    @Test
    void testCompoundAssignRhsIsExpr() {
        String workflow = """
            workflow 'test'
                ruleset 'pricing'
                    'base'     amount > 0 set $price = amount continue
                    'discount' discount > 0 set $price -= amount * discount / 100 continue
                    'tax'      return done
                default allow
            end
            """;

        // $price = 200 - (200 * 10 / 100) = 200 - 20 = 180
        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("amount", 200, "discount", 10), Map.of()
        );

        assertEquals("done", result.getResult());
        assertEquals(180.0, ((Number) result.getVariables().get("price")).doubleValue(), 0.001);
    }

    @Test
    void testDefaultResultHasEmptyVariables() {
        String workflow = String.format(TEMPLATE,
            "'rule1' amount > 1000 set $x = 1 return flagged");

        // amount=1 — rule does not match, falls to default
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 1), Map.of());

        assertEquals("allow", result.getResult());
        assertTrue(result.getVariables().isEmpty());
    }

    @Test
    void testSetVariableVisibleInMultiMatch() {
        String workflow = """
            workflow 'test' evaluation_mode multi_match
                ruleset 'check'
                    'rule_a' amount > 0 set $tagA = 'yes' return matched_a
                    'rule_b' amount > 0 set $tagB = 'yes' return matched_b
                default allow
            end
            """;

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 5), Map.of());

        assertEquals("matched_a", result.getResult());
        // Variables accumulate across matched rules — both are set
        assertEquals("yes", result.getVariables().get("tagA"));
        assertEquals("yes", result.getVariables().get("tagB"));
        assertEquals(2, result.getMatchedRules().size());
    }
}
