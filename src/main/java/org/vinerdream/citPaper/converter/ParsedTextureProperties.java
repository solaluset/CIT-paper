package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.vinerdream.citPaper.utils.NameMatcher;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.vinerdream.citPaper.utils.MapUtils.popValue;

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
    private final String shieldBlockingModel;
    @Getter
    @Setter
    private String armorTexture;
    @Getter
    private final int armorTextureType;
    @Getter
    private final Pattern namePattern;
    private final String damage;
    @Getter
    @Setter
    private NamespacedKey key;

    public ParsedTextureProperties(Map<String, String> properties, Consumer<String> logger) {
        this.type = TextureType.valueOf(popValue(properties, "type", "item").toUpperCase());
        this.items = Arrays.stream(popValue(
                properties,
                "items",
                popValue(properties, "matchItems", "")
        ).split(" ")).map(item -> item.contains(":") ? item : "minecraft:" + item).toList();
        this.texture = popValue(properties, "texture", popValue(properties, "texture.elytra", null));
        this.model = popValue(properties, "model", popValue(properties, "model.bow_standby", null));
        this.namePattern = NameMatcher.filterToPattern(popValue(
                properties,
                "nbt.display.Name",
                popValue(properties, "name", null)
        ));
        this.damage = popValue(properties, "damage", null);
        if (properties.containsKey("key")) {
            this.key = NamespacedKey.fromString(popValue(properties, "key", null));
        }

        this.shieldBlockingModel = popValue(properties, "model.shield_blocking", null);

        String armorTexture = popValue(properties, "armorTexture", null);
        int armorTextureType = 0;
        for (Map.Entry<String, String> entry : properties.entrySet().stream().toList()) {
            if (entry.getKey().startsWith("texture.") && entry.getKey().contains("_layer_")) {
                String value = popValue(properties, entry.getKey(), null);
                if (armorTexture == null) {
                    armorTexture = value;
                    armorTextureType = Integer.parseInt(entry.getKey().split("_layer_")[1]);
                } else if (!armorTexture.equals(value)) {
                    logger.accept("Different armor textures not supported: " + armorTexture + " != " + value);
                }
            }
        }
        if (armorTexture == null && type == TextureType.ELYTRA) {
            this.armorTexture = texture;
            this.armorTextureType = 3;
        } else {
            this.armorTexture = armorTexture;
            this.armorTextureType = armorTextureType;
        }
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
        if (armorTexture != null) {
            result.put("armorTexture", armorTexture);
        }
        return result;
    }

    public boolean itemEquals(ParsedTextureProperties other) {
        return this.items.equals(other.items) && (this.namePattern == null ? other.namePattern == null : (other.namePattern != null && this.namePattern.pattern().equals(other.namePattern.pattern())));
    }
}
