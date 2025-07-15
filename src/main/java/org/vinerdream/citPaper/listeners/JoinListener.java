package org.vinerdream.citPaper.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;

import java.util.Arrays;

public class JoinListener implements Listener {
    private final CITPaper plugin;

    public JoinListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (ItemStack item : event.getPlayer().getInventory().getContents()) {
                plugin.getItemUpdater().updateItem(item);
            }
        }, 20);
    }
}
