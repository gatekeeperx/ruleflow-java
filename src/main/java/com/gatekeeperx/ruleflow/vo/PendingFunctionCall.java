package com.gatekeeperx.ruleflow.vo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A custom-function call site with its arguments already resolved against the payload. */
public class PendingFunctionCall {

    private final String functionName;
    private final Map<String, Object> args;
    private final String key;

    public PendingFunctionCall(String functionName, Map<String, Object> args, String key) {
        this.functionName = functionName;
        this.args = args != null ? new LinkedHashMap<>(args) : new LinkedHashMap<>();
        this.key = key;
    }

    public String getFunctionName() { return functionName; }

    public Map<String, Object> getArgs() { return Collections.unmodifiableMap(args); }

    public String getKey() { return key; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PendingFunctionCall that)) return false;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() { return Objects.hash(key); }

    @Override
    public String toString() {
        return "PendingFunctionCall{functionName='" + functionName + "', args=" + args + ", key='" + key + "'}";
    }
}
