package examples;

import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;

public class AmlOnboardingWorkflowTest {

    // -------------------------------------------------------------------------
    // End-to-end: agrario + sanctions hit_2 → low risk
    // -------------------------------------------------------------------------

    @Test
    void testAgrarioWithSanctionsHit2ReturnsLow() {
        String workflow = """
            workflow 'test_aml_onboarding'
                ruleset 'work_info'
                    'agrario'     customer.workInfo.code = 1010 SET $score = 0.45 * 10 continue
                    'informatico' customer.workInfo.code = 230  SET $score += 0.45 * 5 continue
                    'desempleado' customer.workInfo.code = 2300  SET $score = $score + 0.45 * 5 continue
                ruleset 'sanctions'
                    'hit'   screening(documentNumber, customer.firstName + ' ' + customer.lastName).matchCount > 5
                            set $score = 0.55 * 15
                            action('block') continue
                    'hit_list' screening(documentNumber, customer.firstName + ' ' + customer.lastName).matches.contains { it.matchPriority > 3 }
                            set $score = 0.55 * 15
                            action('block') continue
                    'hit_2' screening(documentNumber, customer.firstName + ' ' + customer.lastName).matchCount > 0
                            set $score = 0.45 * 15
                            action('block') continue
                ruleset 'risk_rating'
                    'low'    $score > 3  AND $score < 15 return low
                    'medium' $score >= 15 AND $score < 30 return medium
                    'high'   $score >= 30 return high
                default low
            end
            """;

        Map<String, Object> request = Map.of(
            "documentNumber", "DOC123",
            "customer", Map.of(
                "firstName", "Juan",
                "lastName", "Garcia",
                "workInfo", Map.of("code", 1010)
            )
        );

        // screening returns matchCount=1, matches=[{matchPriority: null}]
        RuleflowFunction screeningFn = args -> {
            Map<String, Object> matchItem = new HashMap<>();
            matchItem.put("matchPriority", null);
            Map<String, Object> screeningResult = new HashMap<>();
            screeningResult.put("matchCount", 1);
            screeningResult.put("matches", List.of(matchItem));
            return screeningResult;
        };

        WorkflowResult result = new Workflow(workflow).evaluate(
            request, Map.of(), Map.of("screening", screeningFn)
        );

        assertEquals("low", result.getResult());
        // score: agrario sets 0.45*10=4.5, hit_2 sets 0.45*15=6.75
        assertEquals(6.75, ((Number) result.getVariables().get("score")).doubleValue(), 0.001);
        assertTrue(result.getActions().contains("block"));
    }

    // -------------------------------------------------------------------------
    // += compound assignment
    // -------------------------------------------------------------------------

    @Test
    void testCompoundPlusAssignment() {
        String workflow = """
            workflow 'test'
                ruleset 'setup'
                    'init' set $x = 10 continue
                    'add'  set $x += 5 continue
                ruleset 'check'
                    'result' return expr($x)
                default zero
            end
            """;

        WorkflowResult result = new Workflow(workflow).evaluate(Map.of(), Map.of());
        assertEquals(15.0, ((Number) result.getVariables().get("x")).doubleValue(), 0.001);
    }

    // -------------------------------------------------------------------------
    // .contains { it.field > value } with null-safe items
    // -------------------------------------------------------------------------

    @Test
    void testContainsWithItPredicateNullSafe() {
        String workflow = """
            workflow 'test'
                ruleset 'check'
                    'hit' items.contains { it.priority > 3 } return matched
                default no_match
            end
            """;

        // One item with null priority (safe), one with priority=5 (matches)
        Map<String, Object> nullItem = new HashMap<>();
        nullItem.put("priority", null);
        Map<String, Object> request = Map.of(
            "items", List.of(nullItem, Map.of("priority", 5))
        );

        WorkflowResult result = new Workflow(workflow).evaluate(request, Map.of());
        assertEquals("matched", result.getResult());
    }

    @Test
    void testContainsWithItPredicateAllNull() {
        String workflow = """
            workflow 'test'
                ruleset 'check'
                    'hit' items.contains { it.priority > 3 } return matched
                default no_match
            end
            """;

        Map<String, Object> nullItem1 = new HashMap<>();
        nullItem1.put("priority", null);
        Map<String, Object> nullItem2 = new HashMap<>();
        nullItem2.put("priority", null);
        Map<String, Object> request = Map.of("items", List.of(nullItem1, nullItem2));

        WorkflowResult result = new Workflow(workflow).evaluate(request, Map.of());
        assertEquals("no_match", result.getResult());
    }

    // -------------------------------------------------------------------------
    // Inline action + continue (no THEN keyword)
    // -------------------------------------------------------------------------

    @Test
    void testInlineActionContinueAccumulates() {
        String workflow = """
            workflow 'test'
                ruleset 'flag'
                    'trigger' amount > 100
                              action('flag_review') continue
                ruleset 'decision'
                    'approve' return approved
                default denied
            end
            """;

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("amount", 500), Map.of()
        );

        assertEquals("approved", result.getResult());
        assertTrue(result.getActions().contains("flag_review"));
    }

    @Test
    void testInlineActionContinueWithSetClause() {
        String workflow = """
            workflow 'test'
                ruleset 'score'
                    'add' amount > 0
                          set $score = amount * 2
                          action('scored') continue
                ruleset 'result'
                    'high' $score > 50 return high
                    'low'  return low
                default no_result
            end
            """;

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("amount", 100), Map.of()
        );

        assertEquals("high", result.getResult());
        assertEquals(200.0, ((Number) result.getVariables().get("score")).doubleValue(), 0.001);
        assertTrue(result.getActions().contains("scored"));
    }
}
