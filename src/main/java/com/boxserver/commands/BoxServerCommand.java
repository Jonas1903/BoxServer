package com.boxserver.commands;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import com.boxserver.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Main command handler for the BoxServer plugin.
 */
public class BoxServerCommand implements CommandExecutor {
    private final BoxServer plugin;

    public BoxServerCommand(BoxServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "region" -> handleRegionCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            case "blocks" -> handleBlocksCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            case "reload" -> handleReloadCommand(sender);
            case "reset" -> handleResetCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            case "setresettime" -> handleSetResetTimeCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, "&6=== BoxServer Commands ===");
        MessageUtil.send(sender, "&e/boxserver region <create|delete|pos1|pos2|list|info|priority>");
        MessageUtil.send(sender, "&e/boxserver blocks <add|remove|list|clear>");
        MessageUtil.send(sender, "&e/boxserver reload &7- Reload configuration");
        MessageUtil.send(sender, "&e/boxserver reset <region> &7- Reset placed blocks");
        MessageUtil.send(sender, "&e/boxserver setresettime <minutes> &7- Set reset interval");
    }

    private boolean handleRegionCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("boxserver.command.region")) {
            MessageUtil.send(sender, "&cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(sender, "&eUsage: /boxserver region <create|delete|pos1|pos2|list|info|priority>");
            return true;
        }

        String action = args[0].toLowerCase();

        return switch (action) {
            case "create" -> handleRegionCreate(sender, Arrays.copyOfRange(args, 1, args.length));
            case "delete" -> handleRegionDelete(sender, Arrays.copyOfRange(args, 1, args.length));
            case "pos1" -> handlePos1(sender);
            case "pos2" -> handlePos2(sender);
            case "list" -> handleRegionList(sender);
            case "info" -> handleRegionInfo(sender, Arrays.copyOfRange(args, 1, args.length));
            case "priority" -> handleRegionPriority(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> {
                MessageUtil.send(sender, "&cUnknown region command: " + action);
                yield true;
            }
        };
    }

    private boolean handleRegionCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cThis command can only be used by players!");
            return true;
        }

        if (args.length < 2) {
            MessageUtil.send(sender, "&eUsage: /boxserver region create <name> <type>");
            MessageUtil.send(sender, "&eTypes: spawn, pvp, protected, boundary");
            return true;
        }

        String name = args[0];
        String typeStr = args[1].toUpperCase();

        RegionType type;
        try {
            type = RegionType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, "&cInvalid region type: " + typeStr);
            MessageUtil.send(sender, "&eValid types: spawn, pvp, protected, boundary");
            return true;
        }

        if (!plugin.getRegionManager().hasCompleteSelection(player.getUniqueId())) {
            MessageUtil.send(sender, "&cYou must set both pos1 and pos2 first!");
            return true;
        }

        Location pos1 = plugin.getRegionManager().getPos1(player.getUniqueId());
        Location pos2 = plugin.getRegionManager().getPos2(player.getUniqueId());

        if (plugin.getRegionManager().getRegion(name) != null) {
            MessageUtil.send(sender, "&cA region with that name already exists!");
            return true;
        }

        boolean success = plugin.getRegionManager().createRegion(name, pos1.getWorld().getUID(), type, pos1, pos2);
        if (success) {
            MessageUtil.send(sender, "&aRegion '" + name + "' created successfully!");
            plugin.getRegionManager().clearSelection(player.getUniqueId());
        } else {
            MessageUtil.send(sender, "&cFailed to create region!");
        }

        return true;
    }

    private boolean handleRegionDelete(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(sender, "&eUsage: /boxserver region delete <name>");
            return true;
        }

        String name = args[0];
        boolean success = plugin.getRegionManager().deleteRegion(name);
        
        if (success) {
            MessageUtil.send(sender, "&aRegion '" + name + "' deleted successfully!");
        } else {
            MessageUtil.send(sender, "&cRegion '" + name + "' not found!");
        }

        return true;
    }

    private boolean handlePos1(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cThis command can only be used by players!");
            return true;
        }

        Location location = player.getLocation();
        plugin.getRegionManager().setPos1(player.getUniqueId(), location);
        MessageUtil.send(sender, "&aPosition 1 set to " + formatLocation(location));
        return true;
    }

    private boolean handlePos2(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cThis command can only be used by players!");
            return true;
        }

        Location location = player.getLocation();
        plugin.getRegionManager().setPos2(player.getUniqueId(), location);
        MessageUtil.send(sender, "&aPosition 2 set to " + formatLocation(location));
        return true;
    }

    private boolean handleRegionList(CommandSender sender) {
        var regions = plugin.getRegionManager().getAllRegions();
        
        if (regions.isEmpty()) {
            MessageUtil.send(sender, "&eNo regions defined.");
            return true;
        }

        MessageUtil.send(sender, "&6=== Regions (" + regions.size() + ") ===");
        for (Region region : regions) {
            MessageUtil.send(sender, "&e- " + region.getName() + " &7(" + region.getType() + ", priority: " + region.getPriority() + ")");
        }

        return true;
    }

    private boolean handleRegionInfo(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(sender, "&eUsage: /boxserver region info <name>");
            return true;
        }

        String name = args[0];
        Region region = plugin.getRegionManager().getRegion(name);

        if (region == null) {
            MessageUtil.send(sender, "&cRegion '" + name + "' not found!");
            return true;
        }

        MessageUtil.send(sender, "&6=== Region: " + region.getName() + " ===");
        MessageUtil.send(sender, "&eType: &f" + region.getType());
        MessageUtil.send(sender, "&ePriority: &f" + region.getPriority());
        MessageUtil.send(sender, "&ePvP Enabled: &f" + region.isPvpEnabled());
        MessageUtil.send(sender, "&eBounds: &f(" + region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ() + 
                ") to (" + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ() + ")");
        
        if (!region.getWhitelistedBlocks().isEmpty()) {
            MessageUtil.send(sender, "&eWhitelisted Blocks: &f" + region.getWhitelistedBlocks().size() + " blocks");
        }

        return true;
    }

    private boolean handleRegionPriority(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&eUsage: /boxserver region priority <name> <priority>");
            return true;
        }

        String name = args[0];
        int priority;
        try {
            priority = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cPriority must be a number!");
            return true;
        }

        Region region = plugin.getRegionManager().getRegion(name);
        if (region == null) {
            MessageUtil.send(sender, "&cRegion '" + name + "' not found!");
            return true;
        }

        region.setPriority(priority);
        plugin.getRegionManager().saveRegions();
        MessageUtil.send(sender, "&aPriority for region '" + name + "' set to " + priority);

        return true;
    }

    private boolean handleBlocksCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("boxserver.command.blocks")) {
            MessageUtil.send(sender, "&cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(sender, "&eUsage: /boxserver blocks <add|remove|list|clear> <region> [material]");
            return true;
        }

        String action = args[0].toLowerCase();

        return switch (action) {
            case "add" -> handleBlocksAdd(sender, Arrays.copyOfRange(args, 1, args.length));
            case "remove" -> handleBlocksRemove(sender, Arrays.copyOfRange(args, 1, args.length));
            case "list" -> handleBlocksList(sender, Arrays.copyOfRange(args, 1, args.length));
            case "clear" -> handleBlocksClear(sender, Arrays.copyOfRange(args, 1, args.length));
            default -> {
                MessageUtil.send(sender, "&cUnknown blocks command: " + action);
                yield true;
            }
        };
    }

    private boolean handleBlocksAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&eUsage: /boxserver blocks add <region> <material>");
            return true;
        }

        String regionName = args[0];
        String materialName = args[1].toUpperCase();

        Region region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            MessageUtil.send(sender, "&cRegion '" + regionName + "' not found!");
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, "&cInvalid material: " + materialName);
            return true;
        }

        region.addWhitelistedBlock(material);
        plugin.getRegionManager().saveRegions();
        MessageUtil.send(sender, "&aAdded " + material.name() + " to whitelist for region '" + regionName + "'");

        return true;
    }

    private boolean handleBlocksRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&eUsage: /boxserver blocks remove <region> <material>");
            return true;
        }

        String regionName = args[0];
        String materialName = args[1].toUpperCase();

        Region region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            MessageUtil.send(sender, "&cRegion '" + regionName + "' not found!");
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, "&cInvalid material: " + materialName);
            return true;
        }

        region.removeWhitelistedBlock(material);
        plugin.getRegionManager().saveRegions();
        MessageUtil.send(sender, "&aRemoved " + material.name() + " from whitelist for region '" + regionName + "'");

        return true;
    }

    private boolean handleBlocksList(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(sender, "&eUsage: /boxserver blocks list <region>");
            return true;
        }

        String regionName = args[0];
        Region region = plugin.getRegionManager().getRegion(regionName);
        
        if (region == null) {
            MessageUtil.send(sender, "&cRegion '" + regionName + "' not found!");
            return true;
        }

        var blocks = region.getWhitelistedBlocks();
        if (blocks.isEmpty()) {
            MessageUtil.send(sender, "&eNo whitelisted blocks for region '" + regionName + "'");
            return true;
        }

        MessageUtil.send(sender, "&6=== Whitelisted Blocks for " + regionName + " ===");
        for (Material material : blocks) {
            MessageUtil.send(sender, "&e- " + material.name());
        }

        return true;
    }

    private boolean handleBlocksClear(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.send(sender, "&eUsage: /boxserver blocks clear <region>");
            return true;
        }

        String regionName = args[0];
        Region region = plugin.getRegionManager().getRegion(regionName);
        
        if (region == null) {
            MessageUtil.send(sender, "&cRegion '" + regionName + "' not found!");
            return true;
        }

        region.clearWhitelistedBlocks();
        plugin.getRegionManager().saveRegions();
        MessageUtil.send(sender, "&aCleared all whitelisted blocks for region '" + regionName + "'");

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("boxserver.command.reload")) {
            MessageUtil.send(sender, "&cYou don't have permission to use this command!");
            return true;
        }

        plugin.reloadConfig();
        plugin.getRegionManager().reload();
        plugin.getBlockTracker().reload();
        plugin.getResetManager().reload();

        MessageUtil.send(sender, "&aConfiguration reloaded!");
        return true;
    }

    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("boxserver.command.reset")) {
            MessageUtil.send(sender, "&cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.send(sender, "&eUsage: /boxserver reset <region>");
            return true;
        }

        String regionName = args[0];
        Region region = plugin.getRegionManager().getRegion(regionName);
        
        if (region == null) {
            MessageUtil.send(sender, "&cRegion '" + regionName + "' not found!");
            return true;
        }

        int count = plugin.getResetManager().resetRegion(region);
        MessageUtil.send(sender, "&aReset " + count + " blocks in region '" + regionName + "'");

        return true;
    }

    private boolean handleSetResetTimeCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("boxserver.command.reload")) {
            MessageUtil.send(sender, "&cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.send(sender, "&eUsage: /boxserver setresettime <minutes>");
            return true;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cMinutes must be a number!");
            return true;
        }

        if (minutes < 1) {
            MessageUtil.send(sender, "&cMinutes must be at least 1!");
            return true;
        }

        plugin.getResetManager().setResetInterval(minutes);
        MessageUtil.send(sender, "&aReset interval set to " + minutes + " minutes");

        return true;
    }

    private String formatLocation(Location location) {
        return String.format("(%d, %d, %d)", 
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ());
    }
}
