import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class NullTest {

    @Test
    public void givenExpressionAndNullValueWhenOperatingShouldSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x + y * z  = 15 return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow",
           Set.of("x field cannot be found")
        );

        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "y", 22,
            "z", 0
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenExpressionWithNullValueWhenOperatingShouldNotSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'item_a' x = null AND x + y * z  = 15 return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
                "test", "default", "default", "allow",
                Set.of("x field cannot be found")
        );

        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "y", 22,
            "z", 0
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenNullValueWhenInStoredListShouldSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'device' device.fingerprint in list('fingerprint_blacklist') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(
            Map.of("device", Map.of("other", "value")),
            Map.of("fingerprint_blacklist", List.of("fp-123", "fp-456"))
        );

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow",
            Set.of("fingerprint field cannot be found")
        );
        Assertions.assertEquals(expectedResult, result);
        Assertions.assertFalse(result.isError());
    }

    @Test
    public void givenNullValueWhenInLiteralListShouldSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'device' device.fingerprint in 'fp-123', 'fp-456' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(
            Map.of("device", Map.of("other", "value"))
        );

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow",
            Set.of("fingerprint field cannot be found")
        );
        Assertions.assertEquals(expectedResult, result);
        Assertions.assertFalse(result.isError());
    }

    @Test
    public void givenNullValueWhenContainsStoredListShouldSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'email_match' user.email contains list('domain_blacklist') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(
            Map.of("user", Map.of("name", "John")),
            Map.of("domain_blacklist", List.of("spam.com", "fraud.com"))
        );

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow",
            Set.of("email field cannot be found")
        );
        Assertions.assertEquals(expectedResult, result);
        Assertions.assertFalse(result.isError());
    }

    @Test
    public void givenNullValueWhenStartsWithStoredListShouldSetWarning() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'prefix_match' user.phone starts_with list('prefix_blacklist') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(
            Map.of("user", Map.of("name", "John")),
            Map.of("prefix_blacklist", List.of("+1555", "+44"))
        );

        WorkflowResult expectedResult = new WorkflowResult(
            "test", "default", "default", "allow",
            Set.of("phone field cannot be found")
        );
        Assertions.assertEquals(expectedResult, result);
        Assertions.assertFalse(result.isError());
    }

    @Test
    public void testNullValueInMapWithMapElements() {
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device' device.fingerprint in list('fingerprint_blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        Map<String, Object> deviceMap = new HashMap<>();
        deviceMap.put("fingerprint", null);

        WorkflowResult result = ruleEngine.evaluate(
                Map.of("device", deviceMap),
                Map.of("fingerprint_blacklist", List.of("fp-123", "fp-456"))
        );

        WorkflowResult expectedResult = new WorkflowResult(
                "test", "default", "default", "allow",
                Set.of("device.fingerprint field cannot be found")
        );
        Assertions.assertEquals(expectedResult, result);
        Assertions.assertFalse(result.isError());
    }

    @Test
    public void testNullValueInMapWithMapElementsNull() {
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device' device.fingerprint in list('fingerprint_blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        // Map.of() does not allow null values (throws NPE).
        // HashMap supports nulls, which is what Jackson/Gson produce from JSON.
        Map<String, Object> deviceMap = new HashMap<>();
        deviceMap.put("fingerprint", null);

        WorkflowResult result = ruleEngine.evaluate(
                Map.of("device", deviceMap),
                Map.of("fingerprint_blacklist", List.of("fp-123", "fp-456"))
        );

        WorkflowResult expectedResult = new WorkflowResult(
                "test", "default", "default", "allow",
                Set.of("device.fingerprint field cannot be found")
        );
        Assertions.assertEquals(expectedResult, result);
        Assertions.assertFalse(result.isError());
    }
}