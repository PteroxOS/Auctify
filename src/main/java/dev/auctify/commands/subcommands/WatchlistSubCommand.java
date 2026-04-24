package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Handles the /ac watchlist command. Shows and manages player's watchlisted
 * auctions.
 */
public class WatchlistSubCommand implements SubCommand {

    private final Auctify plugin;

    public WatchlistSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (!player.hasPermission("auctify.watchlist")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        // Toggle watch: /ac watchlist <listing_id>
        if (args.length >= 2) {
            String listingId = args[1].toUpperCase();
            AuctionListing listing = findListing(listingId);

            if (listing == null) {
                MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId));
                return;
            }

            if (plugin.getStorageManager().isInWatchlist(player.getUniqueId(), listingId)) {
                plugin.getStorageManager().removeFromWatchlist(player.getUniqueId(), listingId);
                MessageUtil.send(player, "watchlist-removed", Map.of("id", listingId));
            } else {
                plugin.getStorageManager().addToWatchlist(player.getUniqueId(), listingId);
                String itemName = ItemUtil.getDisplayName(listing.getItem());
                MessageUtil.send(player, "watchlist-added", Map.of("id", listingId, "item", itemName));
            }
            return;
        }

        // Show watchlist
        List<String> watchlist = plugin.getStorageManager().getWatchlist(player.getUniqueId());
        if (watchlist.isEmpty()) {
            MessageUtil.send(player, "watchlist-empty", null);
            return;
        }

        MessageUtil.send(player, "watchlist-header", Map.of("count", String.valueOf(watchlist.size())));
        for (String listingId : watchlist) {
            AuctionListing listing = findListing(listingId);
            if (listing != null && listing.isActive() && !listing.isExpired()) {
                String itemName = ItemUtil.getDisplayName(listing.getItem());
                String currentBid = plugin.getEconomyManager().format(listing.getCurrentBid());
                MessageUtil.send(player, "watchlist-entry", Map.of(
                        "id", listingId,
                        "item", itemName,
                        "bid", currentBid));
            } else {
                // Auto-remove expired/ended listings
                plugin.getStorageManager().removeFromWatchlist(player.getUniqueId(), listingId);
            }
        }
    }

    private AuctionListing findListing(String listingId) {
        return plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.getId().equalsIgnoreCase(listingId))
                .findFirst().orElse(null);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
