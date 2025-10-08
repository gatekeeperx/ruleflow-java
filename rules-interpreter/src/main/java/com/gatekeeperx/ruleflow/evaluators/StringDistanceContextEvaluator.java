package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import com.gatekeeperx.ruleflow.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringDistanceContextEvaluator {
    private static final Logger log = LoggerFactory.getLogger(StringDistanceContextEvaluator.class);

    public Object evaluateStringDistance(RuleFlowLanguageParser.StringDistanceContext ctx, Visitor visitor) {
        Object left = visitor.visit(ctx.left);
        Object right = visitor.visit(ctx.right);
        Object result = StringUtils.stringDistance(left == null ? null : left.toString(), right == null ? null : right.toString());
        log.debug("evaluateStringDistance: left={}, right={}, result={}", left, right, result);
        return result;
    }
    public Object evaluatePartialRatio(RuleFlowLanguageParser.PartialRatioContext ctx, Visitor visitor) {
        Object left = visitor.visit(ctx.left);
        Object right = visitor.visit(ctx.right);
        Object result = StringUtils.partialRatio(left == null ? null : left.toString(), right == null ? null : right.toString());
        log.debug("evaluatePartialRatio: left={}, right={}, result={}", left, right, result);
        return result;
    }
    public Object evaluateTokenSortRatio(RuleFlowLanguageParser.TokenSortRatioContext ctx, Visitor visitor) {
        Object left = visitor.visit(ctx.left);
        Object right = visitor.visit(ctx.right);
        Object result = StringUtils.tokenSortRatio(left == null ? null : left.toString(), right == null ? null : right.toString());
        log.debug("evaluateTokenSortRatio: left={}, right={}, result={}", left, right, result);
        return result;
    }
    public Object evaluateTokenSetRatio(RuleFlowLanguageParser.TokenSetRatioContext ctx, Visitor visitor) {
        Object left = visitor.visit(ctx.left);
        Object right = visitor.visit(ctx.right);
        Object result = StringUtils.tokenSetRatio(left == null ? null : left.toString(), right == null ? null : right.toString());
        log.debug("evaluateTokenSetRatio: left={}, right={}, result={}", left, right, result);
        return result;
    }
    public Object evaluateStringSimilarityScore(RuleFlowLanguageParser.StringSimilarityScoreContext ctx, Visitor visitor) {
        Object left = visitor.visit(ctx.left);
        Object right = visitor.visit(ctx.right);
        Object result = StringUtils.stringSimilarityScore(left == null ? null : left.toString(), right == null ? null : right.toString());
        log.debug("evaluateStringSimilarityScore: left={}, right={}, result={}", left, right, result);
        return result;
    }
}