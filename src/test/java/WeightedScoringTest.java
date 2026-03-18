import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WeightedScoringTest {

    @Test
    void testLargeAmountAndHighRiskCountryIsFlag() {
        // (10*0.5) + (10*0.5) = 10 > 8 → flag
        String workflow = """
            workflow 'aml_screening'
                ruleset 'score_transaction'
                    'large'  transaction_amount > 50000                                        set $txn_score = 10 continue
                    'medium' transaction_amount > 10000 AND transaction_amount <= 50000        set $txn_score = 6  continue
                    'small'  transaction_amount > 0 AND transaction_amount <= 10000            set $txn_score = 2  continue

                ruleset 'score_country'
                    'high'   country_risk == 'high'   set $country_score = 10 continue
                    'medium' country_risk == 'medium' set $country_score = 5  continue
                    'low'    country_risk == 'low'    set $country_score = 2  continue

                ruleset 'aml_decision'
                    'flag'   ($txn_score * 0.5) + ($country_score * 0.5) > 8.0 return flag
                    'review' ($txn_score * 0.5) + ($country_score * 0.5) > 5.0 return review

                default clear
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(
            Map.of("transaction_amount", 75000, "country_risk", "high"), Map.of());
        assertEquals("flag", r.getResult());
        assertEquals(10.0, num(r, "txn_score"), 0.001);
        assertEquals(10.0, num(r, "country_score"), 0.001);
    }

    @Test
    void testLargeAmountAndMediumRiskCountryIsReview() {
        // (10*0.5) + (5*0.5) = 7.5, not > 8 → review
        String workflow = """
            workflow 'aml_screening'
                ruleset 'score_transaction'
                    'large'  transaction_amount > 50000                                        set $txn_score = 10 continue
                    'medium' transaction_amount > 10000 AND transaction_amount <= 50000        set $txn_score = 6  continue
                    'small'  transaction_amount > 0 AND transaction_amount <= 10000            set $txn_score = 2  continue

                ruleset 'score_country'
                    'high'   country_risk == 'high'   set $country_score = 10 continue
                    'medium' country_risk == 'medium' set $country_score = 5  continue
                    'low'    country_risk == 'low'    set $country_score = 2  continue

                ruleset 'aml_decision'
                    'flag'   ($txn_score * 0.5) + ($country_score * 0.5) > 8.0 return flag
                    'review' ($txn_score * 0.5) + ($country_score * 0.5) > 5.0 return review

                default clear
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(
            Map.of("transaction_amount", 75000, "country_risk", "medium"), Map.of());
        assertEquals("review", r.getResult());
        assertEquals(10.0, num(r, "txn_score"), 0.001);
        assertEquals(5.0, num(r, "country_score"), 0.001);
    }

    @Test
    void testMediumAmountAndHighRiskCountryIsReview() {
        // (6*0.5) + (10*0.5) = 8.0, not > 8 → review
        String workflow = """
            workflow 'aml_screening'
                ruleset 'score_transaction'
                    'large'  transaction_amount > 50000                                        set $txn_score = 10 continue
                    'medium' transaction_amount > 10000 AND transaction_amount <= 50000        set $txn_score = 6  continue
                    'small'  transaction_amount > 0 AND transaction_amount <= 10000            set $txn_score = 2  continue

                ruleset 'score_country'
                    'high'   country_risk == 'high'   set $country_score = 10 continue
                    'medium' country_risk == 'medium' set $country_score = 5  continue
                    'low'    country_risk == 'low'    set $country_score = 2  continue

                ruleset 'aml_decision'
                    'flag'   ($txn_score * 0.5) + ($country_score * 0.5) > 8.0 return flag
                    'review' ($txn_score * 0.5) + ($country_score * 0.5) > 5.0 return review

                default clear
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(
            Map.of("transaction_amount", 20000, "country_risk", "high"), Map.of());
        assertEquals("review", r.getResult());
        assertEquals(6.0, num(r, "txn_score"), 0.001);
        assertEquals(10.0, num(r, "country_score"), 0.001);
    }

    @Test
    void testMediumAmountAndMediumRiskCountryIsReview() {
        // (6*0.5) + (5*0.5) = 5.5 > 5.0 → review
        String workflow = """
            workflow 'aml_screening'
                ruleset 'score_transaction'
                    'large'  transaction_amount > 50000                                        set $txn_score = 10 continue
                    'medium' transaction_amount > 10000 AND transaction_amount <= 50000        set $txn_score = 6  continue
                    'small'  transaction_amount > 0 AND transaction_amount <= 10000            set $txn_score = 2  continue

                ruleset 'score_country'
                    'high'   country_risk == 'high'   set $country_score = 10 continue
                    'medium' country_risk == 'medium' set $country_score = 5  continue
                    'low'    country_risk == 'low'    set $country_score = 2  continue

                ruleset 'aml_decision'
                    'flag'   ($txn_score * 0.5) + ($country_score * 0.5) > 8.0 return flag
                    'review' ($txn_score * 0.5) + ($country_score * 0.5) > 5.0 return review

                default clear
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(
            Map.of("transaction_amount", 20000, "country_risk", "medium"), Map.of());
        assertEquals("review", r.getResult());
        assertEquals(6.0, num(r, "txn_score"), 0.001);
        assertEquals(5.0, num(r, "country_score"), 0.001);
    }

    @Test
    void testSmallAmountAndLowRiskCountryIsClear() {
        // (2*0.5) + (2*0.5) = 2.0, not > 5.0 → clear (default)
        String workflow = """
            workflow 'aml_screening'
                ruleset 'score_transaction'
                    'large'  transaction_amount > 50000                                        set $txn_score = 10 continue
                    'medium' transaction_amount > 10000 AND transaction_amount <= 50000        set $txn_score = 6  continue
                    'small'  transaction_amount > 0 AND transaction_amount <= 10000            set $txn_score = 2  continue

                ruleset 'score_country'
                    'high'   country_risk == 'high'   set $country_score = 10 continue
                    'medium' country_risk == 'medium' set $country_score = 5  continue
                    'low'    country_risk == 'low'    set $country_score = 2  continue

                ruleset 'aml_decision'
                    'flag'   ($txn_score * 0.5) + ($country_score * 0.5) > 8.0 return flag
                    'review' ($txn_score * 0.5) + ($country_score * 0.5) > 5.0 return review

                default clear
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(
            Map.of("transaction_amount", 5000, "country_risk", "low"), Map.of());
        assertEquals("clear", r.getResult());
        assertEquals(2.0, num(r, "txn_score"), 0.001);
        assertEquals(2.0, num(r, "country_score"), 0.001);
    }

    @Test
    void testVariablesAreExposedOnResult() {
        String workflow = """
            workflow 'aml_screening'
                ruleset 'score_transaction'
                    'large'  transaction_amount > 50000                                        set $txn_score = 10 continue
                    'medium' transaction_amount > 10000 AND transaction_amount <= 50000        set $txn_score = 6  continue
                    'small'  transaction_amount > 0 AND transaction_amount <= 10000            set $txn_score = 2  continue

                ruleset 'score_country'
                    'high'   country_risk == 'high'   set $country_score = 10 continue
                    'medium' country_risk == 'medium' set $country_score = 5  continue
                    'low'    country_risk == 'low'    set $country_score = 2  continue

                ruleset 'aml_decision'
                    'flag'   ($txn_score * 0.5) + ($country_score * 0.5) > 8.0 return flag
                    'review' ($txn_score * 0.5) + ($country_score * 0.5) > 5.0 return review

                default clear
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(
            Map.of("transaction_amount", 20000, "country_risk", "medium"), Map.of());
        assertTrue(r.getVariables().containsKey("txn_score"));
        assertTrue(r.getVariables().containsKey("country_score"));
    }

    @Test
    void testDecisionRulesetIsReportedCorrectly() {
        String workflow = """
            workflow 'aml_screening'
                ruleset 'score_transaction'
                    'large'  transaction_amount > 50000                                        set $txn_score = 10 continue
                    'medium' transaction_amount > 10000 AND transaction_amount <= 50000        set $txn_score = 6  continue
                    'small'  transaction_amount > 0 AND transaction_amount <= 10000            set $txn_score = 2  continue

                ruleset 'score_country'
                    'high'   country_risk == 'high'   set $country_score = 10 continue
                    'medium' country_risk == 'medium' set $country_score = 5  continue
                    'low'    country_risk == 'low'    set $country_score = 2  continue

                ruleset 'aml_decision'
                    'flag'   ($txn_score * 0.5) + ($country_score * 0.5) > 8.0 return flag
                    'review' ($txn_score * 0.5) + ($country_score * 0.5) > 5.0 return review

                default clear
            end
            """;

        WorkflowResult r = new Workflow(workflow).evaluate(
            Map.of("transaction_amount", 75000, "country_risk", "high"), Map.of());
        assertEquals("aml_decision", r.getRuleSet());
    }

    private double num(WorkflowResult r, String key) {
        return ((Number) r.getVariables().get(key)).doubleValue();
    }
}
