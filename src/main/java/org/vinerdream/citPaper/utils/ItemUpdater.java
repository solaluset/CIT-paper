package org.vinerdream.citPaper.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;
import org.vinerdream.citPaper.converter.TextureType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.vinerdream.citPaper.utils.CollectionUtils.mergeMaps;

public class ItemUpdater {
    private final CITPaper plugin;
    private final NamespacedKey originalDataKey;
    private final NamespacedKey originalModelKey;
    private final NamespacedKey originalArmorModelKey;

    private final static Method getCustomName;
    private final static Method getLore;

    static {
        Method method = null;

        try {
            method = ItemMeta.class.getMethod("customName");
        } catch (NoSuchMethodException e) {
            try {
                method = ItemMeta.class.getMethod("getDisplayName");
            } catch (NoSuchMethodException ignored) {}
        }
        getCustomName = method;

        try {
            method = ItemMeta.class.getMethod("lore");
        } catch (NoSuchMethodException e) {
            try {
                method = ItemMeta.class.getMethod("getLore");
            } catch (NoSuchMethodException ignored) {}
        }
        getLore = method;
    }

    public ItemUpdater(CITPaper plugin) {
        this.plugin = plugin;
        this.originalDataKey = new NamespacedKey(plugin, "original-data");
        this.originalModelKey = new NamespacedKey(plugin, "model");
        this.originalArmorModelKey = new NamespacedKey(plugin, "armor-model");
    }

    public void updateItem(ItemStack item, int damage, Map<Enchantment, Integer> enchantments) {
        updateItem(item, getItemName(item), damage, enchantments);
    }

    public void updateItem(ItemStack item) {
        updateItem(item, 0, null);
    }

    public void updateItem(ItemStack item, Map<Enchantment, Integer> enchantments) {
        updateItem(item, 0, enchantments);
    }

    public void updateItem(ItemStack item, int damage) {
        updateItem(item, damage, null);
    }

    public void updateItem(ItemStack item, String name) {
        updateItem(item, name, 0, null);
    }

    public void updateItem(ItemStack item, String name, int damage, Map<Enchantment, Integer> enchantments) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        updateMeta(meta, item.getType(), name, damage, enchantments);
        item.setItemMeta(meta);
    }

    public void updateMeta(BookMeta meta) {
        updateMeta(meta, Material.WRITTEN_BOOK, getItemName(meta), 0, null);
    }

    public void updateMeta(ItemMeta meta, Material type, String name, int damage, Map<Enchantment, Integer> enchantments) {
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final HashSet<TextureType> applied = new HashSet<>();
        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (applied.contains(data.getType())) continue;

            if (data.getItems().stream().noneMatch(itemKey -> type.getKey().toString().equals(itemKey))) {
                continue;
            }
            if (data.getNamePattern() != null && !data.getNamePattern().matcher(name).find()) {
                continue;
            }
            final List<String> lore = getItemLore(meta);
            if (!data.getLoreData().entrySet().stream().allMatch(entry -> {
                if (entry.getKey() == null) {
                    return lore.stream().anyMatch(line -> entry.getValue().matcher(line).find());
                }
                return lore.size() > entry.getKey() && entry.getValue().matcher(lore.get(entry.getKey())).find();
            })) {
                continue;
            }
            if (data.getCustomModelData() != -1 && (!meta.hasCustomModelData() || meta.getCustomModelData() != data.getCustomModelData())) {
                continue;
            }
            if (data.getDamage() != null && meta instanceof Damageable damageable) {
                if (!data.getDamage().check(damageable.getDamage() + damage, type.getMaxDurability())) {
                    continue;
                }
            }
            if (data.getEnchantments() != null && !data.getEnchantments().check(mergeMaps(meta.getEnchants(), enchantments))) {
                continue;
            }
            if (data.getPotion() != null) {
                if (!(meta instanceof PotionMeta potionMeta)) {
                    continue;
                } else {
                    final PotionType potionType = potionMeta.getBasePotionType();
                    if (potionType == null || !data.getPotion().equals(potionType.getKey().toString())) {
                        continue;
                    }
                }
            }

            if (!pdc.has(originalDataKey)) {
                PersistentDataContainer container = pdc.getAdapterContext().newPersistentDataContainer();
                if (meta.getItemModel() != null) {
                    container.set(originalModelKey, PersistentDataType.STRING, meta.getItemModel().toString());
                }
                if (meta.getEquippable().getModel() != null) {
                    container.set(originalArmorModelKey, PersistentDataType.STRING, meta.getEquippable().getModel().toString());
                }
                pdc.set(originalDataKey, PersistentDataType.TAG_CONTAINER, container);
            }
            if (data.getType() == TextureType.ITEM) {
                meta.setItemModel(data.getKey());
            }
            if (data.getArmorData() != null) {
                setArmorTexture(meta, type.getKey().getKey(), NamespacedKey.fromString(data.getArmorData().getModel()));
            }
            applied.add(data.getType());
        }

        if (!applied.isEmpty()) return;

        if (pdc.has(originalDataKey)) {
            PersistentDataContainer container = pdc.get(originalDataKey, PersistentDataType.TAG_CONTAINER);
            assert container != null;
            if (container.has(originalModelKey)) {
                meta.setItemModel(NamespacedKey.fromString(Objects.requireNonNull(container.get(originalModelKey, PersistentDataType.STRING))));
            } else {
                meta.setItemModel(null);
            }
            if (container.has(originalArmorModelKey)) {
                setArmorTexture(
                        meta,
                        type.getKey().getKey(),
                        NamespacedKey.fromString(Objects.requireNonNull(container.get(originalArmorModelKey, PersistentDataType.STRING)))
                );
            } else {
                setArmorTexture(meta, null, null);
            }
            pdc.remove(originalDataKey);
        }
    }

    private void setArmorTexture(ItemMeta meta, String itemName, NamespacedKey texture) {
        if (texture == null || itemName == null) {
            meta.setEquippable(null);
            return;
        }
        EquippableComponent equippable = meta.getEquippable();
        if (itemName.contains("helmet")) {
            equippable.setSlot(EquipmentSlot.HEAD);
        } else if (itemName.contains("chestplate") || itemName.contains("elytra")) {
            equippable.setSlot(EquipmentSlot.CHEST);
        } else if (itemName.contains("leggings")) {
            equippable.setSlot(EquipmentSlot.LEGS);
        } else if (itemName.contains("boots")) {
            equippable.setSlot(EquipmentSlot.FEET);
        } else return;
        equippable.setModel(texture);
        meta.setEquippable(equippable);
    }

    private static String getItemName(ItemStack item) {
        if (item == null) return "";
        return getItemName(item.getItemMeta());
    }

    private static @NotNull String getItemName(ItemMeta meta) {
        if (meta == null) {
            return "";
        }

        final Object result;
        try {
            result = getCustomName.invoke(meta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        if (result == null) {
            if (meta instanceof BookMeta bookMeta) {
                final String title = bookMeta.getTitle();
                if (title != null) {
                    return title;
                }
            }
            return "";
        }

        if (ReflectionUtils.isComponentClassPresent()) {
            return ComponentUtils.componentToString(result);
        }

        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private static @NotNull List<String> getItemLore(ItemMeta meta) {
        if (meta == null) {
            return List.of();
        }

        final Object result;
        try {
            result = getLore.invoke(meta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        if (result == null) return List.of();

        final List<Object> loreList = (List<Object>) result;

        if (ReflectionUtils.isComponentClassPresent()) {
            return loreList.stream().map(ComponentUtils::componentToString).map(s -> s == null ? "" : s).toList();
        }

        return loreList.stream().map(o -> o == null ? "" : o).map(Object::toString).toList();
    }
}
