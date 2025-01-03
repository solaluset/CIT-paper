package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;

public class AnvilListener implements Listener {
    private final CITPaper plugin;

    public AnvilListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();

        if (result == null) return;

        plugin.getItemUpdater().updateItem(result, event.getView().getRenameText());
    }
}
