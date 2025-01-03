package org.vinerdream.citPaper.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtils {
    public static Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> result = new HashMap<>();
        properties.forEach((key, value) -> result.put((String) key, (String) value));
        return result;
    }
}
