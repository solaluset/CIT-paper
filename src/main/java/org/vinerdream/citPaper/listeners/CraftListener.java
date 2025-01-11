package org.vinerdream.citPaper.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.vinerdream.citPaper.CITPaper;

import java.util.Arrays;

public class CraftListener implements Listener {
    private final CITPaper plugin;

    public CraftListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        plugin.getItemUpdater().updateItem(event.getInventory().getResult());
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    plugin.getItemUpdater().updateItem(event.getWhoClicked().getItemOnCursor());
                    PlayerInventory inventory = event.getWhoClicked().getInventory();
                    inventory.setContents(Arrays.stream(
                            inventory.getContents()
                    ).peek(plugin.getItemUpdater()::updateItem).toArray(ItemStack[]::new));
                },
                1
        );
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        ItemStack result = event.getResult();
        plugin.getItemUpdater().updateItem(result);
        event.setResult(result);
    }
}
