package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;

import java.util.Arrays;

public class InventoryListener implements Listener {
    private final CITPaper plugin;

    public InventoryListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        event.getInventory().setContents(Arrays.stream(event.getInventory().getContents()).map(item -> {
            if (item == null) return null;
            plugin.getItemUpdater().updateItem(item);
            return item;
        }).toArray(ItemStack[]::new));
    }
}
