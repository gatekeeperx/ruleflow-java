package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageLexer;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListContextEvaluator implements ContextEvaluator<RuleFlowLanguageParser.ListContext> {
    private static final Logger logger = LoggerFactory.getLogger(ListContextEvaluator.class);

    @Override
    public Object evaluate(RuleFlowLanguageParser.ListContext ctx, Visitor visitor)
        throws PropertyNotFoundException, UnexpectedSymbolException {
        Object result;
        if (ctx.not == null && ctx.op.getType() == RuleFlowLanguageLexer.K_CONTAINS) {
            result = evalContains(ctx, visitor);
        } else if (ctx.not != null && ctx.op.getType() == RuleFlowLanguageLexer.K_CONTAINS) {
            result = !(Boolean) evalContains(ctx, visitor);
        } else if (ctx.not == null && ctx.op.getType() == RuleFlowLanguageLexer.K_IN) {
            result = evalIn(ctx, visitor);
        } else if (ctx.not != null && ctx.op.getType() == RuleFlowLanguageLexer.K_IN) {
            result = !(Boolean) evalIn(ctx, visitor);
        } else if (ctx.not == null && ctx.op.getType() == RuleFlowLanguageLexer.K_STARTS_WITH) {
            result = evalStartsWith(ctx, visitor);
        } else if (ctx.not != null && ctx.op.getType() == RuleFlowLanguageLexer.K_STARTS_WITH) {
            result = !(Boolean) evalStartsWith(ctx, visitor);
        } else {
            throw new UnexpectedSymbolException("Unexpected token near " + ctx.value.getText());
        }
        logger.debug("List: value={}, op={}, not={}, result={}", ctx.value.getText(), ctx.op.getText(), ctx.not != null, result);
        return result;
    }

    private Object evalIn(RuleFlowLanguageParser.ListContext ctx, Visitor visitor) throws PropertyNotFoundException {
        Object value = visitor.visit(ctx.value);

        if (ctx.values.literalList != null) {
            List<String> literals = ctx.values.string_literal().stream()
                .map(literal -> literal.getText().replace("'", ""))
                .toList();
            return literals.contains(value);
        } else if (ctx.values.storedList != null) {
            String listKey = ctx.values.string_literal(0).getText().replace("'", "");
            List<?> list = visitor.getLists().get(listKey);
            return list != null && list.contains(value.toString());
        } else if (ctx.values.validProperty() != null) {
            List<?> validPropertyList = (List<?>) new ValidPropertyContextEvaluator().evaluate(ctx.values.validProperty(), visitor);
            return validPropertyList.contains(value);
        } else {
            throw new RuntimeException("Cannot find symbol " + ctx.values);
        }
    }

    private Object evalContains(RuleFlowLanguageParser.ListContext ctx, Visitor visitor) {
        Object value = visitor.visit(ctx.value);

        if (ctx.values.literalList != null) {
            List<String> literals = ctx.values.string_literal().stream()
                .map(literal -> literal.getText().replace("'", ""))
                .collect(Collectors.toList());
            return literals.stream().anyMatch(literal -> value.toString().contains(literal));
        } else if (ctx.values.storedList != null) {
            String listKey = ctx.values.string_literal(0).getText().replace("'", "");
            List<?> list = visitor.getLists().get(listKey);
            return list != null && list.stream().anyMatch(item -> value.toString().contains((String) item));
        } else {
            throw new RuntimeException("Cannot find symbol");
        }
    }

    private Object evalStartsWith(RuleFlowLanguageParser.ListContext ctx, Visitor visitor)
        throws PropertyNotFoundException, UnexpectedSymbolException {
        String value = visitor.visit(ctx.value).toString();
        List<?> list;

        if (ctx.values.literalList != null) {
            list = ctx.values.string_literal().stream()
                .map(literal -> literal.getText().replace("'", ""))
                .collect(Collectors.toList());
        } else if (ctx.values.storedList != null) {
            String listKey = ctx.values.string_literal(0).getText().replace("'", "");
            list = visitor.getLists().get(listKey).stream().collect(Collectors.toList());
        } else if (ctx.values.validProperty() != null) {
            list = (List<?>) new ValidPropertyContextEvaluator().evaluate(ctx.values.validProperty(), visitor);
        } else {
            throw new UnexpectedSymbolException("Unexpected symbol " + ctx.values);
        }

        return list.stream()
            .anyMatch(elem -> value.startsWith(elem.toString()));
    }
}