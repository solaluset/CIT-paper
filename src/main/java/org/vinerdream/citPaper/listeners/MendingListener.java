package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.vinerdream.citPaper.CITPaper;

public class MendingListener implements Listener {
    private final CITPaper plugin;

    public MendingListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMend(PlayerItemMendEvent event) {
        plugin.getItemUpdater().updateItem(event.getItem(), -event.getRepairAmount());
    }
}
