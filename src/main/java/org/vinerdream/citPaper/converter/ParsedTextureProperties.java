package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.vinerdream.citPaper.utils.NameMatcher;

import java.util.*;
import java.util.regex.Pattern;

public class ParsedTextureProperties {
    @Getter
    private final TextureType type;
    @Getter
    private final List<String> items;
    @Getter
    private final String texture;
    @Getter
    private final String model;
    @Getter
    private final Pattern namePattern;
    private final String damage;
    @Getter
    @Setter
    private NamespacedKey key;

    public ParsedTextureProperties(Map<String, String> properties) {
        this.type = TextureType.valueOf(popProperty(properties, "type", "item").toUpperCase());
        this.items = Arrays.stream(popProperty(
                properties,
                "items",
                popProperty(properties, "matchItems", "")
        ).split(" ")).map(item -> item.contains(":") ? item : "minecraft:" + item).toList();
        this.texture = popProperty(properties, "texture", null);
        this.model = popProperty(properties, "model", null);
        this.namePattern = NameMatcher.filterToPattern(popProperty(
                properties,
                "nbt.display.Name",
                popProperty(properties, "name", null)
        ));
        this.damage = popProperty(properties, "damage", null);
        if (properties.containsKey("key")) {
            this.key = NamespacedKey.fromString(popProperty(properties, "key", null));
        }

        // TODO: apply the model somehow?
        popProperty(properties, "model.shield_blocking", null);
    }

    public Map<String, String> asMap() {
        Map<String, String> result = new HashMap<>();
        result.put("items", String.join(" ", items));
        if (namePattern != null) {
            String pattern = "regex:" + namePattern.pattern();
            if ((namePattern.flags() & Pattern.CASE_INSENSITIVE) != 0) {
                pattern = "i" + pattern;
            }
            result.put("name", pattern);
        }
        if (key != null) {
            result.put("key", key.asString());
        }
        return result;
    }

    private String popProperty(Map<String, String> properties, String key, String defaultValue) {
        String value = properties.remove(key);
        return value != null ? value : defaultValue;
    }
}
