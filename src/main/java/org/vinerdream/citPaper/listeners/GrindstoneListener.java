package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.config.Mode;

public class GrindstoneListener implements Listener {
    private final CITPaper plugin;

    public GrindstoneListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGrindstonePrepare(PrepareGrindstoneEvent event) {
        final ItemStack oraxenResult = plugin.getItemUpdater().updateItem(event.getResult());
        if (plugin.getMode() == Mode.ORAXEN && oraxenResult != null) {
            event.setResult(oraxenResult);
        }
    }
}
