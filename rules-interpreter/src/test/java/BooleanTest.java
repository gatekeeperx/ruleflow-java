import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class BooleanTest {

    @Test
    public void givenBooleanExpressionWhenOperatingANDOperationMustBeResolvedFirst() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x AND y OR z return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "default", "default", "allow", new HashSet<>(), new HashMap<>(), false);
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", false, "y", true, "z", false));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenBooleanExpressionWhenOperatingANDWithConstantMustBeResolvedFirst() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x AND y OR false = false return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", true, "y", true));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenBooleanExpressionWhenUsingParenthesisThenInnerExpressionMustBeResolvedFirst() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' (x OR y) AND z return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "block", new HashSet<>());
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", true, "y", false, "z", true));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenComplexBooleanExpressionWhenOperatingMustBeResolvedOk() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x OR y OR false AND z return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "item_a", "block", new HashSet<>());
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", true, "y", false, "z", true));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenBooleanExpressionWhenOperatingWithNonBooleanValueMustResultInWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x AND false return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult evaluation = ruleEngine.evaluate(Map.of("x", true));

        Assertions.assertTrue(evaluation.getWarnings().isEmpty());
        Assertions.assertEquals("allow", evaluation.getResult());
    }

    @Test
    public void givenExpressionWithNonExistentFieldWhenOperatingMustReturnWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x AND y return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult evaluation = ruleEngine.evaluate(Map.of("x", true));

        Assertions.assertEquals(Set.of("y field cannot be found"), evaluation.getWarnings());
        Assertions.assertEquals("allow", evaluation.getResult());
    }

    @Test
    public void test() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'test' order.custom.user_is_prime = true
                    and features.is_card_bin_in_anomaly_detector <> true
                    and features.fake_users_user_email_score <= 1
                    and user.email_verification_status in 'VERIFIED'
                    and (features.mk_payer_distinct_cc_fingerprint_90d + features.mk_payer_distinct_device_id_90d) <= 8
                    and features.crosses_login_device_qty_users_7d <= 3 and features.crosses_registration_device_qty_users_7d < 1
                    and (features.user_compensations_amount_90d/features.mk_payer_approved_amount_90d) < 0.05
                    and features.mk_payer_approved_qty_7d >= 1 and features.mk_payer_approved_qty_30d >= 6
                    and (features.mk_payer_approved_qty_60d >= features.mk_payer_approved_qty_30d + 6)
                    and (features.mk_payer_approved_qty_90d >= features.mk_payer_approved_qty_60d + 6)
                    and features.mk_payer_approved_amount_30d >= 50
                    and (features.mk_payer_approved_amount_90d >= features.mk_payer_approved_amount_30d + 100) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult evaluation = ruleEngine.evaluate(
            Map.of(
                "order", Map.of("custom", Map.of("user_is_prime", true)),
                "features", Map.of(
                    "is_card_bin_in_anomaly_detector", true,
                    "mk_payer_distinct_cc_fingerprint_90d", 0,
                    "mk_payer_distinct_device_id_90d", 0,
                    "fake_users_user_email_score", 0,
                    "user_compensations_amount_90d", 0,
                    "mk_payer_approved_amount_90d", 0,
                    "mk_payer_approved_qty_90d", 0
                )
            )
        );

        Assertions.assertEquals(Set.of(), evaluation.getWarnings());
        Assertions.assertEquals("allow", evaluation.getResult());
    }
}