import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultipleValuesListTest {

  @Test
  public void testInMethodMultipleValuesLiteralList() {
    String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device_and_merchant' (device.fingerprint, order.merchant.merchantId) in (('fp-456', 'merchant-002'), ('fp-123', 'merchant-001')) return block
               default allow
               end
            """;
    Workflow ruleEngine = new Workflow(workflow);

    WorkflowResult result = ruleEngine.evaluate(Map.of(
        "device", Map.of("fingerprint", "fp-123"),
        "order", Map.of("merchant", Map.of("merchantId", "merchant-001"))
    ));

    WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "device_and_merchant", "block");
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  public void testWithMultipleValuesList() {
    String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device_and_merchant' (device.fingerprint, order.merchant.merchantId) in list('fingerprint_merchant_blacklist') return block
               default allow
               end
            """;
    Workflow ruleEngine = new Workflow(workflow);

    WorkflowResult result = ruleEngine.evaluate(Map.of(
        "device", Map.of("fingerprint", "fp-123"),
        "order", Map.of("merchant", Map.of("merchantId", "merchant-001"))
    ), Map.of(
        "fingerprint_merchant_blacklist", List.of(
           List.of("fp-123", "merchant-001"),
            List.of("fp-456", "merchant-002")
        )
    ));

    WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "device_and_merchant", "block");
    Assertions.assertEquals(expectedResult, result);
  }


  @Test
  public void testWithMultipleValuesDontMatch() {
    String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device_and_merchant' (device.fingerprint, order.merchant.merchantId) in (('fp-456', 'merchant-002'), ('fp-123', 'merchant-002')) return block
               default allow
               end
            """;

    Workflow ruleEngine = new Workflow(workflow);

    WorkflowResult result = ruleEngine.evaluate(Map.of(
        "device", Map.of("fingerprint", "fp-123"),
        "order", Map.of("merchant", Map.of("merchantId", "merchant-001"))
    ));

    WorkflowResult expectedResult = new WorkflowResult("test", "default", "default", "allow");
    Assertions.assertEquals(expectedResult, result);
  }

  @Test
  public void testWithMultipleValuesIncompleteLiteralList() {
    String workflow = """
           workflow 'test'
               ruleset 'dummy'
                   'device_and_merchant' (device.fingerprint, order.merchant.merchantId) in (('fp-123'), ('fp-352', 'merchant-001')) return block
               default allow
               end
            """;
    Workflow ruleEngine = new Workflow(workflow);

    WorkflowResult result = ruleEngine.evaluate(Map.of(
        "device", Map.of("fingerprint", "fp-123"),
        "order", Map.of("merchant", Map.of("merchantId", "merchant-001"))
    ));




    Assertions.assertEquals("test", result.getWorkflow());
    Assertions.assertEquals("default", result.getRuleSet());
    Assertions.assertEquals("default", result.getRule());
    Assertions.assertEquals("allow", result.getResult());
  }

}
