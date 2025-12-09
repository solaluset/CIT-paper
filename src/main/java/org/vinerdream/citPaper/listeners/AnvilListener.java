package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.config.Mode;

public class AnvilListener implements Listener {
    private final CITPaper plugin;

    public AnvilListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        final ItemStack oraxenItem;
        final String renameText = event.getView().getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            oraxenItem = plugin.getItemUpdater().updateItem(event.getResult(), renameText);
        } else {
            oraxenItem = plugin.getItemUpdater().updateItem(event.getResult());
        }
        if (plugin.getMode() == Mode.ORAXEN && oraxenItem != null) {
            event.setResult(oraxenItem);
        }
    }
}
