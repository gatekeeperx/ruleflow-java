import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.PendingFunctionCall;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class FunctionCallExtractorTest {

    private static final String WF = """
        workflow 'test'
            ruleset 'sanctions'
                'hit' screening(customer.document, customer.name + ' ' + customer.lastName).matchCount > 0 return block
            default allow
        end
        """;

    @Test
    public void extractsResolvedArgsForScreening() {
        List<PendingFunctionCall> calls = new Workflow(WF).extractFunctionCalls(
            Map.of("customer", Map.of("document", "DOC1", "name", "John", "lastName", "Doe")),
            Map.of());

        Assertions.assertEquals(1, calls.size());
        PendingFunctionCall call = calls.get(0);
        Assertions.assertEquals("screening", call.getFunctionName());
        Assertions.assertEquals("DOC1", call.getArgs().get("0"));
        Assertions.assertEquals("John Doe", call.getArgs().get("1"));
        Assertions.assertNotNull(call.getKey());
    }

    @Test
    public void deduplicatesIdenticalCalls() {
        String wf = """
            workflow 'test'
                ruleset 'a'
                    'r1' screening(customer.document).matchCount > 0 return block
                    'r2' screening(customer.document).matchCount > 5 return review
                default allow
            end
            """;
        List<PendingFunctionCall> calls = new Workflow(wf).extractFunctionCalls(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of());
        Assertions.assertEquals(1, calls.size());
    }

    @Test
    public void returnsEmptyWhenNoCustomFunctions() {
        String wf = """
            workflow 'test'
                ruleset 'a'
                    'r1' amount > 100 return block
                default allow
            end
            """;
        List<PendingFunctionCall> calls = new Workflow(wf).extractFunctionCalls(
            Map.of("amount", 50), Map.of());
        Assertions.assertTrue(calls.isEmpty());
    }

    @Test
    public void keyMatchesEvaluationTimeKey() {
        Map<String,Object> payload = Map.of("customer", Map.of("document", "DOC1", "name", "John", "lastName", "Doe"));
        List<PendingFunctionCall> calls = new Workflow(WF).extractFunctionCalls(payload, Map.of());
        String extractKey = calls.get(0).getKey();

        String recomputed = com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey.of(
            "screening", Map.of("0", "DOC1", "1", "John Doe"));
        Assertions.assertEquals(recomputed, extractKey);
    }
}
