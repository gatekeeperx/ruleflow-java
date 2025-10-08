import static com.gatekeeperx.ruleflow.visitors.SyntaxValidatorVisitor.validate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class SyntaxValidatorVisitorTest {
  @Test
  void validWorkflowShouldProduceNoErrors() {
    String input = """
            WORKFLOW 'ValidFlow'
                RULESET 'Main'
                    'Rule1'
                    (
                        amount > 100
                        THEN
                        action('notify', {'type': 'email'})
                    )
                DEFAULT
                RETURN 'fallback'
                WITH action('log')
            END
            """;

    List<String> errors = validate(input);
    assertTrue(errors.isEmpty(), "Expected no syntax errors, but found: " + errors);
  }

  @Test
  void invalidWorkflowShouldReportErrors() {
    String input = """
            WORKFLOW 'InvalidFlow'
                RULESET 'Main'
                    'Rule1'
                    (
                        amount >
                        THEN
                        action('notify', {'type': 'email'})
                    )
                DEFAULT
                RETURN
                'fallback'
            END
            """;

    List<String> errors = validate(input);
    assertFalse(errors.isEmpty(), "Expected syntax errors but found none.");
    errors.forEach(System.out::println);
  }
}
