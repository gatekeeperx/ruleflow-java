import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class EvalInListTest {

    @Test
    public void testEvalInListBasicMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "other"),
                Map.of("field1", "test"),
                Map.of("field1", "another")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListNoMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "default", "default", "allow");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "other"),
                Map.of("field1", "another"),
                Map.of("field1", "different")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListNestedProperty() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1.field2 = 'value') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", Map.of("field2", "other")),
                Map.of("field1", Map.of("field2", "value")),
                Map.of("field1", Map.of("field2", "another"))
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithComparison() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'high_value' evalInList('items', elem.price > 100) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "high_value", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "items", List.of(
                Map.of("price", 50),
                Map.of("price", 150),
                Map.of("price", 75)
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithAndCondition() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test' AND elem.field2 = 'value') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "test", "field2", "other"),
                Map.of("field1", "test", "field2", "value"),
                Map.of("field1", "other", "field2", "value")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithOrCondition() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test' OR elem.field1 = 'other') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "different"),
                Map.of("field1", "test"),
                Map.of("field1", "another")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithNotEquals() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 <> 'test') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "test"),
                Map.of("field1", "test"),
                Map.of("field1", "other")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithLessThan() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'low_value' evalInList('items', elem.price < 50) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "low_value", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "items", List.of(
                Map.of("price", 100),
                Map.of("price", 30),
                Map.of("price", 75)
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithGreaterThanOrEqual() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'high_value' evalInList('items', elem.price >= 100) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "high_value", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "items", List.of(
                Map.of("price", 50),
                Map.of("price", 100),
                Map.of("price", 75)
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithParentContextAccess() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = user.id) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "user", Map.of("id", "test123")
        ), Map.of(
            "blacklist", List.of(
                Map.of("field1", "other"),
                Map.of("field1", "test123"),
                Map.of("field1", "another")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListMissingList() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('nonexistent', elem.field1 = 'test') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "default", "default", "allow");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of());

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListEmptyList() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "default", "default", "allow");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of()
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListComplexPredicate() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test' AND (elem.field2 > 100 OR elem.field3 = 'active')) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "test", "field2", 50, "field3", "inactive"),
                Map.of("field1", "test", "field2", 150, "field3", "inactive"),
                Map.of("field1", "other", "field2", 200, "field3", "active")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithStringContains() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 contains 'test') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "other"),
                Map.of("field1", "testvalue"),
                Map.of("field1", "another")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithMultipleFields() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test' AND elem.field2 = 'value' AND elem.field3 = 'active') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "test", "field2", "value", "field3", "inactive"),
                Map.of("field1", "test", "field2", "value", "field3", "active"),
                Map.of("field1", "other", "field2", "value", "field3", "active")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListCaseInsensitive() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'TEST') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "blacklist", List.of(
                Map.of("field1", "other"),
                Map.of("field1", "TEST"),
                Map.of("field1", "another")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithNumericComparison() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('items', elem.quantity * elem.price > 1000) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of(), Map.of(
            "items", List.of(
                Map.of("quantity", 5, "price", 100),
                Map.of("quantity", 10, "price", 150),
                Map.of("quantity", 2, "price", 50)
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithTransactionEmailAndDateComparison() {
        // Test combining parent context access (transaction.email), date function (now()), 
        // and list element properties (elem.fieldName1, elem.endDate)
        // This tests that elem properties work together with parent context properties and date functions
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('aList', elem.fieldName1 = transaction.email AND date(elem.endDate) >= date(now())) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");

        // Use ISO date strings (yyyy-MM-dd format) as the date() function expects strings
        // Format dates as yyyy-MM-dd which is what date() function expects
        LocalDateTime now = LocalDateTime.now();
        String pastDate = now.minusYears(3).toLocalDate().format(java.time.format.DateTimeFormatter.ISO_DATE);
        String futureDate = now.plusYears(3).toLocalDate().format(java.time.format.DateTimeFormatter.ISO_DATE);
        String currentDate = now.toLocalDate().format(java.time.format.DateTimeFormatter.ISO_DATE);

        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "transaction", Map.of("email", "elem1")
        ), Map.of(
            "aList", List.of(
                Map.of("fieldName1", "other", "endDate", currentDate),      // Wrong email
                Map.of("fieldName1", "elem1", "endDate", pastDate),         // Right email but past date (should not match)
                Map.of("fieldName1", "elem1", "endDate", futureDate)        // Matches email AND future date (should match)
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithSwappedOperands() {
        // Test with swapped operands to ensure order doesn't matter
        // transaction.email = elem.fieldName1 (instead of elem.fieldName1 = transaction.email)
        // date(now()) <= date(elem.endDate) (instead of date(elem.endDate) >= date(now()))
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('aList', transaction.email = elem.fieldName1 AND date(now()) <= date(elem.endDate)) return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");

        // Use ISO date strings (yyyy-MM-dd format) as the date() function expects strings
        LocalDateTime now = LocalDateTime.now();
        String pastDate = now.minusYears(3).toLocalDate().format(java.time.format.DateTimeFormatter.ISO_DATE);
        String futureDate = now.plusYears(3).toLocalDate().format(java.time.format.DateTimeFormatter.ISO_DATE);
        String currentDate = now.toLocalDate().format(java.time.format.DateTimeFormatter.ISO_DATE);

        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "transaction", Map.of("email", "elem1")
        ), Map.of(
            "aList", List.of(
                Map.of("fieldName1", "other", "endDate", currentDate),      // Wrong email
                Map.of("fieldName1", "elem1", "endDate", pastDate),         // Right email but past date (should not match)
                Map.of("fieldName1", "elem1", "endDate", futureDate)        // Matches email AND future date (should match)
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void testEvalInListWithElemFieldInJson() {
        // Test that 'elem' keyword in evalInList refers to the list item, not a JSON field named 'elem'
        // This ensures that even if the JSON has a field called 'elem', it doesn't interfere
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'blocked' evalInList('blacklist', elem.field1 = 'test') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "blocked", "block");
        
        // JSON has a field named 'elem' - this should NOT interfere with elem keyword in evalInList
        WorkflowResult result = ruleEngine.evaluate(Map.of(
            "elem", "someValue",  // JSON field named 'elem'
            "otherField", "otherValue"
        ), Map.of(
            "blacklist", List.of(
                Map.of("field1", "other"),
                Map.of("field1", "test"),  // This should match via elem.field1
                Map.of("field1", "another")
            )
        ));

        Assertions.assertEquals(expectedResult, result);
    }
}

