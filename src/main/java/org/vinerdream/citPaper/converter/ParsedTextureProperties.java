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
    @Setter
    private TextureData mainTextureData;
    @Getter
    private final TextureData elytraTextureData;
    @Getter
    private final TextureData shieldBlockingData;
    @Getter
    @Setter
    private TextureData armorData;
    @Getter
    private final int armorDataType;
    @Getter
    private final BowTextureData bowTextureData;
    @Getter
    private final CrossbowTextureData crossbowTextureData;
    @Getter
    private final Pattern namePattern;
    @Getter
    private final DamageData damage;
    @Getter
    private final int weight;
    @Getter
    private final int customModelData;
    @Getter
    @Setter
    private NamespacedKey key;

    public ParsedTextureProperties(Map<String, String> properties, Consumer<String> logger) {
        this.type = TextureType.valueOf(popValue(properties, "item", "type").toUpperCase());
        this.items = Arrays.stream(popValue(
                properties,
                "",
                "matchItems",
                "items"
        ).split(" ")).map(item -> item.contains(":") ? item : "minecraft:" + item).toList();
        TextureData mainTextureData = TextureData.fromMap(properties, null);
        this.elytraTextureData = TextureData.fromMap(properties, "elytra");
        this.namePattern = NameMatcher.filterToPattern(popValue(
                properties,
                null,
                "name",
                "nbt.display.Name",
                "components.custom_name",
                "components.minecraft:custom_name",
                "nbt.title"
        ), logger);
        this.damage = DamageData.fromMap(properties);
        this.customModelData = Integer.parseInt(popValue(
                properties,
                "-1",
                "customModelData",
                "nbt.CustomModelData"
        ));
        if (properties.containsKey("key")) {
            this.key = NamespacedKey.fromString(popValue(properties, null, "key"));
        }

        this.weight = Integer.parseInt(popValue(properties, "0", "weight"));

        this.shieldBlockingData = TextureData.fromMap(properties, "shield_blocking");

        String armorTexture = null;
        int armorTextureType = 0;
        for (Map.Entry<String, String> entry : properties.entrySet().stream().toList()) {
            if (entry.getKey().startsWith("texture.") && entry.getKey().contains("_layer_")) {
                String value = popValue(properties, null, entry.getKey());
                if (armorTexture == null) {
                    armorTexture = value;
                    armorTextureType = Integer.parseInt(entry.getKey().split("_layer_")[1]);
                } else if (!armorTexture.equals(value)) {
                    logger.accept("Different armor textures not supported: " + armorTexture + " != " + value);
                }
            }
        }
        if (armorTexture == null && type == TextureType.ELYTRA) {
            armorTexture = mainTextureData != null ? mainTextureData.getTexture() : null;
            this.armorDataType = 3;
        } else {
            this.armorDataType = armorTextureType;
        }
        this.armorData = new TextureData(popValue(properties, null, "armorModel"), armorTexture);
        if (this.armorData.isEmpty()) {
            this.armorData = null;
        }

        this.bowTextureData = BowTextureData.fromMap(properties, mainTextureData);
        if (mainTextureData == null) {
            mainTextureData = bowTextureData;
        }
        this.crossbowTextureData = CrossbowTextureData.fromMap(properties, mainTextureData);
        if (mainTextureData == null) {
            mainTextureData = crossbowTextureData;
        }
        this.mainTextureData = mainTextureData;
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
        if (damage != null) {
            damage.toMap(result);
        }
        if (armorData != null) {
            result.put("armorModel", armorData.getModel());
        }
        if (bowTextureData != null) {
            result.put("model.bow_standby", bowTextureData.getModel());
            result.put("texture.bow_standby", bowTextureData.getTexture());
            if (bowTextureData.getPulling_0() != null) {
                result.put("model.bow_pulling_0", bowTextureData.getPulling_0().getModel());
                result.put("texture.bow_pulling_0", bowTextureData.getPulling_0().getTexture());
            }
            if (bowTextureData.getPulling_1() != null) {
                result.put("model.bow_pulling_1", bowTextureData.getPulling_1().getModel());
                result.put("texture.bow_pulling_1", bowTextureData.getPulling_1().getTexture());
            }
            if (bowTextureData.getPulling_2() != null) {
                result.put("model.bow_pulling_2", bowTextureData.getPulling_2().getModel());
                result.put("texture.bow_pulling_2", bowTextureData.getPulling_2().getTexture());
            }
        }
        if (crossbowTextureData != null) {
            result.put("model.crossbow_standby", crossbowTextureData.getModel());
            result.put("texture.crossbow_standby", crossbowTextureData.getTexture());
            if (crossbowTextureData.getPulling_0() != null) {
                result.put("model.crossbow_pulling_0", crossbowTextureData.getPulling_0().getModel());
                result.put("texture.crossbow_pulling_0", crossbowTextureData.getPulling_0().getTexture());
            }
            if (crossbowTextureData.getPulling_1() != null) {
                result.put("model.crossbow_pulling_1", crossbowTextureData.getPulling_1().getModel());
                result.put("texture.crossbow_pulling_1", crossbowTextureData.getPulling_1().getTexture());
            }
            if (crossbowTextureData.getPulling_2() != null) {
                result.put("model.crossbow_pulling_2", crossbowTextureData.getPulling_2().getModel());
                result.put("texture.crossbow_pulling_2", crossbowTextureData.getPulling_2().getTexture());
            }
            if (crossbowTextureData.getWithArrow() != null) {
                result.put("model.crossbow_arrow", crossbowTextureData.getWithArrow().getModel());
                result.put("texture.crossbow_arrow", crossbowTextureData.getWithArrow().getTexture());
            }
            if (crossbowTextureData.getWithFirework() != null) {
                result.put("model.crossbow_firework", crossbowTextureData.getWithFirework().getModel());
                result.put("texture.crossbow_firework", crossbowTextureData.getWithFirework().getTexture());
            }
        }
        if (customModelData != -1) {
            result.put("customModelData", String.valueOf(customModelData));
        }
        if (weight != 0) {
            result.put("weight", String.valueOf(weight));
        }
        return result;
    }

    public boolean itemEquals(ParsedTextureProperties other, Consumer<String> logger) {
        boolean almostEquals = (this.namePattern == null ? other.namePattern == null : (other.namePattern != null && this.namePattern.pattern().equals(other.namePattern.pattern())))
                && this.customModelData == other.customModelData;
        if (!almostEquals) return false;
        if (this.items.equals(other.items)) {
            return true;
        }
        if (this.items.stream().anyMatch(other.items::contains)) {
            logger.accept("Potential item conflict in " + this.namePattern);
        }
        return false;
    }

    public boolean hasAnyData() {
        return mainTextureData != null || elytraTextureData != null || shieldBlockingData != null
                || armorData != null || bowTextureData != null || crossbowTextureData != null;
    }
}
