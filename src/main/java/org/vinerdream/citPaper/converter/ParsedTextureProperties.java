package org.vinerdream.citPaper.converter;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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

    private String popProperty(Properties properties, String key, String defaultValue) {
        String value = (String) properties.remove(key);
        return value != null ? value : defaultValue;
    }
}
