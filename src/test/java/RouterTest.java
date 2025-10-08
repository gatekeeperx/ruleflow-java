import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RouterTest {

    @Test
    public void givenRoutingWorkflowWhenRoutingToDefaultCaseMustReturnValuesSuccessfully() {
        String workflow = """
          workflow 'providers'
          ruleset 'routing' 
               'provider1_master1' user_id = '101000042449' return route with route({'provider':'provider1', 'merchant_id': 'master1'})
               'provider1_master2' user_id = '151000033978'  return route with route({'provider': 'provider1', 'merchant_id': 'master1'})
               'provider2_master1' user_id = '151000033979' return route with route({'provider': 'provider2', 'merchant_id': 'master3'})
               'provider3' user_id = '151000033977' return route with route({'provider': 'provider3', 'merchant_id': 'merchant4'})
               'provider4' user_id = '15100003391381823' return route with route({'provider': 'provider4', 'tag': 'merchant5'})
               'provider5' user_id = '151000033976' return route with route({'provider': 'internal'})
           default route with route({'provider': 'internal'})
           end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "providers",
            "default",
            "default",
            "route",
            Map.of("route", Map.of("provider", "internal"))
        );

        WorkflowResult result = ruleEngine.evaluate(Map.of("user_id", "4"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenRoutingWorkflowWhenRoutingToSpecificRuleCaseMustReturnValue() {
        String workflow = """
          workflow 'router'
               ruleset 'providers' 
               'provider1' country in 'ar' return route with route({'provider':'provider1', 'merchant_id': 'master'})
               'provider2' country in 'us' return route with route({'provider': 'provider2', 'merchant_id': 'master2'})
               'provider3' user.type = 'premium' OR user_is_blacklisted = false return route with route({'provider': 'provider3', 'merchant_id': 'master3'})
               'provider4' order.vertical in 'restaurants', 'travel' return route with route({'provider': 'provider4', 'merchant_id': 'master4'})
           default do_not_route
           end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult(
            "router",
            "providers",
            "provider2",
            "route",
            Map.of("route", Map.of("provider", "provider2", "merchant_id", "master2"))
        );

        WorkflowResult result = ruleEngine.evaluate(Map.of("country", "us"));

        Assertions.assertEquals(expectedResult, result);
    }
}