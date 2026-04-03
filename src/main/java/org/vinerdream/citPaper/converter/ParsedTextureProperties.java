package org.vinerdream.citPaper.converter;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vinerdream.citPaper.exceptions.UnsupportedCitTypeException;
import org.vinerdream.citPaper.utils.NameMatcher;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.vinerdream.citPaper.utils.CollectionUtils.popValue;

@Getter
public class ParsedTextureProperties {
    private final static String LORE_PREFIX = "nbt.display.Lore.";

    private final TextureType type;
    private final List<String> items;
    private final @NotNull TextureData mainTextureData;
    private final ElytraTextureData elytraTextureData;
    private final ShieldTextureData shieldTextureData;
    private final @NotNull Map<Integer, TextureData> armorData = new HashMap<>();
    @Setter
    private @Nullable String armorModel;
    private final BowTextureData bowTextureData;
    private final CrossbowTextureData crossbowTextureData;
    private final TridentTextureData tridentTextureData;
    private final FishingRodTextureData fishingRodTextureData;
    private final Pattern namePattern;
    private final DamageData damage;
    private final EnchantmentsData enchantments;
    private final String potion;
    private final int weight;
    private final int customModelData;
    private final Map<Integer, Pattern> loreData = new HashMap<>();
    @Setter
    private NamespacedKey key;
    private final @Nullable OraxenData oraxenData;

    public ParsedTextureProperties(Map<String, String> properties, Consumer<String> logger) throws UnsupportedCitTypeException {
        final String typeString = popValue(properties, "item", "type");
        try {
            this.type = TextureType.valueOf(typeString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UnsupportedCitTypeException("Unsupported CIT type: " + typeString);
        }
        this.oraxenData = OraxenData.fromMap(properties);
        this.items = Arrays.stream(popValue(
                properties,
                "",
                "matchItems",
                "items"
        ).split(" ")).map(item -> item.contains(":") ? item : "minecraft:" + item).toList();
        TextureData mainTextureData = TextureData.fromMap(properties, null);
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
        this.enchantments = EnchantmentsData.fromMap(properties);
        this.potion = popValue(
                properties,
                null,
                "nbt.Potion",
                "components.potion_contents.potion",
                "components.minecraft:potion_contents.potion"
        );
        this.customModelData = Integer.parseInt(popValue(
                properties,
                "-1",
                "customModelData",
                "nbt.CustomModelData",
                "components.custom_model_data",
                "components.minecraft:custom_model_data"
        ));
        if (properties.containsKey("key")) {
            this.key = NamespacedKey.fromString(popValue(properties, null, "key"));
        }

        this.weight = Integer.parseInt(popValue(properties, "0", "weight"));

        this.shieldTextureData = ShieldTextureData.fromMap(properties, mainTextureData);

        String itemTexture = null;
        String itemOverlay = null;
        // iterate over a COPY of key set
        // because we modify the map in the loop
        for (String key : new ArrayList<>(properties.keySet())) {
            if (key.startsWith("texture.") && key.contains("_layer_")) {
                final int type = Integer.parseInt(key.split("_layer_", 2)[1].split("_", 2)[0]);
                final String value = popValue(properties, null, key);

                if (!armorData.containsKey(type)) {
                    armorData.put(type, new TextureData(null, null));
                }

                if (key.endsWith("_overlay")) {
                    armorData.get(type).setOverlay(value);
                    continue;
                }
                armorData.get(type).setTexture(value);

            } else if (key.startsWith("texture.leather_")) {
                if (key.endsWith("_overlay")) {
                    itemOverlay = popValue(properties, null, key);
                } else {
                    itemTexture = popValue(properties, null, key);
                }
            } else if (key.startsWith(LORE_PREFIX)) {
                final String lineNumber = key.replace(LORE_PREFIX, "");
                final String value = popValue(properties, null, key);
                this.loreData.put(
                        lineNumber.equals("*") ? null : Integer.parseInt(lineNumber),
                        NameMatcher.filterToPattern(value, logger)
                );
            }
        }
        this.armorModel = popValue(properties, null, "armorModel");
        if (mainTextureData == null) {
            mainTextureData = new TextureData(null, itemTexture, itemOverlay);
            if (mainTextureData.isEmpty()) {
                mainTextureData = null;
            }
        } else {
            if (mainTextureData.getTexture() == null) {
                mainTextureData.setTexture(itemTexture);
            }
            if (mainTextureData.getOverlay() == null) {
                mainTextureData.setOverlay(itemOverlay);
            }
        }

        this.elytraTextureData = ElytraTextureData.fromMap(properties, mainTextureData);
        if (mainTextureData == null) {
            mainTextureData = elytraTextureData;
        }
        this.bowTextureData = BowTextureData.fromMap(properties, mainTextureData);
        if (mainTextureData == null) {
            mainTextureData = bowTextureData;
        }
        this.crossbowTextureData = CrossbowTextureData.fromMap(properties, mainTextureData);
        if (mainTextureData == null) {
            mainTextureData = crossbowTextureData;
        }
        this.tridentTextureData = TridentTextureData.fromMap(properties, mainTextureData);
        if (mainTextureData == null) {
            mainTextureData = tridentTextureData;
        }
        this.fishingRodTextureData = FishingRodTextureData.fromMap(properties, mainTextureData);
        if (mainTextureData == null) {
            mainTextureData = fishingRodTextureData;
        }

        String model = null;
        String texture = null;
        String overlay = null;
        if (mainTextureData != null) {
            model = mainTextureData.getModel();
            texture = mainTextureData.getTexture();
            overlay = mainTextureData.getOverlay();
        }
        for (String key : new ArrayList<>(properties.keySet())) {
            if (key.startsWith("model.") && model == null) {
                model = popValue(properties, null, key);
                continue;
            }
            if (key.startsWith("texture.") && key.endsWith("_overlay") && overlay == null) {
                overlay = popValue(properties, null, key);
                continue;
            }
            if (key.startsWith("texture.") && texture == null) {
                texture = popValue(properties, null, key);
                continue;
            }
            logger.accept("Unknown property: " + key);
        }
        this.mainTextureData = new TextureData(model, texture, overlay);
        if (type == TextureType.ELYTRA && !armorData.containsKey(3) && texture != null) {
            armorData.put(3, new TextureData(null, texture, overlay));
        }
    }

    public Map<String, String> saveToMap() {
        Map<String, String> result = new HashMap<>();
        result.put("type", type.toString());
        result.put("items", String.join(" ", items));
        if (namePattern != null) {
            result.put("name", NameMatcher.patternToFilter(namePattern));
        }
        if (key != null) {
            result.put("key", key.toString());
        }
        if (damage != null) {
            damage.toMap(result);
        }
        if (enchantments != null) {
            enchantments.toMap(result);
        }
        if (potion != null) {
            result.put("nbt.Potion", potion);
        }
        if (armorModel != null) {
            result.put("armorModel", armorModel);
        }
        if (customModelData != -1) {
            result.put("customModelData", String.valueOf(customModelData));
        }
        for (var entry : loreData.entrySet()) {
            result.put(
                    LORE_PREFIX + (entry.getKey() == null ? "*" : entry.getKey()),
                    NameMatcher.patternToFilter(entry.getValue())
            );
        }
        if (weight != 0) {
            result.put("weight", String.valueOf(weight));
        }
        if (oraxenData != null) {
            oraxenData.toMap(result);
        }
        return result;
    }
}
