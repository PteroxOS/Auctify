package dev.auctify.hook;

import dev.auctify.Auctify;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for Auctify.
 * Provides placeholders like %auctify_total_listings%, %auctify_player_listings%, etc.
 */
public class AuctifyPlaceholderExpansion extends PlaceholderExpansion {

    private final Auctify plugin;

    public AuctifyPlaceholderExpansion(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "auctify";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jephyruu";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Keep registered across /papi reload
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Global placeholders (no player needed)
        switch (params.toLowerCase()) {
            case "total_listings":
                return String.valueOf(plugin.getAuctionManager().getActiveListings().size());
            case "total_active":
                return String.valueOf(plugin.getAuctionManager().getActiveListings().stream()
                        .filter(l -> l.isActive() && !l.isExpired())
                        .count());
        }

        // Player-specific placeholders
        if (player == null) return "";
        var uuid = player.getUniqueId();

        switch (params.toLowerCase()) {
            case "player_listings":
                return String.valueOf(plugin.getAuctionManager().getActiveListings().stream()
                        .filter(l -> l.getSellerUUID().equals(uuid) && l.isActive())
                        .count());
            case "player_balance":
                return plugin.getEconomyManager().format(
                        plugin.getEconomyManager().getBalance(uuid));
            case "player_name":
                return player.getName() != null ? player.getName() : "Unknown";
            default:
                return null; // Unknown placeholder
        }
    }
}
