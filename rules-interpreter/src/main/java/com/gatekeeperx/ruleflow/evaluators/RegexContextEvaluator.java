package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.RegexlikeContext;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class RegexContextEvaluator implements ContextEvaluator<RegexlikeContext> {
    private static final Logger logger = LoggerFactory.getLogger(RegexContextEvaluator.class);

    @Override
    public Object evaluate(RegexlikeContext ctx, Visitor visitor) {
        Object value = visitor.visit(ctx.value);

        String regexPattern = ctx.regex.getText().replace("'", "");
        String input = value.toString();

        Pattern pattern = Pattern.compile(regexPattern);
        String s = pattern.matcher(input).replaceAll("");
        logger.debug("Value: '{}' -> '{}'", value, s);
        return s;
    }
}