package org.vinerdream.citPaper.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.utils.SchedulerUtils;

public class PlayerListener implements Listener {
    private final CITPaper plugin;

    public PlayerListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    private void scheduleInventoryUpdate(Player player) {
        SchedulerUtils.entityRunTaskLater(player, plugin, () -> {
            for (ItemStack item : player.getInventory().getContents()) {
                plugin.getItemUpdater().updateItem(item);
            }
        }, 20);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleInventoryUpdate(event.getPlayer());
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        scheduleInventoryUpdate(event.getPlayer());
    }
}
