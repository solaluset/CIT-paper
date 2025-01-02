package org.vinerdream.citPaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUpdater {
    public static void updateItem(ItemStack item) {
        Component name = item.getItemMeta().displayName();
        updateItem(item, name != null ? PlainTextComponentSerializer.plainText().serialize(name) : null);
    }

    public static void updateItem(ItemStack item, String name) {
        if (item.getType() != Material.NETHERITE_SWORD) return;

        if (!"Dark Dream".equals(name)) return;

        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("orion", "dark_dream"));
        item.setItemMeta(meta);
    }
}
