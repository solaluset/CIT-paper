package org.vinerdream.citPaper.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Getter
public class ResourcePacksPostGenerateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final List<Path> convertedResourcePacks;
    private final Map<Path, Exception> failedResourcePacks;

    public ResourcePacksPostGenerateEvent(List<Path> convertedResourcePacks, Map<Path, Exception> failedResourcePacks) {
        this.convertedResourcePacks = convertedResourcePacks;
        this.failedResourcePacks = failedResourcePacks;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
