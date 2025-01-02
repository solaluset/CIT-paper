package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.utils.ItemUpdater;

public class AnvilListener implements Listener {
    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();

        if (result == null) return;

        ItemUpdater.updateItem(result, event.getView().getRenameText());
    }
}
