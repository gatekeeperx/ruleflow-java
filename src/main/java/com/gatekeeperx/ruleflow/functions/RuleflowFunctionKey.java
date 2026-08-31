package com.gatekeeperx.ruleflow.functions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Produces a deterministic, serializable identity for a resolved custom-function
 * call. The key is stable across processes and time for the same
 * (functionName, resolvedArgs), so a pre-resolved result computed asynchronously
 * can be looked up at evaluation time.
 */
public final class RuleflowFunctionKey {

    private RuleflowFunctionKey() {}

    public static String of(String functionName, Map<String, Object> resolvedArgs) {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append('(');
        canonicalize(resolvedArgs, sb);
        sb.append(')');
        return functionName + ":" + sha256(sb.toString());
    }

    @SuppressWarnings("unchecked")
    private static void canonicalize(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            ((Map<Object, Object>) value).forEach((k, v) -> sorted.put(String.valueOf(k), v));
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : sorted.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(e.getKey()).append('=');
                canonicalize(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof List) {
            sb.append('[');
            boolean first = true;
            for (Object item : (List<Object>) value) {
                if (!first) sb.append(',');
                first = false;
                canonicalize(item, sb);
            }
            sb.append(']');
        } else {
            // Include the type so 5 (Integer) and "5" (String) never collide.
            sb.append(value.getClass().getSimpleName()).append(':').append(value);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
