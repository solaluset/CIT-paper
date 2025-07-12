package org.vinerdream.citPaper.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class CollectionUtils {
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

    public static boolean allHaveAnySuffix(List<String> values, List<String> suffixes) {
        return values.stream().allMatch(item -> suffixes.stream().anyMatch(item::endsWith));
    }

    public static <T> Iterable<T> iterateStream(Stream<T> stream) {
        return stream::iterator;
    }

    public static String pathToString(Path path) {
        return path.toString().replace(File.separator, "/");
    }

    public static Path stringToPath(String string) {
        return Path.of(".", string.split("/")).normalize();
    }
}
