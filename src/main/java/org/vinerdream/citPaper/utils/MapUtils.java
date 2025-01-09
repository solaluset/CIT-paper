package org.vinerdream.citPaper.utils;

import java.util.HashMap;
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

    public static Map<String, String> mapToStringMap(Map<?, ?> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return result;
    }

    public static <K, V> Map<K, V> mergeMaps(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> result = new HashMap<>(map1);

        if (map2 != null) {
            result.putAll(map2);
        }

        return result;
    }
}
