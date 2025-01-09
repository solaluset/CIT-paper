package org.vinerdream.citPaper.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import static org.vinerdream.citPaper.utils.MapUtils.mergeMaps;

public class ItemUpdater {
    private final CITPaper plugin;

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
        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (data.getItems().stream().noneMatch(itemKey -> item.getType().getKey().toString().equals(itemKey))) {
                continue;
            }
            boolean matched = data.getNamePattern() == null || data.getNamePattern().matcher(name).find();
            if (data.getCustomModelData() != -1 && (!meta.hasCustomModelData() || meta.getCustomModelData() != data.getCustomModelData())) {
                matched = false;
            }
            if (data.getDamage() != null && meta instanceof Damageable damageable) {
                if (!data.getDamage().check(damageable.getDamage() + damage, item.getType().getMaxDurability())) {
                    matched = false;
                }
            }
            if (data.getEnchantments() != null && !data.getEnchantments().check(mergeMaps(meta.getEnchants(), enchantments))) {
                matched = false;
            }
            if (!matched) continue;
            meta.setItemModel(data.getKey());
            if (data.getArmorData() != null) {
                setArmorTexture(meta, item.getType().getKey().getKey(), NamespacedKey.fromString(data.getArmorData().getModel()));
            }
            meta.getPersistentDataContainer().set(plugin.getIsManagedKey(), PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
            return;
        }

        if (meta.getPersistentDataContainer().has(plugin.getIsManagedKey())) {
            meta.setItemModel(null);
            setArmorTexture(meta, null, null);
            meta.getPersistentDataContainer().remove(plugin.getIsManagedKey());
            item.setItemMeta(meta);
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
        ItemMeta meta = item.getItemMeta();
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
        } else {
            return "";
        }
    }
}
