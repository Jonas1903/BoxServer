package com.boxserver.models;

/**
 * Enum representing the different types of regions in the BoxServer plugin.
 */
public enum RegionType {
    /**
     * Spawn region - center rectangle with limited block interactions.
     * Block breaking disabled (except whitelisted blocks), block placing disabled,
     * block interaction enabled, wind charges disabled, PvP disabled, player pushing disabled.
     */
    SPAWN,

    /**
     * PvP area - outside spawn, inside the box walls.
     * Block breaking enabled, block placing enabled (with height restriction),
     * wind charges enabled, PvP enabled.
     */
    PVP,

    /**
     * Protected sub-region - inside PvP area.
     * Block breaking disabled, block placing disabled, PvP enabled.
     */
    PROTECTED,

    /**
     * Boundary region - walls, floor, and ceiling of the arena.
     * Cannot be broken or placed upon.
     */
    BOUNDARY
}
