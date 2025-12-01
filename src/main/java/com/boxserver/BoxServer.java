package com.boxserver;

import com.boxserver.commands.BoxServerCommand;
import com.boxserver.commands.BoxServerTabCompleter;
import com.boxserver.listeners.BlockListener;
import com.boxserver.listeners.CombatListener;
import com.boxserver.listeners.PlayerListener;
import com.boxserver.managers.BlockTracker;
import com.boxserver.managers.RegionManager;
import com.boxserver.managers.ResetManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for BoxServer.
 * Manages a cube arena with spawn and PvP regions with specific protection rules.
 */
public class BoxServer extends JavaPlugin {
    private RegionManager regionManager;
    private BlockTracker blockTracker;
    private ResetManager resetManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        regionManager = new RegionManager(this);
        blockTracker = new BlockTracker(this);
        resetManager = new ResetManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        // Register commands
        PluginCommand boxServerCommand = getCommand("boxserver");
        if (boxServerCommand != null) {
            BoxServerCommand commandExecutor = new BoxServerCommand(this);
            boxServerCommand.setExecutor(commandExecutor);
            boxServerCommand.setTabCompleter(new BoxServerTabCompleter(this));
        }

        getLogger().info("BoxServer has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save data before shutdown
        if (regionManager != null) {
            regionManager.saveRegions();
        }
        if (blockTracker != null) {
            blockTracker.saveData();
        }
        if (resetManager != null) {
            resetManager.stopResetTask();
        }

        getLogger().info("BoxServer has been disabled!");
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public BlockTracker getBlockTracker() {
        return blockTracker;
    }

    public ResetManager getResetManager() {
        return resetManager;
    }
}
