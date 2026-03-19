package com.gatekeeperx.ruleflow.functions;

import java.util.Map;

@FunctionalInterface
public interface RuleflowFunction {
    Object apply(Map<String, Object> args);
}
