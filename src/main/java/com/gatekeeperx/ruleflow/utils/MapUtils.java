package com.gatekeeperx.ruleflow.utils;

import java.util.Map;

public class MapUtils {

    public static Object getIgnoreCase(Map<String, ?> map, String key) {
        Object value = map.get(key);
        if (value != null) return value;
        if (map.containsKey(key)) return null;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static boolean containsKeyIgnoreCase(Map<String, ?> map, String key) {
        if (map.containsKey(key)) return true;
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(key)) return true;
        }
        return false;
    }
}
