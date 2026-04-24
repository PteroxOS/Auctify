package dev.auctify.util;

import dev.auctify.Auctify;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manages per-world auction house settings and restrictions. Supports: global
 * mode, per-world mode, and blacklist mode.
 */
public class WorldManager {

    private final Auctify plugin;

    public WorldManager(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Checks if a player can use auction house commands in their current world. */
    public boolean canUseAuctionHouse(Player player) {
        if (player.hasPermission("auctify.admin")) {
            return true; // Admins bypass world restrictions
        }

        String mode = plugin.getConfig().getString("worlds.mode", "global");
        String worldName = player.getWorld().getName();

        return switch (mode.toLowerCase()) {
            case "blacklist" -> !isBlacklistedWorld(worldName);
            case "per-world" -> true; // Per-world allows usage, listings are filtered by world
            case "global" -> true;
            default -> true;
        };
    }

    /** Checks if a world is blacklisted. */
    public boolean isBlacklistedWorld(String worldName) {
        List<String> blacklisted = plugin.getConfig().getStringList("worlds.blacklisted-worlds");
        return blacklisted.contains(worldName);
    }

    /**
     * Gets the auction scope for a world (for per-world mode). Returns world name
     * or "global" if world is in global-worlds list.
     */
    public String getWorldScope(World world) {
        String mode = plugin.getConfig().getString("worlds.mode", "global");

        if (mode.equalsIgnoreCase("global")) {
            return "global";
        }

        if (mode.equalsIgnoreCase("per-world")) {
            List<String> globalWorlds = plugin.getConfig().getStringList("worlds.global-worlds");
            if (globalWorlds.contains(world.getName())) {
                return "global";
            }
            return world.getName(); // Per-world scope
        }

        return "global";
    }

    /**
     * Checks if two worlds share the same auction house (for cross-world bidding).
     */
    public boolean shareAuctionHouse(World world1, World world2) {
        String mode = plugin.getConfig().getString("worlds.mode", "global");

        if (mode.equalsIgnoreCase("global")) {
            return true;
        }

        String scope1 = getWorldScope(world1);
        String scope2 = getWorldScope(world2);

        return scope1.equals(scope2);
    }

    /** Gets the current world mode. */
    public String getMode() {
        return plugin.getConfig().getString("worlds.mode", "global");
    }
}
