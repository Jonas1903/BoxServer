package com.boxserver.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a region in the BoxServer arena.
 */
public class Region {
    private final String name;
    private final UUID worldId;
    private RegionType type;
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;
    private int priority;
    private boolean pvpEnabled;
    private Set<Material> whitelistedBlocks;

    public Region(String name, UUID worldId, RegionType type, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.worldId = worldId;
        this.type = type;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.priority = 0;
        this.pvpEnabled = type != RegionType.SPAWN;
        this.whitelistedBlocks = new HashSet<>();

        // Set default whitelisted blocks for spawn regions
        if (type == RegionType.SPAWN) {
            initDefaultWhitelistedBlocks();
        }
    }

    private void initDefaultWhitelistedBlocks() {
        whitelistedBlocks.add(Material.STONE);
        whitelistedBlocks.add(Material.SPRUCE_LOG);
        whitelistedBlocks.add(Material.DIAMOND_ORE);
        whitelistedBlocks.add(Material.DEEPSLATE_DIAMOND_ORE);
        whitelistedBlocks.add(Material.EMERALD_ORE);
        whitelistedBlocks.add(Material.DEEPSLATE_EMERALD_ORE);
        whitelistedBlocks.add(Material.IRON_ORE);
        whitelistedBlocks.add(Material.DEEPSLATE_IRON_ORE);
        whitelistedBlocks.add(Material.LAPIS_ORE);
        whitelistedBlocks.add(Material.DEEPSLATE_LAPIS_ORE);
        whitelistedBlocks.add(Material.GOLD_ORE);
        whitelistedBlocks.add(Material.DEEPSLATE_GOLD_ORE);
        whitelistedBlocks.add(Material.SPAWNER);
    }

    /**
     * Check if a location is within this region.
     */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getUID().equals(worldId)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /**
     * Check if a block can be broken in this region.
     */
    public boolean canBreak(Material material) {
        return switch (type) {
            case SPAWN -> whitelistedBlocks.contains(material);
            case PVP -> true;
            case PROTECTED, BOUNDARY -> false;
        };
    }

    /**
     * Check if blocks can be placed in this region.
     */
    public boolean canPlace() {
        return type == RegionType.PVP;
    }

    /**
     * Check if wind charges are allowed in this region.
     */
    public boolean allowsWindCharges() {
        return type == RegionType.PVP;
    }

    /**
     * Check if PvP is enabled in this region.
     */
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    /**
     * Get the ceiling Y level of this region.
     */
    public int getCeilingY() {
        return maxY;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public RegionType getType() {
        return type;
    }

    public void setType(RegionType type) {
        this.type = type;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public Set<Material> getWhitelistedBlocks() {
        return whitelistedBlocks;
    }

    public void setWhitelistedBlocks(Set<Material> whitelistedBlocks) {
        this.whitelistedBlocks = whitelistedBlocks;
    }

    public void addWhitelistedBlock(Material material) {
        whitelistedBlocks.add(material);
    }

    public void removeWhitelistedBlock(Material material) {
        whitelistedBlocks.remove(material);
    }

    public void clearWhitelistedBlocks() {
        whitelistedBlocks.clear();
    }

    public void setCorners(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    @Override
    public String toString() {
        return String.format("Region{name='%s', type=%s, bounds=[%d,%d,%d to %d,%d,%d], priority=%d}",
                name, type, minX, minY, minZ, maxX, maxY, maxZ, priority);
    }
}
