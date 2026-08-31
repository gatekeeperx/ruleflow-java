import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.functions.RuleflowFunction;
import com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FunctionPreResolutionTest {

    private static final String WF = """
        workflow 'test'
            ruleset 'sanctions'
                'hit' screening(customer.document).matchCount > 0 return block
            default allow
        end
        """;

    @Test
    public void preResolvedHitSkipsFunctionCall() {
        AtomicInteger calls = new AtomicInteger();
        RuleflowFunction spy = args -> { calls.incrementAndGet(); return Map.of("matchCount", 0); };

        String key = RuleflowFunctionKey.of("screening", Map.of("0", "DOC1"));
        Map<String,Object> resolved = Map.of(key, Map.of("matchCount", 3));

        WorkflowResult result = new Workflow(WF).evaluate(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of(),
            Map.of("screening", spy),
            resolved);

        Assertions.assertEquals("block", result.getResult());
        Assertions.assertEquals(0, calls.get());
    }

    @Test
    public void cacheMissFallsBackToFunction() {
        AtomicInteger calls = new AtomicInteger();
        RuleflowFunction spy = args -> { calls.incrementAndGet(); return Map.of("matchCount", 7); };

        WorkflowResult result = new Workflow(WF).evaluate(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of(),
            Map.of("screening", spy),
            Map.of());

        Assertions.assertEquals("block", result.getResult());
        Assertions.assertEquals(1, calls.get());
    }

    @Test
    public void preResolvedHitWorksWithoutLiveFunction() {
        // No live function supplied at all; a pre-resolved hit must still work.
        String key = RuleflowFunctionKey.of("screening", Map.of("0", "DOC1"));
        Map<String,Object> resolved = Map.of(key, Map.of("matchCount", 3));

        WorkflowResult result = new Workflow(WF).evaluate(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of(),
            Map.of(),
            resolved);

        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    public void existingThreeArgOverloadUnaffected() {
        RuleflowFunction fn = args -> Map.of("matchCount", 0);
        WorkflowResult result = new Workflow(WF).evaluate(
            Map.of("customer", Map.of("document", "DOC1")),
            Map.of(),
            Map.of("screening", fn));
        Assertions.assertEquals("allow", result.getResult());
    }
}
