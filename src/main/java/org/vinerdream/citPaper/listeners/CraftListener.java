package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;

public class CraftListener implements Listener {
    private final CITPaper plugin;

    public CraftListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        plugin.getItemUpdater().updateItem(event.getInventory().getResult());
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getInventory().getResult();
        plugin.getItemUpdater().updateItem(result);
        event.getInventory().setResult(result);
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        ItemStack result = event.getResult();
        plugin.getItemUpdater().updateItem(result);
        event.setResult(result);
    }
}
