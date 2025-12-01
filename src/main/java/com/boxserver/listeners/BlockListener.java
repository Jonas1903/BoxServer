package com.boxserver.listeners;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import com.boxserver.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

import java.util.List;

/**
 * Handles all block-related events for region protection.
 */
public class BlockListener implements Listener {
    private final BoxServer plugin;

    public BlockListener(BoxServer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Check for admin bypass
        if (player.hasPermission("boxserver.admin") || player.hasPermission("boxserver.bypass.build")) {
            return;
        }

        Region region = plugin.getRegionManager().getRegionAt(location);
        if (region == null) {
            return;
        }

        // Check if the block can be broken based on region rules
        if (!region.canBreak(block.getType())) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.no-break", "&cYou cannot break blocks here!");
            MessageUtil.send(player, message);
            return;
        }

        // If the block was player-placed and is being broken, untrack it
        if (plugin.getBlockTracker().isTracked(block)) {
            plugin.getBlockTracker().untrackBlock(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        // Check for admin bypass
        if (player.hasPermission("boxserver.admin") || player.hasPermission("boxserver.bypass.build")) {
            return;
        }

        Region region = plugin.getRegionManager().getRegionAt(location);
        if (region == null) {
            return;
        }

        // Check if blocks can be placed in this region
        if (!region.canPlace()) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.no-place", "&cYou cannot place blocks here!");
            MessageUtil.send(player, message);
            return;
        }

        // PvP region height restriction - cannot place blocks more than 6 blocks above ceiling
        if (region.getType() == RegionType.PVP) {
            int ceilingY = region.getCeilingY();
            int blockY = location.getBlockY();
            
            if (blockY > ceilingY + 6) {
                event.setCancelled(true);
                String message = plugin.getConfig().getString("messages.too-high", "&cYou cannot place blocks this high!");
                MessageUtil.send(player, message);
                return;
            }

            // Track the placed block for reset feature
            plugin.getBlockTracker().trackBlock(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block source = event.getBlock();
        Block target = event.getToBlock();

        // Check if water/lava is flowing into a protected region
        Region sourceRegion = plugin.getRegionManager().getRegionAt(source.getLocation());
        Region targetRegion = plugin.getRegionManager().getRegionAt(target.getLocation());

        // Allow flow within the same region or to unprotected areas
        if (sourceRegion == targetRegion) {
            return;
        }

        // Block flow into spawn or protected regions from outside
        if (targetRegion != null && 
            (targetRegion.getType() == RegionType.SPAWN || 
             targetRegion.getType() == RegionType.PROTECTED ||
             targetRegion.getType() == RegionType.BOUNDARY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> blocks = event.getBlocks();
        Block piston = event.getBlock();
        
        for (Block block : blocks) {
            Location targetLoc = block.getRelative(event.getDirection()).getLocation();
            Region targetRegion = plugin.getRegionManager().getRegionAt(targetLoc);

            // Prevent pistons from pushing blocks into protected regions
            if (targetRegion != null && 
                (targetRegion.getType() == RegionType.SPAWN || 
                 targetRegion.getType() == RegionType.PROTECTED ||
                 targetRegion.getType() == RegionType.BOUNDARY)) {
                
                Region sourceRegion = plugin.getRegionManager().getRegionAt(block.getLocation());
                if (sourceRegion != targetRegion) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> blocks = event.getBlocks();

        for (Block block : blocks) {
            Location targetLoc = block.getRelative(event.getDirection()).getLocation();
            Region targetRegion = plugin.getRegionManager().getRegionAt(targetLoc);

            // Prevent pistons from pulling blocks into protected regions
            if (targetRegion != null && 
                (targetRegion.getType() == RegionType.SPAWN || 
                 targetRegion.getType() == RegionType.PROTECTED ||
                 targetRegion.getType() == RegionType.BOUNDARY)) {
                
                Region sourceRegion = plugin.getRegionManager().getRegionAt(block.getLocation());
                if (sourceRegion != targetRegion) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        Region region = plugin.getRegionManager().getRegionAt(block.getLocation());

        if (region != null && 
            (region.getType() == RegionType.SPAWN || 
             region.getType() == RegionType.PROTECTED)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        Region region = plugin.getRegionManager().getRegionAt(block.getLocation());

        if (region != null && 
            (region.getType() == RegionType.SPAWN || 
             region.getType() == RegionType.PROTECTED ||
             region.getType() == RegionType.BOUNDARY)) {
            event.setCancelled(true);
        }
    }
}
