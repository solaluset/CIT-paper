package org.vinerdream.citPaper.listeners;

import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.utils.VirtualFile;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.utils.OraxenDatapackHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

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
        final String armorType = plugin.getOraxenArmorType();
        for (String texture : List.of("1", "2")) {
            event.getOutput().add(new VirtualFile(
                    "assets/minecraft/textures/models/armor",
                    String.format("%s_layer_%s.png", armorType, texture),
                    getClass().getResourceAsStream("/images/armor_transparent.png")
            ));
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        final File trimsCacheFile = plugin.getDataFolder().toPath().resolve("oraxen-trims-cache.json").toFile();
        try {
            final Set<String> currentTrims = OraxenDatapackHelper.getCurrentTrimsFiles();
            final Set<String> cachedTrims;
            if (trimsCacheFile.isFile()) {
                cachedTrims = OraxenDatapackHelper.getCachedTrimsFiles(trimsCacheFile);
            } else {
                cachedTrims = Set.of();
            }
            if (!currentTrims.equals(cachedTrims)) {
                plugin.getLogger().info("Trims files changed, restarting...");
                OraxenDatapackHelper.cacheTrimsFiles(trimsCacheFile, currentTrims);
                plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), plugin.getMainConfig().getOraxenRestartCommand());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to update Oraxen trim cache: " + e);
        }
    }
}
