package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.config.Mode;

public class EnchantListener implements Listener {
    private final CITPaper plugin;

    public EnchantListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        final ItemStack oraxenResult = plugin.getItemUpdater().updateItem(event.getItem(), event.getEnchantsToAdd());
        if (plugin.getMode() == Mode.ORAXEN && oraxenResult != null) {
            event.setItem(oraxenResult);
        }
    }
}
