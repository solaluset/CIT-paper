package org.vinerdream.citPaper.utils;

import java.util.Map;

public class MapUtils {
    public static String popValue(Map<String, String> map, String defaultValue, String ...keys) {
        String result = null;
        for (int i = keys.length - 1; i >= 0; i--) {
            String value = map.remove(keys[i]);
            if (value != null) {
                result = value;
            }
        }
        return result != null ? result : defaultValue;
    }
}
