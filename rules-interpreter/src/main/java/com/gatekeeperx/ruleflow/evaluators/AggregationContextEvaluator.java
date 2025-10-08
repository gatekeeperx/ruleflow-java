package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AggregationContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.AggregationContext> {
    private static final Logger logger = LoggerFactory.getLogger(AggregationContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.AggregationContext ctx, Visitor visitor) {
        Object value = visitor.visit(ctx.value);

        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            boolean res = false;
            switch (ctx.op.getType()) {
                case RuleFlowLanguageLexer.K_ALL:
                    res = list.stream().allMatch(
                        data -> (Boolean) evalPredicate(data, visitor.getRoot(), visitor.getLists(),
                            ctx.predicate));
                    logger.debug("Aggregation: ALL expr={}, result={}", value, res);

                    return res;
                case RuleFlowLanguageLexer.K_ANY:
                    res = list.stream().anyMatch(
                        data -> (Boolean) evalPredicate(data, visitor.getRoot(), visitor.getLists(),
                            ctx.predicate));
                    logger.debug("Aggregation: ANY expr={}, result={}", value, res);
                    return res;
                case RuleFlowLanguageLexer.K_NONE:
                    res = list.stream().noneMatch(
                        data -> (Boolean) evalPredicate(data, visitor.getRoot(), visitor.getLists(),
                            ctx.predicate));
                    logger.debug("Aggregation: NONE expr={}, result={}", value, res);
                    return res;
                case RuleFlowLanguageLexer.K_AVERAGE:
                    Object average = average(list, ctx.predicate, visitor.getLists(),
                        visitor.getRoot());
                    logger.debug("Aggregation: AVERAGE expr={}, result={}", value, average);
                    return average;
                case RuleFlowLanguageLexer.K_COUNT:
                    Object count = count(list, ctx.predicate, visitor.getLists(),
                        visitor.getRoot());
                    logger.debug("Aggregation: COUNT expr={}, result={}", value, count);
                    return count;
                case RuleFlowLanguageLexer.K_DISTINCT:
                    Object distinctBy = distinctBy(list, ctx.predicate, visitor.getLists(),
                        visitor.getRoot());
                    logger.debug("Aggregation: DISTINCT expr={}, result={}", value, distinctBy);
                    return res;
                default:
                    logger.debug("Aggregation: unknown expr={}", value);
                throw new RuntimeException("Operation not supported: " + ctx.op.getText());
            }
        } else {
            throw new RuntimeException(ctx.value.getText() + " is not a Collection");
        }
    }

    private Object distinctBy(
        List<?> list,
        RuleFlowLanguageParser.ExprContext predicate,
        Map<String, List<?>> lists,
        Object root) {
        if (predicate == null) {
            return list;
        } else if (predicate instanceof RuleFlowLanguageParser.ValueContext) {
            return list.stream().distinct().collect(Collectors.toList());
        } else {
            return list.stream().distinct().map(data -> evalPredicate(data, root, lists, predicate)).collect(Collectors.toList());
        }
    }

    private Object average(
        List<?> list,
        RuleFlowLanguageParser.ExprContext predicate,
        Map<String, List<?>> lists,
        Object root) {
        BigDecimal count = new BigDecimal(count(list, predicate, lists, root).toString());
        return count.divide(BigDecimal.valueOf(list.size()), 3, RoundingMode.DOWN);
    }

    private Object count(
        List<?> list,
        RuleFlowLanguageParser.ExprContext predicate,
        Map<String, List<?>> lists,
        Object root) {
        if (predicate == null) {
            return BigDecimal.valueOf(list.size());
        } else {
            long count = list.stream()
                .filter(data -> (Boolean) new Visitor((Map<String, Object>) data, lists, (Map<String, Object>) root).visit(predicate))
            .count();
            return BigDecimal.valueOf(count);
        }
    }

    private Object evalPredicate(Object data, Object root, Map<String, List<?>> lists, RuleFlowLanguageParser.ExprContext ctx) {
        // Handle direct value comparisons in predicates
        if (ctx instanceof RuleFlowLanguageParser.ValueContext) {
            // For direct value comparisons like {'blocked'}, compare the list item directly with the predicate value
            Object predicateValue = new Visitor((Map<String, Object>) root, lists, (Map<String, Object>) root).visit(ctx);
            return compareValues(data, predicateValue);
        } else {
            // For property comparisons like { type = 'a' }, evaluate in the context of the list item
            return new Visitor((Map<String, Object>) data, lists, (Map<String, Object>) root).visit(ctx);
        }
    }

    private boolean compareValues(Object data, Object predicateValue) {
        if (data == null && predicateValue == null) {
            return true;
        }
        if (data == null || predicateValue == null) {
            return false;
        }
        
        // Handle numeric comparisons (Integer, Long, Double, etc.)
        if (data instanceof Number && predicateValue instanceof Number) {
            return ((Number) data).doubleValue() == ((Number) predicateValue).doubleValue();
        }
        
        // Handle other types with standard equals
        return data.equals(predicateValue);
    }
}