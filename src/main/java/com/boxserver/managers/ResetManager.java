package com.boxserver.managers;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import com.boxserver.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the automatic reset of player-placed blocks in PvP regions.
 */
public class ResetManager {
    private final BoxServer plugin;
    private BukkitTask resetTask;
    private final List<BukkitTask> warningTasks;
    private int resetIntervalMinutes;

    // Warning times in seconds before reset
    private static final int[] WARNING_TIMES = {60, 30, 10, 5};

    public ResetManager(BoxServer plugin) {
        this.plugin = plugin;
        this.warningTasks = new ArrayList<>();
        this.resetIntervalMinutes = plugin.getConfig().getInt("reset-interval-minutes", 10);
        startResetTask();
    }

    /**
     * Start the automatic reset task.
     */
    public void startResetTask() {
        stopResetTask();

        // Convert minutes to ticks (20 ticks = 1 second)
        long intervalTicks = resetIntervalMinutes * 60L * 20L;

        // Schedule warning tasks at specific times before reset
        scheduleWarnings(intervalTicks);

        // Schedule the reset task
        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            performReset();
            // Reschedule warnings for the next cycle
            scheduleWarnings(intervalTicks);
        }, intervalTicks, intervalTicks);

        plugin.getLogger().info("Started reset task with interval of " + resetIntervalMinutes + " minutes.");
    }

    /**
     * Schedule warning messages at specific times before the next reset.
     */
    private void scheduleWarnings(long intervalTicks) {
        // Cancel any existing warning tasks
        for (BukkitTask task : warningTasks) {
            task.cancel();
        }
        warningTasks.clear();

        String warningMessage = plugin.getConfig().getString("messages.reset-warning", "&eBlock reset in %time% seconds!");

        for (int seconds : WARNING_TIMES) {
            long warningTicks = intervalTicks - (seconds * 20L);
            if (warningTicks > 0) {
                final int warningSeconds = seconds;
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String message = MessageUtil.replacePlaceholders(warningMessage, "%time%", String.valueOf(warningSeconds));
                    broadcastToPlayers(message);
                }, warningTicks);
                warningTasks.add(task);
            }
        }
    }

    /**
     * Stop the automatic reset task.
     */
    public void stopResetTask() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
        for (BukkitTask task : warningTasks) {
            task.cancel();
        }
        warningTasks.clear();
    }

    /**
     * Perform the block reset for all PvP regions.
     */
    public void performReset() {
        int totalReset = 0;

        for (Region region : plugin.getRegionManager().getRegionsByType(RegionType.PVP)) {
            totalReset += plugin.getBlockTracker().resetBlocksInRegion(region);
        }

        String resetMessage = plugin.getConfig().getString("messages.reset-complete", "&aAll placed blocks have been reset!");
        broadcastToPlayers(resetMessage);

        plugin.getLogger().info("Reset " + totalReset + " blocks in PvP regions.");
    }

    /**
     * Manually reset blocks in a specific region.
     */
    public int resetRegion(Region region) {
        return plugin.getBlockTracker().resetBlocksInRegion(region);
    }

    /**
     * Set the reset interval in minutes.
     */
    public void setResetInterval(int minutes) {
        this.resetIntervalMinutes = minutes;
        plugin.getConfig().set("reset-interval-minutes", minutes);
        plugin.saveConfig();
        startResetTask();
    }

    /**
     * Get the current reset interval in minutes.
     */
    public int getResetIntervalMinutes() {
        return resetIntervalMinutes;
    }

    /**
     * Broadcast a message to all players.
     */
    private void broadcastToPlayers(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> MessageUtil.send(player, message));
    }

    /**
     * Reload the reset manager configuration.
     */
    public void reload() {
        this.resetIntervalMinutes = plugin.getConfig().getInt("reset-interval-minutes", 10);
        startResetTask();
    }
}
