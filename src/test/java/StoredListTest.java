import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    @Test
    public void test() {
        String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'customer' customer.customerId in list('customer_blocked') return block
               default allow
               end
            """;
        Workflow ruleEngine = new Workflow(workflow);

        WorkflowResult result = ruleEngine.evaluate(Map.of(
                "customer", Map.of("customerId", "123456"),
                "order", Map.of("merchant", Map.of("merchantId", "merchant-001"))
        ), Map.of(
                "customer_blocked", List.of(
                        "123456",
                        "fp-456"
                )
        ));

        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "customer", "block");
        Assertions.assertEquals(expectedResult, result);
    }

}
