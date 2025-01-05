package org.vinerdream.citPaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;

public class ItemUpdater {
    private final CITPaper plugin;

    public ItemUpdater(CITPaper plugin) {
        this.plugin = plugin;
    }

    public void updateItem(ItemStack item) {
        Component name = item.getItemMeta().displayName();
        updateItem(item, name != null ? PlainTextComponentSerializer.plainText().serialize(name) : null);
    }

    public void updateItem(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (data.getItems().stream().noneMatch(itemKey -> item.getType().getKey().asString().equals(itemKey))) {
                continue;
            }
            boolean matched = data.getNamePattern() == null || data.getNamePattern().matcher(name).find();
            if (data.getCustomModelData() != -1 && meta.getCustomModelData() != data.getCustomModelData()) {
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
}
