import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MissingPropertyTest {

    private WorkflowResult evaluate(String rule, Map<String, Object> data) {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' %s return block
                default allow
            end
        """.formatted(rule);
        return new Workflow(workflow).evaluate(data);
    }

    @Test
    public void givenMissingPropertyInOrWhenOtherOperandIsTrueRuleMustMatch() {
        WorkflowResult result = evaluate("x = 1 OR y = 2", Map.of("y", 2));

        Assertions.assertEquals("block", result.getResult());
        Assertions.assertEquals(Set.of("x field cannot be found"), result.getWarnings());
    }

    @Test
    public void givenMissingPropertyInAndRuleMustNotMatch() {
        WorkflowResult result = evaluate("x = 1 AND y = 2", Map.of("y", 2));

        Assertions.assertEquals("allow", result.getResult());
        Assertions.assertEquals(Set.of("x field cannot be found"), result.getWarnings());
    }

    @Test
    public void givenMissingPropertyWithRelationalOperatorsOperationMustBeFalse() {
        for (String op : List.of(">", "<", ">=", "<=", "=", "<>")) {
            WorkflowResult result = evaluate("amount " + op + " 5", Map.of("y", 2));

            Assertions.assertEquals("allow", result.getResult(), op);
            Assertions.assertEquals(Set.of("amount field cannot be found"), result.getWarnings(), op);
        }
    }

    @Test
    public void givenMissingPropertyWithNegatedListOperatorOperationMustBeFalse() {
        WorkflowResult result = evaluate("x not in 'a', 'b'", Map.of("y", 2));

        Assertions.assertEquals("allow", result.getResult());
    }

    @Test
    public void givenMissingPropertyWithIsNullMustStillMatch() {
        Assertions.assertEquals("block", evaluate("x is null", Map.of("y", 2)).getResult());
    }

    @Test
    public void givenMerchantWithoutTagsAnyMustBeFalseWithWarning() {
        WorkflowResult result = evaluate("merchant.tags.any {'asb'}", Map.of("merchant", Map.of("name", "m")));

        Assertions.assertEquals("allow", result.getResult());
        Assertions.assertEquals(Set.of("tags field cannot be found"), result.getWarnings());
        Assertions.assertFalse(result.isError());
    }

    @Test
    public void givenMerchantWithNullTagsAnyMustBeFalseWithWarning() {
        Map<String, Object> merchant = new HashMap<>();
        merchant.put("tags", null);

        WorkflowResult result = evaluate("merchant.tags.any {'asb'}", Map.of("merchant", merchant));

        Assertions.assertEquals("allow", result.getResult());
        Assertions.assertEquals(Set.of("merchant.tags field cannot be found"), result.getWarnings());
        Assertions.assertFalse(result.isError());
    }

    @Test
    public void givenNullCollectionWhenOtherOperandIsTrueRuleMustMatch() {
        Map<String, Object> merchant = new HashMap<>();
        merchant.put("tags", null);

        WorkflowResult result = evaluate("merchant.tags.any {'asb'} OR y = 2", Map.of("merchant", merchant, "y", 2));

        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    public void givenMissingFieldInsideAnyPredicateItemMustBeFalse() {
        WorkflowResult result = evaluate("items.any { type = 'a' }",
            Map.of("items", List.of(Map.of("name", "n"), Map.of("type", "a"))));

        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    public void givenSetCollectionWhenUsingAnyMustMatchElement() {
        WorkflowResult result = evaluate("merchant.tags.any {'asb'}", Map.of("merchant", Map.of("tags", Set.of("asb", "other"))));

        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    public void givenArrayCollectionWhenUsingAnyMustMatchElement() {
        WorkflowResult result = evaluate("merchant.tags.any {'asb'}", Map.of("merchant", Map.of("tags", new String[]{"asb"})));

        Assertions.assertEquals("block", result.getResult());
    }
}
