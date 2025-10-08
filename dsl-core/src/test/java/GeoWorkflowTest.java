import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.Map;

class GeoWorkflowTest {
    @Test
    void testGeohashEncodeExpr() {
        String workflow = """
            workflow 'geo'
                ruleset 'dummy'
                    'geohash_rule' geohash_encode(lat, lon, 8) = '9q8yyk8y' return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of("lat", 37.7749, "lon", -122.4194));
        Assertions.assertEquals("geohash_rule", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    void testDistanceExpr() {
        String workflow = """
            workflow 'geo'
                ruleset 'dummy'
                    'dist_rule' distance(lat1, lon1, lat2, lon2) < 600 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "lat1", 37.7749, "lon1", -122.4194, // SF
            "lat2", 34.0522, "lon2", -118.2437   // LA
        ));
        Assertions.assertEquals("dist_rule", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    void testWithinRadiusExpr() {
        String workflow = """
            workflow 'geo'
                ruleset 'dummy'
                    'near_rule' within_radius(lat1, lon1, lat2, lon2, 600) return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "lat1", 37.7749, "lon1", -122.4194, // SF
            "lat2", 34.0522, "lon2", -118.2437   // LA
        ));
        Assertions.assertEquals("near_rule", result.getRule());
        Assertions.assertEquals("block", result.getResult());
    }
} 