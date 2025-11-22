package org.vinerdream.citPaper.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SchedulerUtils {
    private static final boolean hasGlobalRegionScheduler = ReflectionUtils.hasMethod(Bukkit.class, "getGlobalRegionScheduler");
    private static final boolean hasAsyncScheduler = ReflectionUtils.hasMethod(Bukkit.class, "getAsyncScheduler");
    private static final boolean hasEntityScheduler = ReflectionUtils.hasMethod(Entity.class, "getScheduler");

    public static void runTask(Plugin plugin, Runnable task) {
        if (hasGlobalRegionScheduler) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void runTaskAsynchronously(Plugin plugin, Runnable task) {
        if (hasAsyncScheduler) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void entityRunTaskLater(Entity entity, Plugin plugin, Runnable task, long delay) {
        if (hasEntityScheduler) {
            entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }
}
