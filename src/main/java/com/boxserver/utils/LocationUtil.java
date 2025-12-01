package com.boxserver.utils;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * Utility class for location-related operations.
 */
public class LocationUtil {

    /**
     * Convert a location to a string key for storage.
     */
    public static String locationToKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return String.format("%s;%d;%d;%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Convert a block to a string key for storage.
     */
    public static String blockToKey(Block block) {
        if (block == null) {
            return null;
        }
        return locationToKey(block.getLocation());
    }

    /**
     * Get a formatted string representation of a location.
     */
    public static String formatLocation(Location location) {
        if (location == null) {
            return "null";
        }
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
        return String.format("%s (%d, %d, %d)",
                worldName,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Check if two locations are within a certain distance of each other.
     */
    public static boolean isWithinDistance(Location loc1, Location loc2, double distance) {
        if (loc1 == null || loc2 == null) {
            return false;
        }
        if (loc1.getWorld() == null || loc2.getWorld() == null) {
            return false;
        }
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            return false;
        }
        return loc1.distanceSquared(loc2) <= distance * distance;
    }

    /**
     * Get the chunk key for a location (for chunk-based storage).
     */
    public static long getChunkKey(Location location) {
        if (location == null) {
            return 0;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Get the chunk key for specific coordinates.
     */
    public static long getChunkKey(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
