import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MultiMatchTest {

  @Test
  void validWorkflowShouldProduceNoErrors() {
    String input = """
            WORKFLOW 'ValidFlow'
            EVALUATION_MODE MULTI_MATCH
            RULESET 'Main'
                'Rule1' amount > 100 return notify
                'Rule2' user_id = 50 return call_2fa
            DEFAULT RETURN 'fallback' WITH action('log')
            END
            """;

    Map<String, Object> amount = Map.of("amount", 200, "user_id", 50);
    WorkflowResult result = new Workflow(input)
        .evaluate(amount);

    assertFalse(result.isError());
    assertFalse(result.getMatchedRules().isEmpty());
    assertNotNull(result.getMatchedRules().get(0));
    assertNotNull(result.getMatchedRules().get(1));
    assertEquals("ValidFlow", result.getWorkflow());
    assertEquals("Main", result.getRuleSet());
    assertEquals("Rule1", result.getRule());
    assertEquals("notify", result.getResult());
    assertNotNull(result.getActionsWithParams());
    assertEquals("Main", result.getMatchedRules().get(0).getRuleSet());
    assertEquals("Rule1", result.getMatchedRules().get(0).getRule());
    assertEquals("notify", result.getMatchedRules().get(0).getResult());
    assertEquals("Main", result.getMatchedRules().get(1).getRuleSet());
    assertEquals("Rule2", result.getMatchedRules().get(1).getRule());
    assertEquals("call_2fa", result.getMatchedRules().get(1).getResult());
    assertNotNull(result.getWarnings());
  }
}
