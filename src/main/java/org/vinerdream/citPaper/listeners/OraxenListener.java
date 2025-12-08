package org.vinerdream.citPaper.listeners;

import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.vinerdream.citPaper.CITPaper;

import java.util.List;
import java.util.Locale;

public class OraxenListener implements Listener {
    private final CITPaper plugin;

    public OraxenListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemsLoaded(OraxenItemsLoadedEvent event) {
        plugin.loadRenames();
    }

    @EventHandler
    public void onPackGenerated(OraxenPackGeneratedEvent event) {
        final String armorType = plugin.getConfig().getString("oraxen.armorType", "CHAINMAIL").toLowerCase(Locale.ROOT);
        for (String texture : List.of("1", "2")) {
            event.getOutput().add(new VirtualFile(
                    "assets/minecraft/textures/models/armor",
                    String.format("%s_layer_%s.png", armorType, texture),
                    getClass().getResourceAsStream("/images/armor_transparent.png")
            ));
        }
    }
}
