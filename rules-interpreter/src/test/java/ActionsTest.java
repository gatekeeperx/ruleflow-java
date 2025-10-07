import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ActionsTest {

    @Test
    public void given_workflow_with_actions_without_params_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review')
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of("manual_review", Map.of())
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_workflow_with_actions_name_wildcard_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with manual_review
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of("manual_review", Map.of())
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_workflow_with_actions_without_explicit_params_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review')
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of("manual_review", Map.of())
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_workflow_with_two_rules_must_return_ok() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review') and action('logout_user')
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of("manual_review", Map.of(), "logout_user", Map.of())
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_two_actions_one_with_params_one_without_params_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'foo': 'bar'}) and action('logout_user')
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of(
                "manual_review", Map.of("test", "me", "foo", "bar"),
                "logout_user", Map.of()
            )
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_action_with_params_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'foo': 'bar'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of("manual_review", Map.of("test", "me", "foo", "bar"))
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_action_with_params_using_request_values_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'userId': user_id})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        
        // Explicit assertions to clearly state what we want to test and expect
        Assertions.assertEquals("test", result.getWorkflow(), "Workflow name should match");
        Assertions.assertEquals("dummy", result.getRuleSet(), "Ruleset name should match");
        Assertions.assertEquals("rule_a", result.getRule(), "Rule name should match");
        Assertions.assertEquals("block", result.getResult(), "Result should be 'block'");
        
        // Verify the action parameters are correctly resolved
        Map<String, String> expectedParams = Map.of("test", "me", "userId", "15");
        Assertions.assertEquals(expectedParams, result.getActionsWithParams().get("manual_review"), 
            "Action parameters should be correctly resolved with request values");
        
        // Verify the action is present in the actions list
        Assertions.assertTrue(result.getActions().contains("manual_review"), 
            "Action should be present in the actions list");
    }

    @Test
    public void given_action_with_nonexistent_property_in_params_should_return_matched_rule_with_warnings() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'nonexistent': nonexistent_property})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        
        // The exception should be caught and the workflow should fall back to default
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        
        // Verify that the workflow fell back to default due to the exception
        Assertions.assertEquals("test", result.getWorkflow());
        Assertions.assertEquals("dummy", result.getRuleSet());
        Assertions.assertEquals("rule_a", result.getRule());
        Assertions.assertEquals("block", result.getResult());
        
        // Check that there are warnings about the failed property resolution
        Assertions.assertTrue(result.getWarnings().size() > 0, "Should have warnings about failed property resolution");
        boolean hasActionParamWarning = result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("Failed to resolve property") || warning.contains("Action parameter resolution failed"));
        Assertions.assertTrue(hasActionParamWarning, "Should have warning about action parameter resolution failure");
    }

    @Test
    public void given_action_with_nonexistent_property_in_rule_should_return_default_with_warnings() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'key': non_existent_property})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("not_user_id", 15);

        // The exception should be caught and the workflow should fall back to default
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);

        // Verify that the workflow fell back to default due to the exception
        Assertions.assertEquals("test", result.getWorkflow());
        Assertions.assertEquals("default", result.getRuleSet());
        Assertions.assertEquals("default", result.getRule());
        Assertions.assertEquals("allow", result.getResult());

        // Check that there are warnings about the failed property resolution
        Assertions.assertFalse(result.getWarnings().isEmpty(), "Should have warnings about failed property resolution");
        Assertions.assertEquals("user_id field cannot be found", result.getWarnings().stream().findFirst().get());
    }

    @Test
    public void given_action_with_empty_string_property_value_in_params_should_handle_gracefully() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'emptyValue': empty_property})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15, "empty_property", "");
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        
        // Verify that empty string values are handled gracefully
        Assertions.assertEquals("test", result.getWorkflow());
        Assertions.assertEquals("dummy", result.getRuleSet());
        Assertions.assertEquals("rule_a", result.getRule());
        Assertions.assertEquals("block", result.getResult());
        
        // Verify that empty string values are preserved
        Map<String, String> expectedParams = Map.of("test", "me", "emptyValue", "");
        Assertions.assertEquals(expectedParams, result.getActionsWithParams().get("manual_review"), 
            "Empty string property values should be preserved");
    }

    @Test
    public void given_action_with_numeric_property_value_in_params_should_handle_gracefully() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'numericValue': numeric_property})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15, "numeric_property", 42);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        
        // Verify that numeric values are handled gracefully
        Assertions.assertEquals("test", result.getWorkflow());
        Assertions.assertEquals("dummy", result.getRuleSet());
        Assertions.assertEquals("rule_a", result.getRule());
        Assertions.assertEquals("block", result.getResult());
        
        // Verify that numeric values are converted to string
        Map<String, String> expectedParams = Map.of("test", "me", "numericValue", "42");
        Assertions.assertEquals(expectedParams, result.getActionsWithParams().get("manual_review"), 
            "Numeric property values should be converted to string");
    }

    @Test
    public void given_action_with_boolean_property_value_in_params_should_handle_gracefully() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'booleanValue': boolean_property})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15, "boolean_property", true);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        
        // Verify that boolean values are handled gracefully
        Assertions.assertEquals("test", result.getWorkflow());
        Assertions.assertEquals("dummy", result.getRuleSet());
        Assertions.assertEquals("rule_a", result.getRule());
        Assertions.assertEquals("block", result.getResult());
        
        // Verify that boolean values are converted to string
        Map<String, String> expectedParams = Map.of("test", "me", "booleanValue", "true");
        Assertions.assertEquals(expectedParams, result.getActionsWithParams().get("manual_review"), 
            "Boolean property values should be converted to string");
    }

    @Test
    public void given_action_with_complex_object_property_value_in_params_should_handle_gracefully() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'objectValue': object_property})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15, "object_property", new Object() {
            @Override
            public String toString() {
                return "complex_object";
            }
        });
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        
        // Verify that complex object values are handled gracefully
        Assertions.assertEquals("test", result.getWorkflow());
        Assertions.assertEquals("dummy", result.getRuleSet());
        Assertions.assertEquals("rule_a", result.getRule());
        Assertions.assertEquals("block", result.getResult());
        
        // Verify that complex object values are converted to string using toString()
        Map<String, String> expectedParams = Map.of("test", "me", "objectValue", "complex_object");
        Assertions.assertEquals(expectedParams, result.getActionsWithParams().get("manual_review"), 
            "Complex object property values should be converted to string using toString()");
    }

    @Test
    public void given_action_with_runtime_exception_during_property_resolution_in_action_should_return_the_rule() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with action('manual_review', {'test': 'me', 'problematic': problematic_property})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15, "problematic_property", new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("Simulated runtime exception during property resolution");
            }
        });
        
        // The runtime exception should be caught and the workflow should fall back to default
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        
        // Verify that the workflow fell back to default due to the runtime exception
        Assertions.assertEquals("test", result.getWorkflow());
        Assertions.assertEquals("dummy", result.getRuleSet());
        Assertions.assertEquals("rule_a", result.getRule());
        Assertions.assertEquals("block", result.getResult());
        
        // Check that there are warnings about the runtime exception
        Assertions.assertTrue(result.getWarnings().size() > 0, "Should have warnings about runtime exception");
        boolean hasRuntimeExceptionWarning = result.getWarnings().stream()
            .anyMatch(warning -> warning.contains("RuntimeException") || warning.contains("Error while evaluating rule") || warning.contains("Failed to resolve property"));
        Assertions.assertTrue(hasRuntimeExceptionWarning, "Should have warning about runtime exception during property resolution");
    }

    @Test
    public void given_action_with_wildcard_name_and_params_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with manual_review({'test': 'me', 'foo': 'bar'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of("manual_review", Map.of("test", "me", "foo", "bar"))
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_two_actions_with_wildcard_names_and_params_must_return_valid_response() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with manual_review({'test': 'me', 'foo': 'bar'}) AND apply_restriction({'responsible': 'homer'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of(
                "manual_review", Map.of("test", "me", "foo", "bar"),
                "apply_restriction", Map.of("responsible", "homer")
            )
        );
        assertWorkflowResult(expectedResult, result);
    }

    @Test
    public void given_two_actions_with_same_name_both_must_be_in_actions_list() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' user_id = 15 return block with apply_restriction({'test': 'me', 'foo': 'bar'}) AND apply_restriction({'responsible': 'homer'})
                default allow
            end
        """;
        Map<String, Object> request = Map.of("user_id", 15);
        WorkflowResult result = new com.gatekeeperx.ruleflow.Workflow(workflow).evaluate(request);
        WorkflowResult expectedResult = new WorkflowResult(
            "test", "dummy", "rule_a", "block",
            Map.of("apply_restriction", Map.of("responsible", "homer", "test", "me", "foo", "bar"))
        );
        assertWorkflowResult(expectedResult, result);
    }

    private void assertWorkflowResult(WorkflowResult expected, WorkflowResult actual) {
        Assertions.assertEquals(expected.getWorkflow(), actual.getWorkflow());
        Assertions.assertEquals(expected.getRuleSet(), actual.getRuleSet());
        Assertions.assertEquals(expected.getRule(), actual.getRule());
        Assertions.assertEquals(expected.getResult(), actual.getResult());
        Assertions.assertEquals(expected.getActions(), actual.getActions());
        Assertions.assertEquals(expected.getActionsWithParams(), actual.getActionsWithParams());
    }
}