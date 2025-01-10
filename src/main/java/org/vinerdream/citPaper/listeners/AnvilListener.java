package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.vinerdream.citPaper.CITPaper;

public class AnvilListener implements Listener {
    private final CITPaper plugin;

    public AnvilListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        plugin.getItemUpdater().updateItem(event.getResult(), event.getView().getRenameText());
    }
}
