import com.gatekeeperx.ruleflow.functions.RuleflowFunctionKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleflowFunctionKeyTest {

    @Test
    public void sameFunctionAndArgsProduceSameKey() {
        Map<String,Object> a = Map.of("0", "DOC123", "1", "John Doe");
        Map<String,Object> b = Map.of("0", "DOC123", "1", "John Doe");
        Assertions.assertEquals(
            RuleflowFunctionKey.of("screening", a),
            RuleflowFunctionKey.of("screening", b));
    }

    @Test
    public void keyIsIndependentOfArgInsertionOrder() {
        Map<String,Object> ordered = new LinkedHashMap<>();
        ordered.put("documentNumber", "DOC123");
        ordered.put("fullName", "John Doe");
        Map<String,Object> reversed = new LinkedHashMap<>();
        reversed.put("fullName", "John Doe");
        reversed.put("documentNumber", "DOC123");
        Assertions.assertEquals(
            RuleflowFunctionKey.of("screening", ordered),
            RuleflowFunctionKey.of("screening", reversed));
    }

    @Test
    public void differentArgsProduceDifferentKey() {
        Assertions.assertNotEquals(
            RuleflowFunctionKey.of("screening", Map.of("0", "DOC123")),
            RuleflowFunctionKey.of("screening", Map.of("0", "DOC999")));
    }

    @Test
    public void differentFunctionNameProducesDifferentKey() {
        Map<String,Object> args = Map.of("0", "x");
        Assertions.assertNotEquals(
            RuleflowFunctionKey.of("screening", args),
            RuleflowFunctionKey.of("score", args));
    }

    @Test
    public void handlesNestedMapsAndListsDeterministically() {
        Map<String,Object> nested = Map.of("0", Map.of("b", 2, "a", List.of(1, 2, 3)));
        Assertions.assertEquals(
            RuleflowFunctionKey.of("f", nested),
            RuleflowFunctionKey.of("f", Map.of("0", Map.of("a", List.of(1, 2, 3), "b", 2))));
    }

    @Test
    public void handlesNullArgValues() {
        Map<String,Object> args = new LinkedHashMap<>();
        args.put("0", null);
        Assertions.assertNotNull(RuleflowFunctionKey.of("f", args));
    }
}
