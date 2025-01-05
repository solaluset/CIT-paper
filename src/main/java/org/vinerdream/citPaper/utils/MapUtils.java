package org.vinerdream.citPaper.utils;

import java.util.Map;

public class MapUtils {
    public static String popValue(Map<String, String> map, String key, String defaultValue) {
        String value = map.remove(key);
        return value != null ? value : defaultValue;
    }
}
