package com.boxserver.managers;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import com.boxserver.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages the automatic reset of player-placed blocks in PvP regions.
 */
public class ResetManager {
    private final BoxServer plugin;
    private BukkitTask resetTask;
    private long lastResetTime;
    private int resetIntervalMinutes;

    public ResetManager(BoxServer plugin) {
        this.plugin = plugin;
        this.resetIntervalMinutes = plugin.getConfig().getInt("reset-interval-minutes", 10);
        this.lastResetTime = System.currentTimeMillis();
        startResetTask();
    }

    /**
     * Start the automatic reset task.
     */
    public void startResetTask() {
        stopResetTask();

        // Convert minutes to ticks (20 ticks = 1 second)
        long intervalTicks = resetIntervalMinutes * 60L * 20L;

        // Warning task - runs every second to check for warnings
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkWarnings, 20L, 20L);

        // Reset task
        resetTask = Bukkit.getScheduler().runTaskTimer(plugin, this::performReset, intervalTicks, intervalTicks);

        plugin.getLogger().info("Started reset task with interval of " + resetIntervalMinutes + " minutes.");
    }

    /**
     * Stop the automatic reset task.
     */
    public void stopResetTask() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
    }

    /**
     * Check and send warning messages before reset.
     */
    private void checkWarnings() {
        long timeUntilReset = getTimeUntilReset();
        long seconds = timeUntilReset / 1000;

        String warningMessage = plugin.getConfig().getString("messages.reset-warning", "&eBlock reset in %time% seconds!");

        // Send warnings at 60, 30, and 10 seconds
        if (seconds == 60 || seconds == 30 || seconds == 10 || seconds == 5) {
            String message = MessageUtil.replacePlaceholders(warningMessage, "%time%", String.valueOf(seconds));
            broadcastToPlayers(message);
        }
    }

    /**
     * Perform the block reset for all PvP regions.
     */
    public void performReset() {
        int totalReset = 0;

        for (Region region : plugin.getRegionManager().getRegionsByType(RegionType.PVP)) {
            totalReset += plugin.getBlockTracker().resetBlocksInRegion(region);
        }

        lastResetTime = System.currentTimeMillis();

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
     * Get the time until the next reset in milliseconds.
     */
    public long getTimeUntilReset() {
        long elapsed = System.currentTimeMillis() - lastResetTime;
        long interval = resetIntervalMinutes * 60L * 1000L;
        return Math.max(0, interval - elapsed);
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
        this.lastResetTime = System.currentTimeMillis();
        startResetTask();
    }
}
