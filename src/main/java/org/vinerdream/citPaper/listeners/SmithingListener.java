package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.config.Mode;

public class SmithingListener implements Listener {
    private final CITPaper plugin;

    public SmithingListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareSmith(PrepareSmithingEvent event) {
        if (denySmithing(event.getInventory().getInputEquipment())) {
            event.setResult(null);
            return;
        }
        final ItemStack oraxenResult = plugin.getItemUpdater().updateItem(event.getResult());
        if (plugin.getMode() == Mode.ORAXEN && oraxenResult != null) {
            event.setResult(oraxenResult);
        }
    }

    @EventHandler
    public void onSmith(SmithItemEvent event) {
        if (denySmithing(event.getInventory().getInputEquipment())) {
            event.setCancelled(true);
            return;
        }
        final ItemStack oraxenResult = plugin.getItemUpdater().updateItem(event.getInventory().getResult());
        if (plugin.getMode() == Mode.ORAXEN && oraxenResult != null) {
            event.getInventory().setResult(oraxenResult);
        }
    }

    @SuppressWarnings("removal")
    private boolean denySmithing(final ItemStack input) {
        if (plugin.getMode() != Mode.ORAXEN) {
            return false;
        }
        if (input != null && input.getItemMeta() instanceof ArmorMeta meta) {
            return meta.getTrim() != null && meta.getTrim().getPattern().getKey().getNamespace().equals("oraxen");
        }
        return false;
    }
}
