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
 * Handles the /ac bidhistory command.
 * Usage: /ac bidhistory <id>
 * Shows bid history for a specific auction listing.
 */
public class BidHistorySubCommand implements SubCommand {

    private final Auctify plugin;

    public BidHistorySubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("auctify.bid")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendRaw(player, "§cUsage: §f/ac bidhistory <listing_id>");
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

        List<BidRecord> bids = plugin.getStorageManager().getBidHistory(listingId);

        // Header
        MessageUtil.sendRaw(player, "§8|================================================");
        MessageUtil.sendRaw(player, "§8|  §6§lBID HISTORY §7— §e" + listingId);
        MessageUtil.sendRaw(player, "§8|================================================");
        MessageUtil.sendRaw(player, "§8|  §7Item: §f" + listing.getItem().getType().name());
        MessageUtil.sendRaw(player, "§8|  §7Seller: §f" + listing.getSellerName());
        MessageUtil.sendRaw(player, "§8|================================================");

        if (bids.isEmpty()) {
            MessageUtil.sendRaw(player, "§8|  §7No bids placed yet.");
        } else {
            MessageUtil.sendRaw(player, "§8|  §7Total bids: §f" + bids.size());
            MessageUtil.sendRaw(player, "§8|                                                ");

            int count = 0;
            for (BidRecord bid : bids) {
                if (count++ >= 10) { // Show max 10 bids
                    MessageUtil.sendRaw(player, "§8|  §8... and " + (bids.size() - 10) + " more");
                    break;
                }

                String timeAgo = formatTimeAgo(bid.timestamp());
                MessageUtil.sendRaw(player, "§8|  §f" + (count) + ". §e" + bid.bidderName() +
                        " §7bid §a" + plugin.getEconomyManager().format(bid.amount()) +
                        " §8(" + timeAgo + ")");
            }
        }

        MessageUtil.sendRaw(player, "§8|================================================");

        // Show current status
        if (listing.hasBids()) {
            MessageUtil.sendRaw(player, "§8|  §6Current top bid: §a" +
                    plugin.getEconomyManager().format(listing.getCurrentBid()) +
                    " §7by §e" + listing.getTopBidderName());
        } else {
            MessageUtil.sendRaw(player, "§8|  §7Starting price: §a" +
                    plugin.getEconomyManager().format(listing.getStartPrice()));
        }
        MessageUtil.sendRaw(player, "§8|================================================");
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
