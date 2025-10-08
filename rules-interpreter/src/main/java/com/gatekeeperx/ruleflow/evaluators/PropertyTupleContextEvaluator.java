package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.PropertyTupleContext;
import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.ValidPropertyContext;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyTupleContextEvaluator implements ContextEvaluator<PropertyTupleContext> {
    private static final Logger logger = LoggerFactory.getLogger(PropertyTupleContextEvaluator.class);

  @Override
  public Object evaluate(PropertyTupleContext ctx, Visitor visitor)
      throws PropertyNotFoundException {
    List<String> propertyValues = new ArrayList<>();
    for (ValidPropertyContext validPropertContext : ctx.validProperty()){
      String visit = (String) visitor.visit(validPropertContext);
      logger.debug("PropertyTuple: propertyName={}", visit);
      propertyValues.add(visit);
    }
    return propertyValues;
  }
}
