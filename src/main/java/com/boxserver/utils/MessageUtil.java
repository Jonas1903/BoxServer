package com.boxserver.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Utility class for handling messages with color code support.
 */
public class MessageUtil {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
            LegacyComponentSerializer.legacyAmpersand();

    /**
     * Send a colored message to a command sender.
     */
    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        Component component = LEGACY_SERIALIZER.deserialize(message);
        sender.sendMessage(component);
    }

    /**
     * Convert a message with & color codes to a Component.
     */
    public static Component colorize(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(message);
    }

    /**
     * Replace placeholders in a message.
     */
    public static String replacePlaceholders(String message, String... replacements) {
        if (message == null || replacements == null || replacements.length == 0 || replacements.length % 2 != 0) {
            return message;
        }
        String result = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }
}
