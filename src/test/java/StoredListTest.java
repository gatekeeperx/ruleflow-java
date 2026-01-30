import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StoredListTest {


    @Test
    public void testSingleValueStoredLists() {
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device' device.fingerprint in list('fingerprint_blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "device", Map.of("fingerprint", "fp-123"),
                "order", Map.of("merchant", Map.of("merchantId", "merchant-001"))
        ), Map.of(
                "fingerprint_blacklist", List.of(
                        "fp-123",
                        "fp-456"
                )
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "device", "block");
        Assertions.assertEquals(expectedResult, result);
    }

    // ========== Tests for Map-based stored lists ==========

    @Test
    public void testInListWithMapElements() {
        // Test: value IN list('name') where list contains Map elements
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device' device.fingerprint in list('fingerprint_blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        // List contains Maps with fingerprint field - should match if any value equals
        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "device", Map.of("fingerprint", "fp-123")
        ), Map.of(
                "fingerprint_blacklist", List.of(
                        Map.of("fingerprint", "fp-123", "reason", "stolen"),
                        Map.of("fingerprint", "fp-456", "reason", "fraud")
                )
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "device", "block");
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testInListWithMapElements_NoMatch() {
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device' device.fingerprint in list('fingerprint_blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        // Value not in any Map's values
        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "device", Map.of("fingerprint", "fp-999")
        ), Map.of(
                "fingerprint_blacklist", List.of(
                        Map.of("fingerprint", "fp-123", "reason", "stolen"),
                        Map.of("fingerprint", "fp-456", "reason", "fraud")
                )
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "default", "default", "allow");
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testContainsListWithMapElements() {
        // Test: value CONTAINS list('name') where list contains Map elements
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'email_match' user.email contains list('domain_blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "user", Map.of("email", "user@spam.com")
        ), Map.of(
                "domain_blacklist", List.of(
                        Map.of("domain", "spam.com", "type", "spam"),
                        Map.of("domain", "fraud.com", "type", "fraud")
                )
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "email_match", "block");
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testStartsWithListWithMapElements() {
        // Test: value STARTS_WITH list('name') where list contains Map elements
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'prefix_match' user.phone starts_with list('prefix_blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "user", Map.of("phone", "+1555123456")
        ), Map.of(
                "prefix_blacklist", List.of(
                        Map.of("prefix", "+1555", "country", "US"),
                        Map.of("prefix", "+44", "country", "UK")
                )
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "prefix_match", "block");
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testTupleInListWithMapElements() {
        // Test: (val1, val2) IN list('name') where list contains Map elements with ordered fields
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device_and_merchant' (device.fingerprint, order.merchantId) in list('blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        // Create LinkedHashMap to preserve field order (fingerprint, merchantId)
        Map<String, String> entry1 = new LinkedHashMap<>();
        entry1.put("fingerprint", "fp-123");
        entry1.put("merchantId", "merchant-001");

        Map<String, String> entry2 = new LinkedHashMap<>();
        entry2.put("fingerprint", "fp-456");
        entry2.put("merchantId", "merchant-002");

        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "device", Map.of("fingerprint", "fp-123"),
                "order", Map.of("merchantId", "merchant-001")
        ), Map.of(
                "blacklist", List.of(entry1, entry2)
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "device_and_merchant", "block");
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testTupleInListWithMapElements_NoMatch() {
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device_and_merchant' (device.fingerprint, order.merchantId) in list('blacklist') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        // Create LinkedHashMap with different values
        Map<String, String> entry1 = new LinkedHashMap<>();
        entry1.put("fingerprint", "fp-123");
        entry1.put("merchantId", "merchant-002");  // Different merchantId

        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "device", Map.of("fingerprint", "fp-123"),
                "order", Map.of("merchantId", "merchant-001")
        ), Map.of(
                "blacklist", List.of(entry1)
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "default", "default", "allow");
        Assertions.assertEquals(expectedResult, result);
    }

}
