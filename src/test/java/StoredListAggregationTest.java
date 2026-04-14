import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class StoredListAggregationTest {

    @Test
    public void testStoredListAnyWithItAlias() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'matched' list('blacklist').any { it.field = 'blocked' } return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expected = new WorkflowResult("test", "dummy", "matched", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field", "safe"),
                Map.of("field", "blocked"),
                Map.of("field", "other")
            )
        ));

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testStoredListAnyNoMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'matched' list('blacklist').any { it.field = 'blocked' } return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expected = new WorkflowResult("test", "default", "default", "allow");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field", "safe"),
                Map.of("field", "clean"),
                Map.of("field", "ok")
            )
        ));

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testStoredListAll() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'all_pass' list('entries').all { it.score > 0 } return pass
                default fail
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expected = new WorkflowResult("test", "dummy", "all_pass", "pass");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "entries", List.of(
                Map.of("score", 5),
                Map.of("score", 10),
                Map.of("score", 3)
            )
        ));

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testStoredListNone() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'none_flagged' list('users').none { it.flagged = true } return clean
                default dirty
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expected = new WorkflowResult("test", "dummy", "none_flagged", "clean");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "users", List.of(
                Map.of("flagged", false),
                Map.of("flagged", false),
                Map.of("flagged", false)
            )
        ));

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testStoredListCount() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'high_risk' list('transactions').count { it.risk > 5 } > 2 return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expected = new WorkflowResult("test", "dummy", "high_risk", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "transactions", List.of(
                Map.of("risk", 8),
                Map.of("risk", 2),
                Map.of("risk", 7),
                Map.of("risk", 9)
            )
        ));

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testStoredListMissingList() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'matched' list('nonexistent').any { it.field = 'x' } return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expected = new WorkflowResult("test", "default", "default", "allow");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of());

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testStoredListAnyWithParentContext() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'matched' list('blacklist').any { it.id = .userId } return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expected = new WorkflowResult("test", "dummy", "matched", "block");
        WorkflowResult result = ruleEngine.evaluate(
            Map.of("userId", "abc123"),
            Map.of("blacklist", List.of(
                Map.of("id", "xyz"),
                Map.of("id", "abc123")
            ))
        );

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testStoredListEquivalentToEvalInList() {
        String workflowWithList = """
            workflow 'test'
                ruleset 'dummy'
                    'matched' list('blacklist').any { it.field = 'blocked' } return block
                default allow
            end
        """;
        String workflowWithEvalInList = """
            workflow 'test'
                ruleset 'dummy'
                    'matched' evalInList('blacklist', elem.field = 'blocked') return block
                default allow
            end
        """;

        Map<String, List<?>> lists = Map.of(
            "blacklist", List.of(
                Map.of("field", "safe"),
                Map.of("field", "blocked")
            )
        );

        WorkflowResult resultList = new Workflow(workflowWithList).evaluate(Map.of(), lists);
        WorkflowResult resultEvalInList = new Workflow(workflowWithEvalInList).evaluate(Map.of(), lists);

        Assertions.assertEquals(resultEvalInList, resultList);
    }
}
