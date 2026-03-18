import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ContinueStatementTest {

    @Test
    void testContinueSetsVariableAndEvaluationProceedsToNextRuleset() {
        String workflow = """
            workflow 'test'
                ruleset 'scoring'
                    'score' amount > 0 set $score = amount * 2 continue

                ruleset 'decision'
                    'high' $score > 100 return block

                default allow
            end
            """;

        // score = 200*2 = 400 > 100 → block
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 200), Map.of());
        assertEquals("block", result.getResult());
        assertEquals(400.0, ((Number) result.getVariables().get("score")).doubleValue(), 0.001);
    }

    @Test
    void testContinueDoesNotReturnItsOwnResult() {
        String workflow = """
            workflow 'test'
                ruleset 'scoring'
                    'score' amount > 0 set $score = amount * 2 continue

                ruleset 'decision'
                    'low' $score <= 10 return low

                default allow
            end
            """;

        // score = 3*2 = 6 <= 10 → low
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 3), Map.of());
        assertEquals("low", result.getResult());
        assertEquals("decision", result.getRuleSet());
    }

    @Test
    void testContinueFallsToDefaultWhenNoSubsequentRuleMatches() {
        String workflow = """
            workflow 'test'
                ruleset 'scoring'
                    'score' amount > 0 set $score = amount * 2 continue

                ruleset 'decision'
                    'high' $score > 1000 return block

                default allow
            end
            """;

        // score = 10*2 = 20, not > 1000 → default allow
        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 10), Map.of());
        assertEquals("allow", result.getResult());
        assertEquals(20.0, ((Number) result.getVariables().get("score")).doubleValue(), 0.001);
    }

    @Test
    void testMultipleScoringRulesetsWithContinue() {
        String workflow = """
            workflow 'onboarding'
                ruleset 'score_occupation'
                    'farmer'  occupation == 'farmer'  set $occ = 10 continue
                    'student' occupation == 'student' set $occ = 3  continue

                ruleset 'score_country'
                    'high_risk' country == 'XX'              set $nat = 10 continue
                    'local'     country == 'CO'              set $nat = 5  continue
                    'other'     country <> 'XX' AND country <> 'CO' set $nat = 7  continue

                ruleset 'risk_rating'
                    'high'   ($occ * 0.5) + ($nat * 0.5) > 7 return high_risk
                    'medium' ($occ * 0.5) + ($nat * 0.5) > 4 return medium_risk

                default low_risk
            end
            """;

        // farmer(10) + high_risk_country(10) → (10*0.5)+(10*0.5) = 10 > 7 → high_risk
        WorkflowResult r1 = new Workflow(workflow).evaluate(
            Map.of("occupation", "farmer", "country", "XX"), Map.of());
        assertEquals("high_risk", r1.getResult());

        // student(3) + local(5) → (3*0.5)+(5*0.5) = 4, not > 4 → low_risk
        WorkflowResult r2 = new Workflow(workflow).evaluate(
            Map.of("occupation", "student", "country", "CO"), Map.of());
        assertEquals("low_risk", r2.getResult());

        // farmer(10) + local(5) → (10*0.5)+(5*0.5) = 7.5 > 7 → high_risk
        WorkflowResult r3 = new Workflow(workflow).evaluate(
            Map.of("occupation", "farmer", "country", "CO"), Map.of());
        assertEquals("high_risk", r3.getResult());
    }

    @Test
    void testHardBlockFiresBeforeContinueRules() {
        String workflow = """
            workflow 'test'
                ruleset 'hard_blocks'
                    'blocked' is_blocked == true return block

                ruleset 'scoring'
                    'score' amount > 0 set $score = amount continue

                ruleset 'decision'
                    'high' $score > 100 return high_risk

                default allow
            end
            """;

        // blocked=true → block immediately, scoring never runs
        WorkflowResult blocked = new Workflow(workflow).evaluate(
            Map.of("is_blocked", true, "amount", 500), Map.of());
        assertEquals("block", blocked.getResult());
        assertFalse(blocked.getVariables().containsKey("score"));

        // blocked=false, amount=500 → scoring runs, $score=500 > 100 → high_risk
        WorkflowResult scored = new Workflow(workflow).evaluate(
            Map.of("is_blocked", false, "amount", 500), Map.of());
        assertEquals("high_risk", scored.getResult());
        assertEquals(500.0, ((Number) scored.getVariables().get("score")).doubleValue(), 0.001);
    }

    @Test
    void testContinueWithNoSetClauses() {
        String workflow = """
            workflow 'test'
                ruleset 'gate'
                    'pass' amount > 0 continue

                ruleset 'decision'
                    'ok' amount > 5 return approved

                default denied
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(Map.of("amount", 10), Map.of());
        assertEquals("approved", r.getResult());
    }

    @Test
    void testContinueInMultiMatchDoesNotAddToMatchedRules() {
        String workflow = """
            workflow 'test' evaluation_mode multi_match
                ruleset 'scoring'
                    'score' amount > 0 set $score = amount continue

                ruleset 'tags'
                    'high' amount > 100 return high
                    'any'  amount > 0   return tagged

                default allow
            end
            """;

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of("amount", 200), Map.of());
        assertEquals("high", result.getResult());
        // continue rule must NOT appear in matchedRules
        assertEquals(2, result.getMatchedRules().size());
        assertTrue(result.getMatchedRules().stream().noneMatch(r -> r.getRule().equals("score")));
        assertEquals(200.0, ((Number) result.getVariables().get("score")).doubleValue(), 0.001);
    }
}
