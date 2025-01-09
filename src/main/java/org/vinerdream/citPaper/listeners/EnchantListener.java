package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.vinerdream.citPaper.CITPaper;

public class EnchantListener implements Listener {
    private final CITPaper plugin;

    public EnchantListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        plugin.getItemUpdater().updateItem(event.getItem(), event.getEnchantsToAdd());
    }
}
