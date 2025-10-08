package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.RuleFlowLanguageParser.DateOperationContext;
import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;

public class DateOperationContextEvaluator implements ContextEvaluator<com.gatekeeperx.ruleflow.RuleFlowLanguageParser.DateOperationContext> {

  @Override
  public Object evaluate(DateOperationContext ctx, Visitor visitor)
      throws PropertyNotFoundException, UnexpectedSymbolException {
     if (ctx.dateExpr() != null) {
         return visitor.visit(ctx.dateExpr());
     } else {
         throw new UnexpectedSymbolException("Unexpected symbol " + ctx);
     }
  }
}
