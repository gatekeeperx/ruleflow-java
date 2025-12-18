package com.gatekeeperx.ruleflow.visitors;

import com.gatekeeperx.ruleflow.RuleFlowLanguageBaseVisitor;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.PropertyTupleContext;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.evaluators.AggregationContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.BinaryAndContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.BinaryOrContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.ComparatorContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.DateDiffContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.DateOperationContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.DateParseExprContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.DateValueContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.DayOfWeekContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.EvalInListContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.GeoOperationContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.ListContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.MathAddContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.MathMulContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.ParenthesisContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.PropertyContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.PropertyTupleContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.RegexContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.TupleListContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.UnaryContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.ValidPropertyContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.ValueContextEvaluator;
import com.gatekeeperx.ruleflow.evaluators.StringDistanceContextEvaluator;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

public class Visitor extends RuleFlowLanguageBaseVisitor<Object> {
    private final Map<String, ?> data;
    private final Map<String, List<?>> lists;
    private final Map<String, ?> root;

    public Visitor(Map<String, ?> data, Map<String, List<?>> lists, Map<String, ?> root) {
        this.data = data;
        this.lists = lists != null ? lists : Map.of();
        this.root = root;
    }

    @Override
    public Object visit(ParseTree tree) {
        ParserRuleContext ctx = (ParserRuleContext) tree;

        try {
            if (ctx instanceof RuleFlowLanguageParser.ComparatorContext) {
                return new ComparatorContextEvaluator().evaluate((RuleFlowLanguageParser.ComparatorContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.AggregationContext) {
                return new AggregationContextEvaluator().evaluate((RuleFlowLanguageParser.AggregationContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.MathMulContext) {
                return new MathMulContextEvaluator().evaluate((RuleFlowLanguageParser.MathMulContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.MathAddContext) {
                return new MathAddContextEvaluator().evaluate((RuleFlowLanguageParser.MathAddContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.ParenthesisContext) {
                return new ParenthesisContextEvaluator().evaluate((RuleFlowLanguageParser.ParenthesisContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.ValueContext) {
                return new ValueContextEvaluator().evaluate((RuleFlowLanguageParser.ValueContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.PropertyContext) {
                return new PropertyContextEvaluator().evaluate((RuleFlowLanguageParser.PropertyContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.ValidPropertyContext) {
                return new ValidPropertyContextEvaluator().evaluate((RuleFlowLanguageParser.ValidPropertyContext) ctx, this);
            } else if (ctx instanceof PropertyTupleContext) {
                return new PropertyTupleContextEvaluator().evaluate((PropertyTupleContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.DateDiffContext) {
                return new DateDiffContextEvaluator().evaluate((RuleFlowLanguageParser.DateDiffContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.DateAddContext) {
                return new com.gatekeeperx.ruleflow.evaluators.DateAddContextEvaluator().evaluate((RuleFlowLanguageParser.DateAddContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.DateSubtractContext) {
                return new com.gatekeeperx.ruleflow.evaluators.DateSubtractContextEvaluator().evaluate((RuleFlowLanguageParser.DateSubtractContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.ListContext) {
                return new ListContextEvaluator().evaluate((RuleFlowLanguageParser.ListContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.TupleListContext) {
                return new TupleListContextEvaluator().evaluate((RuleFlowLanguageParser.TupleListContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.UnaryContext) {
                return new UnaryContextEvaluator().evaluate((RuleFlowLanguageParser.UnaryContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.BinaryAndContext) {
                return new BinaryAndContextEvaluator().evaluate((RuleFlowLanguageParser.BinaryAndContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.BinaryOrContext) {
                return new BinaryOrContextEvaluator().evaluate((RuleFlowLanguageParser.BinaryOrContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.DayOfWeekContext) {
                return new DayOfWeekContextEvaluator().evaluate((RuleFlowLanguageParser.DayOfWeekContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.RegexlikeContext) {
                return new RegexContextEvaluator().evaluate((RuleFlowLanguageParser.RegexlikeContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.DateValueContext) {
                return new DateValueContextEvaluator().evaluate((RuleFlowLanguageParser.DateValueContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.DateParseExprContext) {
                return new DateParseExprContextEvaluator().evaluate((RuleFlowLanguageParser.DateParseExprContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.DateOperationContext) {
                return new DateOperationContextEvaluator().evaluate((RuleFlowLanguageParser.DateOperationContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.NowContext) {
                return new com.gatekeeperx.ruleflow.evaluators.NowContextEvaluator().evaluate((RuleFlowLanguageParser.NowContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.StringDistanceContext) {
                return new StringDistanceContextEvaluator().evaluateStringDistance((RuleFlowLanguageParser.StringDistanceContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.PartialRatioContext) {
                return new StringDistanceContextEvaluator().evaluatePartialRatio((RuleFlowLanguageParser.PartialRatioContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.TokenSortRatioContext) {
                return new StringDistanceContextEvaluator().evaluateTokenSortRatio((RuleFlowLanguageParser.TokenSortRatioContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.TokenSetRatioContext) {
                return new StringDistanceContextEvaluator().evaluateTokenSetRatio((RuleFlowLanguageParser.TokenSetRatioContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.StringSimilarityScoreContext) {
                return new StringDistanceContextEvaluator().evaluateStringSimilarityScore((RuleFlowLanguageParser.StringSimilarityScoreContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.GeoOperationContext) {
                return new GeoOperationContextEvaluator().evaluate((RuleFlowLanguageParser.GeoOperationContext) ctx, this);
            } else if (ctx instanceof RuleFlowLanguageParser.EvalInListContext) {
                return new EvalInListContextEvaluator().evaluate((RuleFlowLanguageParser.EvalInListContext) ctx, this);
            } else {
                throw new IllegalArgumentException("Operation not supported: " + ctx.getClass());
            }
        } catch (PropertyNotFoundException | UnexpectedSymbolException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, ?> getData() {
        return data;
    }

    public Map<String, List<?>> getLists() {
        return lists;
    }

    public Map<String, ?> getRoot() {
        return root;
    }
}