package org.vinerdream.citPaper.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.vinerdream.citPaper.CITPaper;
import org.vinerdream.citPaper.converter.ParsedTextureProperties;

public class BookListener implements Listener {
    private final CITPaper plugin;

    public BookListener(CITPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBookSign(PlayerEditBookEvent event) {
        BookMeta meta = event.getNewBookMeta();
        if (meta.getTitle() == null || meta.hasDisplayName()) return;

        for (ParsedTextureProperties data : plugin.getRenames()) {
            if (data.getNamePattern() == null) continue;
            if (data.getItems().stream().noneMatch("minecraft:written_book"::equals)) {
                continue;
            }
            if (!data.getNamePattern().matcher(meta.getTitle()).find()) {
                continue;
            }
            meta.setItemModel(data.getKey());
            meta.getPersistentDataContainer().set(plugin.getIsManagedKey(), PersistentDataType.BOOLEAN, true);
            event.setNewBookMeta(meta);
            return;
        }}
}
