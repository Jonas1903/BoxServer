package com.boxserver.managers;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all regions in the BoxServer plugin.
 */
public class RegionManager {
    private final BoxServer plugin;
    private final Map<String, Region> regions;
    private final Map<UUID, Location> pos1Selections;
    private final Map<UUID, Location> pos2Selections;
    private File regionsFile;

    public RegionManager(BoxServer plugin) {
        this.plugin = plugin;
        this.regions = new ConcurrentHashMap<>();
        this.pos1Selections = new ConcurrentHashMap<>();
        this.pos2Selections = new ConcurrentHashMap<>();
        loadRegions();
    }

    /**
     * Load regions from the configuration file.
     */
    public void loadRegions() {
        regions.clear();
        regionsFile = new File(plugin.getDataFolder(), "regions.yml");

        if (!regionsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);
        ConfigurationSection regionsSection = config.getConfigurationSection("regions");

        if (regionsSection == null) {
            return;
        }

        for (String name : regionsSection.getKeys(false)) {
            ConfigurationSection regionSection = regionsSection.getConfigurationSection(name);
            if (regionSection == null) continue;

            try {
                UUID worldId = UUID.fromString(regionSection.getString("world", ""));
                RegionType type = RegionType.valueOf(regionSection.getString("type", "PVP"));
                int minX = regionSection.getInt("minX");
                int minY = regionSection.getInt("minY");
                int minZ = regionSection.getInt("minZ");
                int maxX = regionSection.getInt("maxX");
                int maxY = regionSection.getInt("maxY");
                int maxZ = regionSection.getInt("maxZ");
                int priority = regionSection.getInt("priority", 0);
                boolean pvpEnabled = regionSection.getBoolean("pvpEnabled", type != RegionType.SPAWN);

                Region region = new Region(name, worldId, type, minX, minY, minZ, maxX, maxY, maxZ);
                region.setPriority(priority);
                region.setPvpEnabled(pvpEnabled);

                // Load whitelisted blocks
                List<String> blockList = regionSection.getStringList("whitelistedBlocks");
                if (!blockList.isEmpty()) {
                    Set<Material> blocks = blockList.stream()
                            .map(Material::valueOf)
                            .collect(Collectors.toSet());
                    region.setWhitelistedBlocks(blocks);
                }

                regions.put(name.toLowerCase(), region);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load region: " + name + " - " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + regions.size() + " regions.");
    }

    /**
     * Save all regions to the configuration file.
     */
    public void saveRegions() {
        if (regionsFile == null) {
            regionsFile = new File(plugin.getDataFolder(), "regions.yml");
        }

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection regionsSection = config.createSection("regions");

        for (Region region : regions.values()) {
            ConfigurationSection regionSection = regionsSection.createSection(region.getName());
            regionSection.set("world", region.getWorldId().toString());
            regionSection.set("type", region.getType().name());
            regionSection.set("minX", region.getMinX());
            regionSection.set("minY", region.getMinY());
            regionSection.set("minZ", region.getMinZ());
            regionSection.set("maxX", region.getMaxX());
            regionSection.set("maxY", region.getMaxY());
            regionSection.set("maxZ", region.getMaxZ());
            regionSection.set("priority", region.getPriority());
            regionSection.set("pvpEnabled", region.isPvpEnabled());

            List<String> blockList = region.getWhitelistedBlocks().stream()
                    .map(Material::name)
                    .toList();
            regionSection.set("whitelistedBlocks", blockList);
        }

        try {
            config.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save regions: " + e.getMessage());
        }
    }

    /**
     * Create a new region.
     */
    public boolean createRegion(String name, UUID worldId, RegionType type, Location pos1, Location pos2) {
        if (regions.containsKey(name.toLowerCase())) {
            return false;
        }

        Region region = new Region(
                name,
                worldId,
                type,
                pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ()
        );

        regions.put(name.toLowerCase(), region);
        saveRegions();
        return true;
    }

    /**
     * Delete a region.
     */
    public boolean deleteRegion(String name) {
        Region removed = regions.remove(name.toLowerCase());
        if (removed != null) {
            saveRegions();
            return true;
        }
        return false;
    }

    /**
     * Get a region by name.
     */
    public Region getRegion(String name) {
        return regions.get(name.toLowerCase());
    }

    /**
     * Get all regions.
     */
    public Collection<Region> getAllRegions() {
        return regions.values();
    }

    /**
     * Get the highest priority region at a location.
     */
    public Region getRegionAt(Location location) {
        return regions.values().stream()
                .filter(r -> r.contains(location))
                .max(Comparator.comparingInt(Region::getPriority))
                .orElse(null);
    }

    /**
     * Get all regions at a location, sorted by priority.
     */
    public List<Region> getRegionsAt(Location location) {
        return regions.values().stream()
                .filter(r -> r.contains(location))
                .sorted(Comparator.comparingInt(Region::getPriority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all regions of a specific type.
     */
    public List<Region> getRegionsByType(RegionType type) {
        return regions.values().stream()
                .filter(r -> r.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Set position 1 for a player's region selection.
     */
    public void setPos1(UUID playerId, Location location) {
        pos1Selections.put(playerId, location);
    }

    /**
     * Set position 2 for a player's region selection.
     */
    public void setPos2(UUID playerId, Location location) {
        pos2Selections.put(playerId, location);
    }

    /**
     * Get position 1 for a player's region selection.
     */
    public Location getPos1(UUID playerId) {
        return pos1Selections.get(playerId);
    }

    /**
     * Get position 2 for a player's region selection.
     */
    public Location getPos2(UUID playerId) {
        return pos2Selections.get(playerId);
    }

    /**
     * Clear a player's region selection.
     */
    public void clearSelection(UUID playerId) {
        pos1Selections.remove(playerId);
        pos2Selections.remove(playerId);
    }

    /**
     * Check if a player has both positions selected.
     */
    public boolean hasCompleteSelection(UUID playerId) {
        Location pos1 = pos1Selections.get(playerId);
        Location pos2 = pos2Selections.get(playerId);
        
        if (pos1 == null || pos2 == null) {
            return false;
        }
        
        // Both positions must be in the same world
        return pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld());
    }

    /**
     * Reload all region data.
     */
    public void reload() {
        loadRegions();
    }
}
