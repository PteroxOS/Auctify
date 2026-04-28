package dev.auctify.util;

import dev.auctify.Auctify;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized error handling with user-friendly messages.
 * Provides consistent error formatting and logging.
 */
public class ErrorHandler {

    private final Auctify plugin;

    public ErrorHandler(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles an economy-related error and sends a user-friendly message.
     */
    public void handleEconomyError(Player player, String operation, Exception e) {
        plugin.getLogger().warning("§c[Economy] Error during " + operation + " for " + player.getName() + ": " + e.getMessage());
        
        Map<String, String> context = new HashMap<>();
        context.put("operation", operation);
        context.put("error", e.getClass().getSimpleName());
        
        MessageUtil.send(player, "economy-error", context);
    }

    /**
     * Handles a storage-related error.
     */
    public void handleStorageError(String operation, Exception e) {
        plugin.getLogger().severe("§c[Storage] Error during " + operation + ": " + e.getMessage());
        plugin.getLogger().severe("§c[Storage] Stack trace:");
        for (StackTraceElement element : e.getStackTrace()) {
            plugin.getLogger().severe("  " + element.toString());
        }
    }

    /**
     * Handles a GUI-related error.
     */
    public void handleGUIError(Player player, String guiType, Exception e) {
        plugin.getLogger().warning("§c[GUI] Error in " + guiType + " for " + player.getName() + ": " + e.getMessage());
        
        player.closeInventory();
        MessageUtil.send(player, "gui-error", Map.of("type", guiType));
    }

    /**
     * Handles a permission error.
     */
    public void handlePermissionError(Player player, String permission) {
        plugin.getLogger().warning("§c[Permission] " + player.getName() + " attempted to use permission: " + permission);
        MessageUtil.send(player, "no-permission", Map.of("permission", permission));
    }

    /**
     * Handles an invalid command argument error.
     */
    public void handleArgumentError(Player player, String argument, String expected) {
        MessageUtil.send(player, "invalid-argument", Map.of(
            "argument", argument,
            "expected", expected
        ));
    }

    /**
     * Logs a successful operation with context.
     */
    public void logSuccess(String operation, Map<String, String> context) {
        StringBuilder sb = new StringBuilder("§a[Success] ").append(operation);
        if (!context.isEmpty()) {
            sb.append(" - ");
            context.forEach((key, value) -> sb.append(key).append("=").append(value).append(", "));
            sb.setLength(sb.length() - 2); // Remove trailing comma
        }
        plugin.getLogger().info(sb.toString());
    }

    /**
     * Logs a warning with context.
     */
    public void logWarning(String operation, String message, Map<String, String> context) {
        StringBuilder sb = new StringBuilder("§e[Warning] ").append(operation).append(": ").append(message);
        if (!context.isEmpty()) {
            sb.append(" - ");
            context.forEach((key, value) -> sb.append(key).append("=").append(value).append(", "));
            sb.setLength(sb.length() - 2);
        }
        plugin.getLogger().warning(sb.toString());
    }
}
