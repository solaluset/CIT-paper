package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;

public class GrindstoneListener implements Listener {
    private final CITPaper plugin;

    public GrindstoneListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGrindstonePrepare(PrepareGrindstoneEvent event) {
        ItemStack item = event.getResult();
        plugin.getItemUpdater().updateItem(item);
        event.setResult(item);
    }
}
