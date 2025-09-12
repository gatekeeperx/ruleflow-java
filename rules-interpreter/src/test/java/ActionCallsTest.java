import io.github.iamrenny.ruleflow.vo.WorkflowResult;
import io.github.iamrenny.ruleflow.vo.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

class ActionCallsTest {

    @Test
    public void actionCalls_should_store_multiple_actions_with_same_name_separately() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with apply_restriction({'test': 'me'}) AND apply_restriction({'responsible': 'homer'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new io.github.iamrenny.ruleflow.Workflow(workflow).evaluate(request);
        
        List<Action> actionCalls = result.getActionCalls();
        Assertions.assertEquals(2, actionCalls.size());
        
        // Verify both actions have the same name but different parameters
        Assertions.assertEquals("apply_restriction", actionCalls.get(0).getName());
        Assertions.assertEquals("apply_restriction", actionCalls.get(1).getName());
        Assertions.assertEquals(Map.of("test", "me"), actionCalls.get(0).getParams());
        Assertions.assertEquals(Map.of("responsible", "homer"), actionCalls.get(1).getParams());
    }

    @Test
    public void actionCalls_should_work_with_different_action_names() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me'}) and action('logout_user', {'reason': 'suspicious'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new io.github.iamrenny.ruleflow.Workflow(workflow).evaluate(request);
        
        List<Action> actionCalls = result.getActionCalls();
        Assertions.assertEquals(2, actionCalls.size());
        Assertions.assertEquals("manual_review", actionCalls.get(0).getName());
        Assertions.assertEquals("logout_user", actionCalls.get(1).getName());
    }



    @Test
    public void actionCalls_should_store_multiple_equal_actions_separately() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with apply_restriction({'test': 'me'}) AND apply_restriction({'test': 'me'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new io.github.iamrenny.ruleflow.Workflow(workflow).evaluate(request);

        List<Action> actionCalls = result.getActionCalls();
        Assertions.assertEquals(2, actionCalls.size());

        // Verify both actions have the same name but different parameters
        Assertions.assertEquals("apply_restriction", actionCalls.get(0).getName());
        Assertions.assertEquals("apply_restriction", actionCalls.get(1).getName());
        Assertions.assertEquals(Map.of("test", "me"), actionCalls.get(0).getParams());
        Assertions.assertEquals(Map.of("test", "me"), actionCalls.get(1).getParams());
    }

    @Test
    public void actionCalls_should_store_multiple_equal_actions_with_different_values_separately() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with apply_restriction({'test': 'me'}) AND apply_restriction({'test': 'not_me'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new io.github.iamrenny.ruleflow.Workflow(workflow).evaluate(request);

        List<Action> actionCalls = result.getActionCalls();
        Assertions.assertEquals(2, actionCalls.size());

        // Verify both actions have the same name but different parameters
        Assertions.assertEquals("apply_restriction", actionCalls.get(0).getName());
        Assertions.assertEquals("apply_restriction", actionCalls.get(1).getName());
        Assertions.assertEquals(Map.of("test", "me"), actionCalls.get(0).getParams());
        Assertions.assertEquals(Map.of("test", "not_me"), actionCalls.get(1).getParams());
    }

}
