package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;

public class InventoryListener implements Listener {
    private final CITPaper plugin;

    public InventoryListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            plugin.getItemUpdater().updateItem(item);
        }
    }
}
