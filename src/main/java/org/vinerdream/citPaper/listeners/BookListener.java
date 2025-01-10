package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.vinerdream.citPaper.CITPaper;

public class BookListener implements Listener {
    private final CITPaper plugin;

    public BookListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBookSign(PlayerEditBookEvent event) {
        BookMeta meta = event.getNewBookMeta();
        plugin.getItemUpdater().updateMeta(meta);
        event.setNewBookMeta(meta);
    }
}
