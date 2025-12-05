package org.vinerdream.citPaper.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class ItemUtils {
    private static final Registry<ItemType> registry = Bukkit.getRegistry(ItemType.class);

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
}
