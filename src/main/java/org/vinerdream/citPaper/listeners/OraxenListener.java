package org.vinerdream.citPaper.listeners;

import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.vinerdream.citPaper.CITPaper;

public class OraxenListener implements Listener {
    private final CITPaper plugin;

    public OraxenListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemsLoaded(OraxenItemsLoadedEvent event) {
        plugin.loadRenames();
    }
}
