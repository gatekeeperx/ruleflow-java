import com.gatekeeperx.ruleflow.Workflow;
import com.gatekeeperx.ruleflow.vo.WorkflowResult;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class DateTimeTest {

    @Test
    public void givenDateLiteralWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_match' date(x) = date('2024-06-01') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateTimeWithZoneWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'dt_match' datetime(x) = datetime('2024-06-01T12:30Z') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "dt_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01T12:30Z"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateTimeWithPositiveOffsetWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'dt_match' x = '2024-06-01T12:30+02:00' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "dt_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01T12:30+02:00"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateTimeWithNegativeOffsetWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'dt_match' x = '2024-06-01T12:30-05:00' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "dt_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01T12:30-05:00"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateTimeWithSecondsAndZoneWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'dt_match' x = '2024-06-01T12:30:45Z' return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "dt_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01T12:30:45Z"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenInvalidDateTimeFormatWhenComparedMustNotMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'dt_match' date(x) = date('2024/06/01 12:30') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        // Should not match, so expect default
        WorkflowResult expectedResult = new WorkflowResult("test", "default", "default",
            "allow", Set.of("Invalid date/datetime: 2024/06/01 12:30"), Map.of(),true);
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024/06/01 12:30"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateCastWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_cast_match' date(x) = date('2024-06-01') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_cast_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateTimeCastWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'datetime_cast_match' datetime(x) = datetime('2024-06-01T12:30Z') return block
                default allow
            end
        """;

        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "datetime_cast_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01T12:30Z"));

        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDatePropertyWhenComparedMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_prop_match' date(x) = date(order_date) return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_prop_match", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("x", "2024-06-01", "order_date", "2024-06-01"));
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateCastWhenUsedInDateAddMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_add_cast' date_add('2024-06-01', 5, day) = datetime('2024-06-06') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_add_cast", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDatePropertyWhenUsedInDateAddMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_add_prop' date_add(order_date, 2, hour) = datetime('2024-06-01T02:00Z') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_add_prop", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("order_date", "2024-06-01T00:00Z"));
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateSubtractWithLiteralMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_subtract_lit' date_subtract('2024-06-01T12:30Z', 30, minute) = datetime('2024-06-01T12:00Z') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_subtract_lit", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenAddDaysWithProertyMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'add_days_prop' date_add(order_date, 3, day) = datetime('2024-06-04') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "add_days_prop", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("order_date", "2024-06-01"));
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenSubtractDaysWithLiteralMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'subtract_days_lit' date_subtract('2024-06-10', 7, day) = datetime('2024-06-03') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "subtract_days_lit", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenNowWhenComparedToNowMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'now_match' date(now()) = date(now()) return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals("block", result.getResult());
    }

    @Test
    public void givenDayOfWeekWithLiteralMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'dow_lit' day_of_week('2024-06-01') = 'SATURDAY' return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "dow_lit", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateDiffWithPropertiesMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_diff_prop' date_diff(start_date, end_date, day) = 9 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_diff_prop", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of("start_date", "2024-06-01", "end_date", "2024-06-10"));
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenComparisonWithTimeUnitMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'cmp_timeunit' datetime('2024-06-01T12:00Z') > datetime('2024-06-01T11:00Z') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "cmp_timeunit", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenInvalidDateAddUnitShouldFail() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'invalid_unit' date_add('2024-06-01', 5, month) = date('2024-11-01') return block
                default allow
            end
        """;
        Assertions.assertThrows(RuntimeException.class, () -> new Workflow(workflow));
    }

    @Test
    public void givenInvalidDateStringShouldFail() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'invalid_date' date('not-a-date') = '2024-06-01' return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertNotEquals("block", result.getResult());
    }

    @Test
    public void givenLeapYearDateAddShouldMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'leap_year' date_add('2024-02-28', 1, day) = datetime('2024-02-29') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "leap_year", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateDiffZeroShouldMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_diff_zero' dateDiff('2024-06-01', '2024-06-01', day) = 0 return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_diff_zero", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateAddMinuteRolloverShouldMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'minute_rollover' date_add('2024-06-01T23:59Z', 2, minute) = datetime('2024-06-02T00:01Z') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "minute_rollover", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }

    @Test
    public void givenDateAndDateTimeWhenComparingMustMatch() {
        String workflow = """
            workflow 'test'
                ruleset 'dummy'
                    'date_and_datetime' date('2024-06-01T12:30Z') = date('2024-06-01') return block
                default allow
            end
        """;
        Workflow ruleEngine = new Workflow(workflow);
        WorkflowResult expectedResult = new WorkflowResult("test", "dummy", "date_and_datetime", "block");
        WorkflowResult result = ruleEngine.evaluate(Map.of());
        Assertions.assertEquals(expectedResult, result);
    }
} 