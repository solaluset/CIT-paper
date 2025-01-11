package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.SmithItemEvent;
import org.vinerdream.citPaper.CITPaper;

public class SmithingListener implements Listener {
    private final CITPaper plugin;

    public SmithingListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSmith(SmithItemEvent event) {
        plugin.getItemUpdater().updateItem(event.getInventory().getResult());
    }
}
