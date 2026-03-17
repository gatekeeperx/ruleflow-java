package com.gatekeeperx.ruleflow.functions;

import java.util.List;

@FunctionalInterface
public interface RuleflowFunction {
    Object apply(List<Object> args);
}
