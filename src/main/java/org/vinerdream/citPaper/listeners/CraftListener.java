package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.config.Mode;

public class CraftListener implements Listener {
    private final CITPaper plugin;

    public CraftListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        final ItemStack oraxenResult = plugin.getItemUpdater().updateItem(event.getInventory().getResult());
        if (plugin.getMode() == Mode.ORAXEN && oraxenResult != null) {
            event.getInventory().setResult(oraxenResult);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        final ItemStack oraxenResult = plugin.getItemUpdater().updateItem(event.getInventory().getResult());
        if (plugin.getMode() == Mode.ORAXEN && oraxenResult != null) {
            event.getInventory().setResult(oraxenResult);
        }
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        ItemStack result = event.getResult();
        final ItemStack oraxenResult = plugin.getItemUpdater().updateItem(result);
        event.setResult(result);
        if (plugin.getMode() == Mode.ORAXEN && oraxenResult != null) {
            event.setResult(oraxenResult);
        }
    }
}
