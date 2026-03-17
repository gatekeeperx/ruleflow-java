import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class CustomFunctionTest {

    private static final String SIMPLE_WORKFLOW_TEMPLATE = """
        workflow 'test'
            ruleset 'check'
                %s
            default allow
        end
        """;

    // -------------------------------------------------------------------------
    // Primitive return value
    // -------------------------------------------------------------------------

    @Test
    public void testPrimitiveStringReturn() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'match' screening(userId) == 'pass' then setVar('riskScore', 0.445 * 5)");

        RuleflowFunction fn = args -> "pass";

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "abc123"),
            Map.of(),
            Map.of("screening", fn)
        );

        Assertions.assertEquals("approved", result.getResult());
    }

    @Test
    public void testMultipleArgs() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'high score' score(age, income) >= 700 return approved");

        RuleflowFunction fn = args -> {
            int age = ((Number) args.get(0)).intValue();
            int income = ((Number) args.get(1)).intValue();
            return age + income;
        };

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("age", 300, "income", 500),
            Map.of(),
            Map.of("score", fn)
        );

        Assertions.assertEquals("approved", result.getResult());
    }

    @Test
    public void testNoArgs() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'active' getStatus() == 'active' return ok");

        RuleflowFunction fn = args -> "active";

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of(),
            Map.of(),
            Map.of("getStatus", fn)
        );

        Assertions.assertEquals("ok", result.getResult());
    }

    @Test
    public void testBooleanReturn() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'blocked' isBlocked(userId) return block");

        RuleflowFunction fn = args -> true;

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "u1"),
            Map.of(),
            Map.of("isBlocked", fn)
        );

        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    public void testArithmeticOnReturn() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'high' multiplier(x) * 10 > 50 return reject");

        RuleflowFunction fn = args -> 6;

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("x", 2),
            Map.of(),
            Map.of("multiplier", fn)
        );

        Assertions.assertEquals("reject", result.getResult());
    }

    // -------------------------------------------------------------------------
    // Structured return: #memberAccess
    // -------------------------------------------------------------------------

    @Test
    public void testStructuredReturnFieldAccess() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'high risk' screening(userId).matchCount > 9 return block");

        RuleflowFunction fn = args -> Map.of("risk_score", 750, "label", "high");

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "abc"),
            Map.of(),
            Map.of("screening", fn)
        );

        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    public void testStructuredReturnFieldAccessNoMatch() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'high risk' screening(userId).risk_score > 500 return block");

        RuleflowFunction fn = args -> Map.of("risk_score", 300);

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "abc"),
            Map.of(),
            Map.of("screening", fn)
        );

        Assertions.assertEquals("allow", result.getResult());
    }

    @Test
    public void testNestedFieldAccess() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'critical' screening(userId).details.level == 'critical' return prevent");

        RuleflowFunction fn = args -> Map.of(
            "details", Map.of("level", "critical", "reason", "velocity")
        );

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "abc"),
            Map.of(),
            Map.of("screening", fn)
        );

        Assertions.assertEquals("prevent", result.getResult());
    }

    @Test
    public void testStructuredReturnListFieldContains() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'fraud tag' screening(userId).matches contains 'fraud' return prevent");

        RuleflowFunction fn = args -> Map.of("tags", List.of("fraud", "high_risk"));

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "abc"),
            Map.of(),
            Map.of("screening", fn)
        );

        Assertions.assertEquals("prevent", result.getResult());
    }

    @Test
    public void testStructuredReturnListFieldAggregation() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'restricted items' screening(userId).items.any { type = 'restricted' } return review");

        RuleflowFunction fn = args -> Map.of(
            "items", List.of(
                Map.of("type", "normal"),
                Map.of("type", "restricted")
            )
        );

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("userId", "abc"),
            Map.of(),
            Map.of("screening", fn)
        );

        Assertions.assertEquals("review", result.getResult());
    }

    // -------------------------------------------------------------------------
    // Multiple functions
    // -------------------------------------------------------------------------

    @Test
    public void testMultipleFunctions() {
        String workflow = """
            workflow 'multi'
                ruleset 'check'
                    'first rule' fnA(x) == 'yes' return matched_a
                    'second rule' fnB(y) > 10 return matched_b
                default allow
            end
            """;

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("x", "input", "y", 5),
            Map.of(),
            Map.of(
                "fnA", (RuleflowFunction) args -> "yes",
                "fnB", (RuleflowFunction) args -> 20
            )
        );

        Assertions.assertEquals("matched_a", result.getResult());
    }

    // -------------------------------------------------------------------------
    // Undefined function → warning, default returned
    // -------------------------------------------------------------------------

    @Test
    public void testUndefinedFunctionReturnsDefault() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'match' undefinedFn(x) == 'ok' return approved");

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("x", "val"),
            Map.of(),
            Map.of()
        );

        Assertions.assertEquals("allow", result.getResult());
        Assertions.assertFalse(result.getWarnings().isEmpty());
    }

    // -------------------------------------------------------------------------
    // evaluate(request, lists, functions) overload
    // -------------------------------------------------------------------------

    @Test
    public void testEvaluateWithListsAndFunctions() {
        String workflow = String.format(SIMPLE_WORKFLOW_TEMPLATE,
            "'match' myFn(val) == 'ok' return yes");

        RuleflowFunction fn = args -> "ok";

        WorkflowResult result = new Workflow(workflow).evaluate(
            Map.of("val", "input"),
            Map.of("someList", List.of("a", "b")),
            Map.of("myFn", fn)
        );

        Assertions.assertEquals("yes", result.getResult());
    }
}
