package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.auction.BidRecord;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Handles the /ac bidhistory command. Usage: /ac bidhistory <id>. Shows bid
 * history for a specific auction listing.
 */
public class BidHistorySubCommand implements SubCommand {

    private final Auctify plugin;

    public BidHistorySubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("auctify.bid")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length < 2) {
            // Show player's own bid history
            showPlayerBidHistory(player);
            return;
        }

        String listingId = args[1].toUpperCase();
        AuctionListing listing = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.getId().equalsIgnoreCase(listingId))
                .findFirst().orElse(null);

        if (listing == null) {
            MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId));
            return;
        }

        showListingBidHistory(player, listing);
    }

    private void showListingBidHistory(Player player, AuctionListing listing) {
        String listingId = listing.getId();
        List<BidRecord> bids = plugin.getStorageManager().getBidHistory(listingId);

        // Header
        MessageUtil.send(player, "bidhistory-header", null);
        MessageUtil.send(player, "bidhistory-title", Map.of("id", listingId));
        MessageUtil.send(player, "bidhistory-header", null);
        MessageUtil.send(player, "bidhistory-item", Map.of("item", listing.getItem().getType().name()));
        MessageUtil.send(player, "bidhistory-seller", Map.of("seller", listing.getSellerName()));
        MessageUtil.send(player, "bidhistory-header", null);

        if (bids.isEmpty()) {
            MessageUtil.send(player, "bidhistory-no-bids", null);
        } else {
            MessageUtil.send(player, "bidhistory-total", Map.of("count", String.valueOf(bids.size())));

            // Calculate statistics
            double avgBid = bids.stream().mapToDouble(BidRecord::amount).average().orElse(0);
            double maxBid = bids.stream().mapToDouble(BidRecord::amount).max().orElse(0);
            double minBid = bids.stream().mapToDouble(BidRecord::amount).min().orElse(0);

            MessageUtil.send(player, "bidhistory-stats", Map.of(
                    "avg", plugin.getEconomyManager().format(avgBid),
                    "max", plugin.getEconomyManager().format(maxBid),
                    "min", plugin.getEconomyManager().format(minBid)));
            MessageUtil.send(player, "bidhistory-header", null);

            int count = 0;
            for (BidRecord bid : bids) {
                if (count++ >= 10) { // Show max 10 bids
                    MessageUtil.send(player, "bidhistory-more", Map.of("count", String.valueOf(bids.size() - 10)));
                    break;
                }

                String timeAgo = formatTimeAgo(bid.timestamp());
                MessageUtil.send(player, "bidhistory-entry", Map.of(
                        "number", String.valueOf(count),
                        "bidder", bid.bidderName(),
                        "amount", plugin.getEconomyManager().format(bid.amount()),
                        "time", timeAgo));
            }
        }

        MessageUtil.send(player, "bidhistory-footer", null);

        // Show current status
        if (listing.hasBids()) {
            MessageUtil.send(player, "bidhistory-current-top", Map.of(
                    "amount", plugin.getEconomyManager().format(listing.getCurrentBid()),
                    "bidder", listing.getTopBidderName()));
        } else {
            MessageUtil.send(player, "bidhistory-starting-price", Map.of(
                    "price", plugin.getEconomyManager().format(listing.getStartPrice())));
        }
        MessageUtil.send(player, "bidhistory-footer", null);
    }

    private void showPlayerBidHistory(Player player) {
        List<BidRecord> allBids = plugin.getStorageManager().getPlayerBidHistory(player.getUniqueId());

        if (allBids.isEmpty()) {
            MessageUtil.send(player, "bidhistory-player-empty", null);
            return;
        }

        MessageUtil.send(player, "bidhistory-player-header", Map.of("player", player.getName()));
        MessageUtil.send(player, "bidhistory-player-total", Map.of("count", String.valueOf(allBids.size())));

        // Calculate statistics
        double totalSpent = allBids.stream().mapToDouble(BidRecord::amount).sum();
        double avgBid = allBids.stream().mapToDouble(BidRecord::amount).average().orElse(0);

        MessageUtil.send(player, "bidhistory-player-stats", Map.of(
                "total", plugin.getEconomyManager().format(totalSpent),
                "avg", plugin.getEconomyManager().format(avgBid)));
        MessageUtil.send(player, "bidhistory-header", null);

        // Show recent bids (last 10)
        int count = 0;
        for (BidRecord bid : allBids) {
            if (count++ >= 10) {
                MessageUtil.send(player, "bidhistory-more", Map.of("count", String.valueOf(allBids.size() - 10)));
                break;
            }

            String timeAgo = formatTimeAgo(bid.timestamp());
            MessageUtil.send(player, "bidhistory-player-entry", Map.of(
                    "number", String.valueOf(count),
                    "amount", plugin.getEconomyManager().format(bid.amount()),
                    "time", timeAgo));
        }
    }

    private String formatTimeAgo(long timestamp) {
        long seconds = (System.currentTimeMillis() - timestamp) / 1000;
        if (seconds < 60)
            return seconds + "s ago";
        if (seconds < 3600)
            return (seconds / 60) + "m ago";
        return (seconds / 3600) + "h ago";
    }
}
