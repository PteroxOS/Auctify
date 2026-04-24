package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.auction.AutoBid;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/** Handles the /ac autobid command for managing auto-bid configurations. */
public class AutoBidSubCommand implements SubCommand {

    private final Auctify plugin;

    public AutoBidSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.autobid")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length == 1) {
            showStatus(player);
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("set")) {
            if (args.length < 4) {
                MessageUtil.send(player, "autobid-usage", null);
                return;
            }

            String listingId = args[2];
            double maxAmount;
            try {
                maxAmount = Double.parseDouble(args[3]);
                if (maxAmount <= 0 || maxAmount > Double.MAX_VALUE / 2) {
                    MessageUtil.send(player, "invalid-number", Map.of("input", args[3]));
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "invalid-number", Map.of("input", args[3]));
                return;
            }

            setAutoBid(player, listingId, maxAmount);
        } else if (action.equals("remove")) {
            if (args.length < 3) {
                MessageUtil.send(player, "autobid-usage", null);
                return;
            }

            String listingId = args[2];
            removeAutoBid(player, listingId);
        } else if (action.equals("clear")) {
            clearAutoBids(player);
        } else {
            MessageUtil.send(player, "autobid-usage", null);
        }
    }

    private void showStatus(Player player) {
        java.util.List<AutoBid> autoBids = plugin.getStorageManager().getAutoBidsForPlayer(player.getUniqueId());

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§6§l✦ Auto-Bid Status");
        MessageUtil.sendRaw(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (autoBids.isEmpty()) {
            MessageUtil.sendRaw(player, " §7You have no active auto-bids.");
        } else {
            for (AutoBid autoBid : autoBids) {
                AuctionListing listing = plugin.getAuctionManager().getActiveListings().stream()
                        .filter(l -> l.getId().equals(autoBid.getListingId()))
                        .findFirst()
                        .orElse(null);
                if (listing != null && listing.isActive() && !listing.isExpired()) {
                    double currentBid = listing.getCurrentBid();
                    double remaining = autoBid.getMaxBidAmount() - currentBid;
                    MessageUtil.sendRaw(player, " §7" + autoBid.getListingId() + ": §a"
                            + plugin.getAuctionManager().getEconomy().format(autoBid.getMaxBidAmount()) + " §7(§e"
                            + plugin.getAuctionManager().getEconomy().format(remaining) + " remaining§7)");
                }
            }
        }

        MessageUtil.sendRaw(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendRaw(player, " §e/ac autobid set <id> <max> §7- Set auto-bid for listing");
        MessageUtil.sendRaw(player, " §e/ac autobid remove <id> §7- Remove auto-bid");
        MessageUtil.sendRaw(player, " §e/ac autobid clear §7- Clear all auto-bids");
        MessageUtil.sendRaw(player, "");
    }

    private void setAutoBid(Player player, String listingId, double maxAmount) {
        AuctionListing listing = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.getId().equals(listingId))
                .findFirst()
                .orElse(null);
        if (listing == null) {
            MessageUtil.send(player, "listing-not-found", Map.of("id", listingId));
            return;
        }

        if (!listing.isActive() || listing.isExpired()) {
            MessageUtil.send(player, "listing-not-active", null);
            return;
        }

        if (listing.getSellerUUID().equals(player.getUniqueId())) {
            MessageUtil.send(player, "autobid-own-listing", null);
            return;
        }

        if (maxAmount <= listing.getCurrentBid()) {
            MessageUtil.send(player, "autobid-too-low",
                    Map.of("current", plugin.getAuctionManager().getEconomy().format(listing.getCurrentBid())));
            return;
        }

        AutoBid autoBid = new AutoBid(player.getUniqueId(), player.getName(), listingId, maxAmount);
        plugin.getStorageManager().saveAutoBid(autoBid);

        MessageUtil.send(player, "autobid-set", Map.of(
                "id", listingId,
                "max", plugin.getAuctionManager().getEconomy().format(maxAmount)));
    }

    private void removeAutoBid(Player player, String listingId) {
        AutoBid existing = plugin.getStorageManager().getAutoBid(listingId, player.getUniqueId());
        if (existing == null) {
            MessageUtil.send(player, "autobid-not-found", Map.of("id", listingId));
            return;
        }

        plugin.getStorageManager().deleteAutoBid(listingId, player.getUniqueId());
        MessageUtil.send(player, "autobid-removed", Map.of("id", listingId));
    }

    private void clearAutoBids(Player player) {
        plugin.getStorageManager().clearAutoBidsForPlayer(player.getUniqueId());
        MessageUtil.send(player, "autobid-cleared", null);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
