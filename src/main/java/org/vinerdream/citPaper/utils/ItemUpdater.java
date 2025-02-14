package org.vinerdream.citPaper.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import static org.vinerdream.citPaper.utils.CollectionUtils.mergeMaps;

public class ItemUpdater {
    private final CITPaper plugin;
    private final NamespacedKey originalDataKey;
    private final NamespacedKey originalModelKey;
    private final NamespacedKey originalArmorModelKey;

    private final static Method getCustomName;

    static {
        Method customName = null;
        try {
            customName = ItemMeta.class.getMethod("customName");
        } catch (NoSuchMethodException e) {
            try {
                customName = ItemMeta.class.getMethod("getDisplayName");
            } catch (NoSuchMethodException ignored) {}
        }
        getCustomName = customName;
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
        String title = getItemName(meta);
        if (title.isEmpty()) {
            title = meta.getTitle();
            if (title == null) return;
        }
        updateMeta(meta, Material.WRITTEN_BOOK, title, 0, null);
    }

    public void updateMeta(ItemMeta meta, Material type, String name, int damage, Map<Enchantment, Integer> enchantments) {
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (data.getItems().stream().noneMatch(itemKey -> type.getKey().toString().equals(itemKey))) {
                continue;
            }
            boolean matched = data.getNamePattern() == null || data.getNamePattern().matcher(name).find();
            if (data.getCustomModelData() != -1 && (!meta.hasCustomModelData() || meta.getCustomModelData() != data.getCustomModelData())) {
                matched = false;
            }
            if (data.getDamage() != null && meta instanceof Damageable damageable) {
                if (!data.getDamage().check(damageable.getDamage() + damage, type.getMaxDurability())) {
                    matched = false;
                }
            }
            if (data.getEnchantments() != null && !data.getEnchantments().check(mergeMaps(meta.getEnchants(), enchantments))) {
                matched = false;
            }
            if (!matched) continue;
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
            meta.setItemModel(data.getKey());
            if (data.getArmorData() != null) {
                setArmorTexture(meta, type.getKey().getKey(), NamespacedKey.fromString(data.getArmorData().getModel()));
            }
            return;
        }

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

    private static String getItemName(ItemMeta meta) {
        if (meta == null) {
            return "";
        }

        final Object result;
        try {
            result = getCustomName.invoke(meta);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        if (ReflectionUtils.isClassPresent("net.kyori.adventure.text.Component")) {
            String name = ComponentUtils.componentToString(result);
            if (name != null) return name;
        }
        if (result instanceof String str) {
            return str;
        } else if (meta instanceof BookMeta bookMeta) {
            return bookMeta.getTitle();
        } else {
            return "";
        }
    }
}
