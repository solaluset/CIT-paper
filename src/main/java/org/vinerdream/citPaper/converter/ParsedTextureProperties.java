package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.stream.Collectors;

public class ParsedTextureProperties {
    @Getter
    private final TextureType type;
    private final List<String> items;
    @Getter
    private final String texture;
    @Getter
    private final String model;
    private final String nameFilter;
    private final String damage;
    @Getter
    @Setter
    private NamespacedKey key;

    public ParsedTextureProperties(Properties properties) {
        this.type = TextureType.valueOf(popProperty(properties, "type", "item").toUpperCase());
        this.items = Arrays.stream(popProperty(
                properties,
                "items",
                popProperty(properties, "matchItems", "")
        ).split(" ")).toList();
        this.texture = popProperty(properties, "texture", null);
        this.model = popProperty(properties, "model", null);
        this.nameFilter = popProperty(properties, "nbt.display.Name", null);
        this.damage = popProperty(properties, "damage", null);
    }

    public Map<String, String> asMap() {
        Map<String, String> result = new HashMap<>();
        result.put("items", String.join(" ", items));
        result.put("name", nameFilter);
        if (key != null) {
            result.put("key", key.asString());
        }
        return result;
    }

    private String popProperty(Properties properties, String key, String defaultValue) {
        String value = (String) properties.remove(key);
        return value != null ? value : defaultValue;
    }
}
