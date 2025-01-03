package org.vinerdream.citPaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (data.getItems().stream().noneMatch(itemKey -> item.getType().getKey().asString().equals(itemKey))) {
                return;
            }
            if (!data.getNamePattern().matcher(name).find()) {
                return;
            }
            ItemMeta meta = item.getItemMeta();
            meta.setItemModel(data.getKey());
            item.setItemMeta(meta);
        }
    }
}
