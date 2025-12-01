package com.boxserver.managers;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import com.boxserver.utils.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player-placed blocks for the reset feature.
 * Uses chunk-based storage for efficiency.
 */
public class BlockTracker {
    private final BoxServer plugin;
    private final Map<Long, Set<String>> chunkBlocks; // Chunk key -> Set of block keys
    private final Map<String, Long> blockTimestamps; // Block key -> Timestamp
    private File dataFile;

    public BlockTracker(BoxServer plugin) {
        this.plugin = plugin;
        this.chunkBlocks = new ConcurrentHashMap<>();
        this.blockTimestamps = new ConcurrentHashMap<>();
        loadData();
    }

    /**
     * Track a placed block.
     */
    public void trackBlock(Block block) {
        if (block == null) {
            return;
        }

        String key = LocationUtil.blockToKey(block);
        if (key == null) {
            return;
        }

        long chunkKey = LocationUtil.getChunkKey(block.getLocation());
        
        chunkBlocks.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(key);
        blockTimestamps.put(key, System.currentTimeMillis());
    }

    /**
     * Untrack a block (when broken by a player).
     */
    public void untrackBlock(Block block) {
        if (block == null) {
            return;
        }

        String key = LocationUtil.blockToKey(block);
        if (key == null) {
            return;
        }

        long chunkKey = LocationUtil.getChunkKey(block.getLocation());
        
        Set<String> blocks = chunkBlocks.get(chunkKey);
        if (blocks != null) {
            blocks.remove(key);
            if (blocks.isEmpty()) {
                chunkBlocks.remove(chunkKey);
            }
        }
        blockTimestamps.remove(key);
    }

    /**
     * Check if a block is tracked (player-placed).
     */
    public boolean isTracked(Block block) {
        if (block == null) {
            return false;
        }

        String key = LocationUtil.blockToKey(block);
        if (key == null) {
            return false;
        }

        return blockTimestamps.containsKey(key);
    }

    /**
     * Get all tracked blocks in a region.
     */
    public List<Location> getTrackedBlocksInRegion(Region region) {
        List<Location> locations = new ArrayList<>();
        World world = Bukkit.getWorld(region.getWorldId());
        
        if (world == null) {
            return locations;
        }

        // Iterate through all tracked blocks and check if they're in the region
        for (String key : blockTimestamps.keySet()) {
            Location loc = parseLocationKey(key, world);
            if (loc != null && region.contains(loc)) {
                locations.add(loc);
            }
        }

        return locations;
    }

    /**
     * Reset (remove) all tracked blocks in a region.
     */
    public int resetBlocksInRegion(Region region) {
        List<Location> blocksToRemove = getTrackedBlocksInRegion(region);
        int count = 0;

        for (Location loc : blocksToRemove) {
            Block block = loc.getBlock();
            if (block.getType() != Material.AIR) {
                block.setType(Material.AIR);
                count++;
            }
            untrackBlock(block);
        }

        return count;
    }

    /**
     * Get the total number of tracked blocks.
     */
    public int getTotalTrackedBlocks() {
        return blockTimestamps.size();
    }

    /**
     * Parse a location key back to a Location object.
     */
    private Location parseLocationKey(String key, World defaultWorld) {
        if (key == null) {
            return null;
        }

        String[] parts = key.split(";");
        if (parts.length != 4) {
            return null;
        }

        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                world = defaultWorld;
            }
            if (world == null) {
                return null;
            }

            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Load tracked blocks from file.
     */
    public void loadData() {
        chunkBlocks.clear();
        blockTimestamps.clear();

        dataFile = new File(plugin.getDataFolder(), "placed-blocks.yml");
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");

        if (blocksSection == null) {
            return;
        }

        for (String key : blocksSection.getKeys(false)) {
            long timestamp = blocksSection.getLong(key);
            String decodedKey = key.replace("_", ";");
            blockTimestamps.put(decodedKey, timestamp);

            // Parse location to get chunk key
            String[] parts = decodedKey.split(";");
            if (parts.length == 4) {
                try {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[3]);
                    long chunkKey = LocationUtil.getChunkKey(x, z);
                    chunkBlocks.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(decodedKey);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        plugin.getLogger().info("Loaded " + blockTimestamps.size() + " tracked blocks.");
    }

    /**
     * Save tracked blocks to file.
     */
    public void saveData() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), "placed-blocks.yml");
        }

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection blocksSection = config.createSection("blocks");

        for (Map.Entry<String, Long> entry : blockTimestamps.entrySet()) {
            // Replace semicolons with underscores for YAML key compatibility
            String encodedKey = entry.getKey().replace(";", "_");
            blocksSection.set(encodedKey, entry.getValue());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save tracked blocks: " + e.getMessage());
        }
    }

    /**
     * Clear all tracked blocks data.
     */
    public void clearAll() {
        chunkBlocks.clear();
        blockTimestamps.clear();
        saveData();
    }

    /**
     * Reload block tracking data.
     */
    public void reload() {
        loadData();
    }
}
