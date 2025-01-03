package org.vinerdream.citPaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;

public class ItemUpdater {
    private final CITPaper plugin;
    private final NamespacedKey isManagedKey;

    public ItemUpdater(CITPaper plugin) {
        this.plugin = plugin;
        this.isManagedKey = new NamespacedKey(plugin, "is_managed");
    }

    public void updateItem(ItemStack item) {
        Component name = item.getItemMeta().displayName();
        updateItem(item, name != null ? PlainTextComponentSerializer.plainText().serialize(name) : null);
    }

    public void updateItem(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (data.getNamePattern() == null) continue;
            if (data.getItems().stream().noneMatch(itemKey -> item.getType().getKey().asString().equals(itemKey))) {
                continue;
            }
            if (!data.getNamePattern().matcher(name).find()) {
                continue;
            }
            meta.setItemModel(data.getKey());
            meta.getPersistentDataContainer().set(isManagedKey, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
            return;
        }

        if (meta.getPersistentDataContainer().has(isManagedKey)) {
            meta.setItemModel(null);
            meta.getPersistentDataContainer().remove(isManagedKey);
            item.setItemMeta(meta);
        }
    }
}
