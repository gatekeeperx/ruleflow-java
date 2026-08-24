import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NullCheckTest {

    // --- IS NULL ---

    @Test
    void isNull_missingField_returnsTrue() {
        Workflow engine = buildWorkflow("x IS NULL");
        WorkflowResult result = engine.evaluate(Map.of());
        assertEquals("block", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void isNull_nullValue_returnsTrue() {
        Workflow engine = buildWorkflow("x IS NULL");
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("x", null);
        WorkflowResult result = engine.evaluate(payload);
        assertEquals("block", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void isNull_presentField_returnsFalse() {
        Workflow engine = buildWorkflow("x IS NULL");
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("allow", result.getResult());
    }

    // --- IS NOT NULL ---

    @Test
    void isNotNull_presentField_returnsTrue() {
        Workflow engine = buildWorkflow("x IS NOT NULL");
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("block", result.getResult());
    }

    @Test
    void isNotNull_missingField_returnsFalse() {
        Workflow engine = buildWorkflow("x IS NOT NULL");
        WorkflowResult result = engine.evaluate(Map.of());
        assertEquals("allow", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void isNotNull_nullValue_returnsFalse() {
        Workflow engine = buildWorkflow("x IS NOT NULL");
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("x", null);
        WorkflowResult result = engine.evaluate(payload);
        assertEquals("allow", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    // --- IS EMPTY ---

    @Test
    void isEmpty_emptyString_returnsTrue() {
        Workflow engine = buildWorkflow("x IS EMPTY");
        WorkflowResult result = engine.evaluate(Map.of("x", ""));
        assertEquals("block", result.getResult());
    }

    @Test
    void isEmpty_missingField_returnsTrue() {
        Workflow engine = buildWorkflow("x IS EMPTY");
        WorkflowResult result = engine.evaluate(Map.of());
        assertEquals("block", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void isEmpty_nullValue_returnsTrue() {
        Workflow engine = buildWorkflow("x IS EMPTY");
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("x", null);
        WorkflowResult result = engine.evaluate(payload);
        assertEquals("block", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void isEmpty_nonEmptyString_returnsFalse() {
        Workflow engine = buildWorkflow("x IS EMPTY");
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("allow", result.getResult());
    }

    @Test
    void isEmpty_nonStringValue_returnsFalse() {
        Workflow engine = buildWorkflow("x IS EMPTY");
        WorkflowResult result = engine.evaluate(Map.of("x", 42));
        assertEquals("allow", result.getResult());
    }

    // --- IS NOT EMPTY ---

    @Test
    void isNotEmpty_nonEmptyString_returnsTrue() {
        Workflow engine = buildWorkflow("x IS NOT EMPTY");
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("block", result.getResult());
    }

    @Test
    void isNotEmpty_emptyString_returnsFalse() {
        Workflow engine = buildWorkflow("x IS NOT EMPTY");
        WorkflowResult result = engine.evaluate(Map.of("x", ""));
        assertEquals("allow", result.getResult());
    }

    // --- IS BLANK ---

    @Test
    void isBlank_whitespaceOnly_returnsTrue() {
        Workflow engine = buildWorkflow("x IS BLANK");
        WorkflowResult result = engine.evaluate(Map.of("x", "   "));
        assertEquals("block", result.getResult());
    }

    @Test
    void isBlank_emptyString_returnsTrue() {
        Workflow engine = buildWorkflow("x IS BLANK");
        WorkflowResult result = engine.evaluate(Map.of("x", ""));
        assertEquals("block", result.getResult());
    }

    @Test
    void isBlank_missingField_returnsTrue() {
        Workflow engine = buildWorkflow("x IS BLANK");
        WorkflowResult result = engine.evaluate(Map.of());
        assertEquals("block", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void isBlank_nonBlankString_returnsFalse() {
        Workflow engine = buildWorkflow("x IS BLANK");
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("allow", result.getResult());
    }

    // --- IS NOT BLANK ---

    @Test
    void isNotBlank_nonBlankString_returnsTrue() {
        Workflow engine = buildWorkflow("x IS NOT BLANK");
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("block", result.getResult());
    }

    @Test
    void isNotBlank_whitespaceOnly_returnsFalse() {
        Workflow engine = buildWorkflow("x IS NOT BLANK");
        WorkflowResult result = engine.evaluate(Map.of("x", "   "));
        assertEquals("allow", result.getResult());
    }

    // --- Combinations ---

    @Test
    void isNotNull_combinedWithOtherCondition() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' x IS NOT NULL AND x <> 'test' return block
                default allow
            end
        """;
        Workflow engine = new Workflow(workflow);
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("block", result.getResult());
    }

    @Test
    void isNotNull_combinedWithOtherCondition_nullField() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' x IS NOT NULL AND x <> 'test' return block
                default allow
            end
        """;
        Workflow engine = new Workflow(workflow);
        WorkflowResult result = engine.evaluate(Map.of());
        assertEquals("allow", result.getResult());
    }

    // --- Nested properties ---

    @Test
    void isNull_nestedProperty_missing() {
        Workflow engine = buildWorkflow("device.fingerprint IS NULL");
        HashMap<String, Object> device = new HashMap<>();
        device.put("fingerprint", null);
        WorkflowResult result = engine.evaluate(Map.of("device", device));
        assertEquals("block", result.getResult());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void isNull_nestedProperty_present() {
        Workflow engine = buildWorkflow("device.fingerprint IS NULL");
        WorkflowResult result = engine.evaluate(Map.of("device", Map.of("fingerprint", "fp-123")));
        assertEquals("allow", result.getResult());
    }

    @Test
    void isNotNull_nestedProperty_present() {
        Workflow engine = buildWorkflow("device.fingerprint IS NOT NULL");
        WorkflowResult result = engine.evaluate(Map.of("device", Map.of("fingerprint", "fp-123")));
        assertEquals("block", result.getResult());
    }

    // --- Case insensitivity ---

    @Test
    void caseInsensitive_isNull() {
        Workflow engine = buildWorkflow("x is null");
        WorkflowResult result = engine.evaluate(Map.of());
        assertEquals("block", result.getResult());
    }

    @Test
    void caseInsensitive_isNotEmpty() {
        Workflow engine = buildWorkflow("x Is Not Empty");
        WorkflowResult result = engine.evaluate(Map.of("x", "hello"));
        assertEquals("block", result.getResult());
    }

    @Test
    void caseInsensitive_isBlank() {
        Workflow engine = buildWorkflow("x IS BLANK");
        WorkflowResult result = engine.evaluate(Map.of("x", "   "));
        assertEquals("block", result.getResult());
    }

    private Workflow buildWorkflow(String condition) {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'rule_a' %s return block
                default allow
            end
        """.formatted(condition);
        return new Workflow(workflow);
    }
}
