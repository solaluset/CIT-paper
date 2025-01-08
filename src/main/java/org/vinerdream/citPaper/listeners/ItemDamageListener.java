package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.vinerdream.citPaper.CITPaper;

public class ItemDamageListener implements Listener {
    private final CITPaper plugin;

    public ItemDamageListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        plugin.getItemUpdater().updateItem(event.getItem(), event.getDamage());
    }
}
