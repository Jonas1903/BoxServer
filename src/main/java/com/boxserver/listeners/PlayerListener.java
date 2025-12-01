package com.boxserver.listeners;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import com.boxserver.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Handles player-related events for region protection.
 */
public class PlayerListener implements Listener {
    private final BoxServer plugin;

    public PlayerListener(BoxServer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check for wind charge usage
        if (item != null && item.getType() == Material.WIND_CHARGE) {
            Location location = player.getLocation();
            Region region = plugin.getRegionManager().getRegionAt(location);

            if (region != null && !region.allowsWindCharges()) {
                if (!player.hasPermission("boxserver.admin") && !player.hasPermission("boxserver.bypass.build")) {
                    event.setCancelled(true);
                    String message = plugin.getConfig().getString("messages.no-windcharge", "&cWind charges are disabled in this area!");
                    MessageUtil.send(player, message);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // Check if projectile is a wind charge
        if (event.getEntity() instanceof WindCharge windCharge) {
            if (windCharge.getShooter() instanceof Player player) {
                Location location = player.getLocation();
                Region region = plugin.getRegionManager().getRegionAt(location);

                if (region != null && !region.allowsWindCharges()) {
                    if (!player.hasPermission("boxserver.admin") && !player.hasPermission("boxserver.bypass.build")) {
                        event.setCancelled(true);
                        String message = plugin.getConfig().getString("messages.no-windcharge", "&cWind charges are disabled in this area!");
                        MessageUtil.send(player, message);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Handle wind charge explosions
        if (event.getEntity() instanceof WindCharge) {
            event.blockList().removeIf(block -> {
                Region region = plugin.getRegionManager().getRegionAt(block.getLocation());
                if (region == null) {
                    return false;
                }

                // Remove blocks in protected regions from explosion list
                return region.getType() == RegionType.SPAWN ||
                       region.getType() == RegionType.PROTECTED ||
                       region.getType() == RegionType.BOUNDARY;
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if actual block position changed
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();
        Region toRegion = plugin.getRegionManager().getRegionAt(to);

        // Handle player pushing prevention in spawn
        if (toRegion != null && toRegion.getType() == RegionType.SPAWN) {
            // Check if player is being pushed by another player's velocity
            Vector velocity = player.getVelocity();
            if (velocity.length() > 0.5 && !player.isSprinting() && !player.isFlying()) {
                // This is likely being pushed - reduce the velocity
                player.setVelocity(velocity.multiply(0.1));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Handle ender pearl teleportation
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            Player player = event.getPlayer();
            Location to = event.getTo();

            if (player.hasPermission("boxserver.admin") || player.hasPermission("boxserver.bypass.build")) {
                return;
            }

            Region toRegion = plugin.getRegionManager().getRegionAt(to);
            Region fromRegion = plugin.getRegionManager().getRegionAt(event.getFrom());

            // Prevent ender pearling into spawn from outside
            if (toRegion != null && toRegion.getType() == RegionType.SPAWN) {
                if (fromRegion == null || fromRegion.getType() != RegionType.SPAWN) {
                    event.setCancelled(true);
                    MessageUtil.send(player, "&cYou cannot ender pearl into this area!");
                }
            }
        }
    }
}
