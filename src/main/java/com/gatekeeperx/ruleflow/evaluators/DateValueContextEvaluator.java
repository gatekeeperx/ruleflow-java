package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.DateValueContext;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import com.gatekeeperx.ruleflow.utils.DateTimeUtils;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateValueContextEvaluator implements ContextEvaluator<DateValueContext> {
    private static final Logger logger = LoggerFactory.getLogger(DateValueContextEvaluator.class);
    public Object evaluate(RuleFlowLanguageParser.DateValueContext ctx, Visitor visitor) {
        if(ctx.string_literal() != null) {
            ZonedDateTime zonedDateTime = DateTimeUtils.toZonedDateTime(
                ctx.string_literal().getText().replace("'", ""));
            logger.debug("DateValue: {}", zonedDateTime);
            return zonedDateTime;
        } else if(ctx.validProperty() != null) {
            String value = (String) new ValidPropertyContextEvaluator().evaluate(
                ctx.validProperty(), visitor);
            ZonedDateTime zonedDateTime = DateTimeUtils.toZonedDateTime(value);
            logger.debug("DateValue: {}", zonedDateTime);
            return zonedDateTime;
        } else {
            throw new IllegalArgumentException("Date not supported: " + ctx.getText());
        }
    }
} 