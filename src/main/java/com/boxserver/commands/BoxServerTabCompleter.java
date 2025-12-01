package com.boxserver.commands;

import com.boxserver.BoxServer;
import com.boxserver.models.Region;
import com.boxserver.models.RegionType;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer for BoxServer commands.
 */
public class BoxServerTabCompleter implements org.bukkit.command.TabCompleter {
    private final BoxServer plugin;

    private static final List<String> MAIN_COMMANDS = Arrays.asList("region", "blocks", "reload", "reset", "setresettime");
    private static final List<String> REGION_SUBCOMMANDS = Arrays.asList("create", "delete", "pos1", "pos2", "list", "info", "priority");
    private static final List<String> BLOCKS_SUBCOMMANDS = Arrays.asList("add", "remove", "list", "clear");
    private static final List<String> REGION_TYPES = Arrays.stream(RegionType.values())
            .map(t -> t.name().toLowerCase())
            .collect(Collectors.toList());

    public BoxServerTabCompleter(BoxServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return filterStartsWith(MAIN_COMMANDS, args[0]);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "region" -> completions = completeRegionCommand(sender, args);
            case "blocks" -> completions = completeBlocksCommand(sender, args);
            case "reset" -> {
                if (args.length == 2) {
                    completions = filterStartsWith(getRegionNames(), args[1]);
                }
            }
            case "setresettime" -> {
                if (args.length == 2) {
                    completions = Arrays.asList("5", "10", "15", "20", "30");
                }
            }
        }

        return completions;
    }

    private List<String> completeRegionCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("boxserver.command.region")) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return filterStartsWith(REGION_SUBCOMMANDS, args[1]);
        }

        String action = args[1].toLowerCase();

        return switch (action) {
            case "create" -> {
                if (args.length == 4) {
                    yield filterStartsWith(REGION_TYPES, args[3]);
                }
                yield new ArrayList<>();
            }
            case "delete", "info", "priority" -> {
                if (args.length == 3) {
                    yield filterStartsWith(getRegionNames(), args[2]);
                }
                yield new ArrayList<>();
            }
            default -> new ArrayList<>();
        };
    }

    private List<String> completeBlocksCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("boxserver.command.blocks")) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return filterStartsWith(BLOCKS_SUBCOMMANDS, args[1]);
        }

        String action = args[1].toLowerCase();

        if (args.length == 3) {
            return filterStartsWith(getRegionNames(), args[2]);
        }

        if (args.length == 4 && (action.equals("add") || action.equals("remove"))) {
            // Filter materials to common blocks for easier selection
            List<String> materials = Arrays.stream(Material.values())
                    .filter(Material::isBlock)
                    .map(m -> m.name().toLowerCase())
                    .collect(Collectors.toList());
            return filterStartsWith(materials, args[3]);
        }

        return new ArrayList<>();
    }

    private List<String> getRegionNames() {
        return plugin.getRegionManager().getAllRegions().stream()
                .map(Region::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
