package dev.auctify.hook;

import dev.auctify.Auctify;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PlaceholderAPI Expansion for Auctify.
 * Provides placeholders for auction statistics and leaderboards.
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final Auctify plugin;

    public PlaceholderAPIHook(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "auctify";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "PteroxOS";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the expansion on
                     // reload
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("top_seller_")) {
            // %auctify_top_seller_1%, %auctify_top_seller_2%, etc.
            try {
                int rank = Integer.parseInt(params.substring("top_seller_".length()));
                return getTopSeller(rank);
            } catch (NumberFormatException e) {
                return "Invalid rank";
            }
        }

        if (params.startsWith("top_bidder_")) {
            // %auctify_top_bidder_1%, %auctify_top_bidder_2%, etc.
            try {
                int rank = Integer.parseInt(params.substring("top_bidder_".length()));
                return getTopBidder(rank);
            } catch (NumberFormatException e) {
                return "Invalid rank";
            }
        }

        if (params.equals("active_listings")) {
            return String.valueOf(plugin.getAuctionManager().getActiveListings().size());
        }

        if (params.equals("total_listings")) {
            return String.valueOf(plugin.getAuctionManager().getActiveListings().size());
        }

        if (player == null) {
            return null;
        }

        // Player-specific placeholders
        UUID playerUUID = player.getUniqueId();

        if (params.equals("player_listings")) {
            long count = plugin.getAuctionManager().getActiveListings().stream()
                    .filter(l -> l.getSellerUUID().equals(playerUUID) && l.isActive())
                    .count();
            return String.valueOf(count);
        }

        if (params.equals("player_active_bids")) {
            long count = plugin.getAuctionManager().getActiveListings().stream()
                    .filter(l -> l.hasBids() && l.getTopBidderUUID().equals(playerUUID))
                    .count();
            return String.valueOf(count);
        }

        if (params.equals("player_total_sold")) {
            // This would need history data - return placeholder for now
            return "0";
        }

        return null;
    }

    private String getTopSeller(int rank) {
        var listings = plugin.getAuctionManager().getActiveListings();

        Map<String, Long> sellerCounts = listings.stream()
                .filter(l -> l.isActive())
                .collect(Collectors.groupingBy(
                        l -> l.getSellerName(),
                        Collectors.counting()));

        return sellerCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .skip(rank - 1)
                .findFirst()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .orElse("No data");
    }

    private String getTopBidder(int rank) {
        var listings = plugin.getAuctionManager().getActiveListings();

        Map<String, Long> bidderCounts = listings.stream()
                .filter(l -> l.hasBids() && l.getTopBidderName() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getTopBidderName(),
                        Collectors.counting()));

        return bidderCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .skip(rank - 1)
                .findFirst()
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .orElse("No data");
    }
}
