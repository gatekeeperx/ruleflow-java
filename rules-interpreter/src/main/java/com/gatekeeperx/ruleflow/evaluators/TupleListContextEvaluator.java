package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.ValidPropertyContext;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TupleListContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.TupleListContext> {
    private static final Logger logger = LoggerFactory.getLogger(TupleListContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.TupleListContext ctx, Visitor visitor) throws PropertyNotFoundException {
        int type = ctx.op.getType();
        boolean isNegated = ctx.not != null;

        Object result = switch (type) {
            case RuleFlowLanguageLexer.K_CONTAINS -> (Boolean) evalContains(ctx, visitor);
            case RuleFlowLanguageLexer.K_IN -> (Boolean) evalIn(ctx, visitor);
            case RuleFlowLanguageLexer.K_STARTS_WITH -> (Boolean) evalStartsWith(ctx, visitor);
            default -> throw new RuntimeException("Unknown operation: " + ctx.op.getText());
        };

        Object finalResult = isNegated ? !((Boolean) result) : result;
        logger.debug("TupleList: op={}, not={}, result={}", ctx.op.getText(), isNegated, finalResult);
        return finalResult;
    }

    private Object evalIn(RuleFlowLanguageParser.TupleListContext ctx, Visitor visitor) {
        List<String> inputTuple = resolvePropertyTuple(visitor, ctx.propertyTuple().validProperty());

        if (ctx.values.literalTupleList != null) {
            List<String> literals = ctx.values.string_literal().stream()
                .map(lit -> lit.getText().replace("'", ""))
                .collect(Collectors.toList());
            List<List<String>> tuples = resolveLiteralTuples(literals, inputTuple.size());
            return tuples.stream().anyMatch(tuple -> tuple.equals(inputTuple));

        } else if (ctx.values.storedList != null) {
            String listKey = ctx.values.string_literal(0).getText().replace("'", "");
            List<?> stored = visitor.getLists().get(listKey);

            if (stored != null && stored.stream().allMatch(i -> i instanceof List<?> tuple && tuple.size() == inputTuple.size())) {
                return stored.stream().map(i -> (List<?>) i)
                    .anyMatch(tuple -> {
                        for (int i = 0; i < tuple.size(); i++) {
                            if (!tuple.get(i).equals(inputTuple.get(i))) {
                                return false;
                            }
                        }
                        return true;
                    });
            }
            return false;

        } else if (ctx.values.validProperty() != null) {
            List<?> values = (List<?>) visitor.visit(ctx.values.validProperty());
            return values.contains(null);
        }

        throw new RuntimeException("Unsupported value type: " + ctx.values);
    }

    private Object evalContains(RuleFlowLanguageParser.TupleListContext ctx, Visitor visitor) {
        Object value = visitor.visit(ctx);
        List<String> values = resolveStringList(ctx, visitor);
        return values.stream().anyMatch(val -> value.toString().contains(val));
    }

    private Object evalStartsWith(RuleFlowLanguageParser.TupleListContext ctx, Visitor visitor) throws PropertyNotFoundException {
        Object value = visitor.visit(ctx.values);
        List<String> values = resolveStringList(ctx, visitor);
        return values.stream().anyMatch(val -> value.toString().startsWith(val));
    }

    private List<String> resolvePropertyTuple(Visitor visitor, List<ValidPropertyContext> properties) {
        return properties.stream()
            .map(visitor::visit)
            .map(String.class::cast)
            .collect(Collectors.toList());
    }

    private List<List<String>> resolveLiteralTuples(List<String> literals, int tupleSize) {
        if (literals.size() % tupleSize != 0) {
            throw new IllegalArgumentException("Literal count must be divisible by tuple size. Got " + literals.size() + " elements for tuple size " + tupleSize);
        }

        List<List<String>> tuples = new ArrayList<>();
        for (int i = 0; i < literals.size(); i += tupleSize) {
            tuples.add(new ArrayList<>(literals.subList(i, i + tupleSize)));
        }
        return tuples;
    }

    private List<String> resolveStringList(RuleFlowLanguageParser.TupleListContext context, Visitor visitor) {
        if (context.values != null) {
            return context.values.string_literal().stream()
                .map(lit -> lit.getText().replace("'", ""))
                .collect(Collectors.toList());
        } else if (context.values.storedList != null) {
            if (context.values.string_literal().isEmpty()) {
                throw new RuntimeException("Expected at least one string literal in stored list");
            }
            List<?> listKey = visitor.getLists().get(context.values.storedList.getText());
            return listKey != null ? listKey.stream().map(Object::toString).collect(Collectors.toList()) : List.of();
        } else {
            throw new RuntimeException("Unknown list type");
        }
    }
}
