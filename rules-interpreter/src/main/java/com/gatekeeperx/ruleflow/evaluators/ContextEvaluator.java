package com.gatekeeperx.ruleflow.evaluators;

import com.gatekeeperx.ruleflow.errors.PropertyNotFoundException;
import com.gatekeeperx.ruleflow.errors.UnexpectedSymbolException;
import com.gatekeeperx.ruleflow.visitors.Visitor;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Interface for context evaluators in the RuleFlow engine.
 * Implementations should provide logic for evaluating specific parse tree contexts.
 */
public interface ContextEvaluator<T extends ParserRuleContext> {
    Object evaluate(T ctx, Visitor visitor)
        throws PropertyNotFoundException, UnexpectedSymbolException;
}