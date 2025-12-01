package com.boxserver.listeners;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import com.boxserver.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles combat-related events for PvP region protection.
 */
public class CombatListener implements Listener {
    private final BoxServer plugin;

    public CombatListener(BoxServer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only handle player damage
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = getAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        // Check for bypass permissions
        if (attacker.hasPermission("boxserver.admin") || attacker.hasPermission("boxserver.bypass.pvp")) {
            return;
        }

        // Check regions for both players
        Location victimLoc = victim.getLocation();
        Location attackerLoc = attacker.getLocation();

        Region victimRegion = plugin.getRegionManager().getRegionAt(victimLoc);
        Region attackerRegion = plugin.getRegionManager().getRegionAt(attackerLoc);

        // If either player is in a region where PvP is disabled, cancel the damage
        if (victimRegion != null && !victimRegion.isPvpEnabled()) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.no-pvp", "&cPvP is disabled in this area!");
            MessageUtil.send(attacker, message);
            return;
        }

        if (attackerRegion != null && !attackerRegion.isPvpEnabled()) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.no-pvp", "&cPvP is disabled in this area!");
            MessageUtil.send(attacker, message);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Prevent all player damage in spawn (falling, suffocation, etc.)
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Location location = player.getLocation();
        Region region = plugin.getRegionManager().getRegionAt(location);

        if (region != null && region.getType() == RegionType.SPAWN) {
            // Only prevent environmental damage types in spawn
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.FALL ||
                cause == EntityDamageEvent.DamageCause.SUFFOCATION ||
                cause == EntityDamageEvent.DamageCause.DROWNING ||
                cause == EntityDamageEvent.DamageCause.FIRE ||
                cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                cause == EntityDamageEvent.DamageCause.LAVA ||
                cause == EntityDamageEvent.DamageCause.CONTACT ||
                cause == EntityDamageEvent.DamageCause.CRAMMING) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Get the attacking player from a damager entity.
     * Handles projectiles and other indirect damage sources.
     */
    private Player getAttacker(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        return null;
    }
}
