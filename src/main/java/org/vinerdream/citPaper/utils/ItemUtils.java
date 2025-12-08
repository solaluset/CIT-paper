package org.vinerdream.citPaper.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vinerdream.citPaper.CITPaper;

import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class ItemUtils {
    private static final Registry<ItemType> registry = Bukkit.getRegistry(ItemType.class);
    public static final boolean ITEM_MODEL_EXISTS = ReflectionUtils.hasMethod(ItemMeta.class, "getItemModel");
    public static final boolean EQUIPPABLE_EXISTS = ReflectionUtils.hasMethod(ItemMeta.class, "getEquippable");

    public static @Nullable Material getMaterial(final String id) {
        final NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) {
            return null;
        }
        assert registry != null;
        final ItemType itemType = registry.get(key);
        if (itemType == null) {
            return null;
        }
        return itemType.createItemStack().getType();
    }

    public static @Nullable NamespacedKey getItemModel(final ItemMeta itemMeta) {
        if (!ITEM_MODEL_EXISTS) {
            return null;
        }
        return itemMeta.getItemModel();
    }

    public static boolean isHelmet(final String name) {
        return name.contains("helmet");
    }

    public static boolean isChestplate(final String name) {
        return name.contains("chestplate");
    }

    public static boolean isLeggings(final String name) {
        return name.contains("leggings");
    }

    public static boolean isBoots(final String name) {
        return name.contains("boots");
    }

    public static boolean isArmor(final String name) {
        return isHelmet(name) || isChestplate(name) || isLeggings(name) || isBoots(name);
    }

    public static boolean isElytra(final String name) {
        return name.contains("elytra");
    }

    public static ItemStack oraxenItemCopy(CITPaper plugin, ItemMeta meta, Material oldMaterial) {
        final String oraxenMaterial = getOraxenMaterial(plugin.getOraxenArmorType(), oldMaterial.getKey().getKey());
        if (oldMaterial.getKey().getKey().equals(oraxenMaterial)) {
            return null;
        }
        final ItemStack newItem = new ItemStack(Objects.requireNonNull(getMaterial(oraxenMaterial)));
        cleanModifiers(oldMaterial, newItem.getType(), meta);
        return newItem;
    }

    public static void cleanModifiers(final Material fromMaterial, final Material toMaterial, final ItemMeta meta) {
        var modifiers = meta.getAttributeModifiers();
        if (modifiers == null) {
            modifiers = fromMaterial.getDefaultAttributeModifiers();
        }
        modifiers.entries().forEach(entry -> {
            final var key = entry.getKey();
            final var value = entry.getValue();
            if (key == null || value == null) {
                return;
            }
            meta.removeAttributeModifier(key, value);
            if (toMaterial.getDefaultAttributeModifiers().containsValue(value)) {
                return;
            }
            meta.addAttributeModifier(key, value);
        });
    }

    public static @NotNull String getOraxenMaterial(final String oraxenArmorType, final @NotNull String item) {
        final String name;
        if (item.contains(":")) {
            name = item.split(":")[1];
        } else {
            name = item.toLowerCase(Locale.ROOT);
        }
        if (isHelmet(name)) {
            return oraxenArmorType + "_helmet";
        } else if (isChestplate(name)) {
            return oraxenArmorType + "_chestplate";
        } else if (isLeggings(name)) {
            return oraxenArmorType + "_leggings";
        } else if (isBoots(name)) {
            return oraxenArmorType + "_boots";
        }
        return name;
    }
}
