package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bulk buy subcommand - allows players to buy multiple items at once.
 */
public class BulkBuySubCommand implements SubCommand {

    private final Auctify plugin;

    public BulkBuySubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "bulkbuy-usage", null);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "add" -> handleAdd(player, args);
            case "remove" -> handleRemove(player, args);
            case "list" -> handleList(player);
            case "clear" -> handleClear(player);
            case "buy" -> handleBuy(player);
            default -> MessageUtil.send(player, "bulkbuy-usage", null);
        }
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "bulkbuy-add-usage", null);
            return;
        }

        String listingId = args[2];
        plugin.getAuctionManager().getListingById(listingId).ifPresentOrElse(listing -> {
            if (listing.isExpired() || !listing.isActive()) {
                MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId));
                return;
            }

            if (listing.getSellerUUID().equals(player.getUniqueId())) {
                MessageUtil.send(player, "bulkbuy-own-listing", null);
                return;
            }

            // Check if already in bulk buy list
            List<String> bulkBuyList = plugin.getGUIManager().getBulkBuyList(player.getUniqueId());
            if (bulkBuyList.contains(listingId)) {
                MessageUtil.send(player, "bulkbuy-already-added", null);
                return;
            }

            // Add to bulk buy list
            bulkBuyList.add(listingId);
            plugin.getGUIManager().setBulkBuyList(player.getUniqueId(), bulkBuyList);
            MessageUtil.send(player, "bulkbuy-added",
                    Map.of("item", listing.getItem().getType().name(), "id", listingId));
        }, () -> MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId)));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "bulkbuy-remove-usage", null);
            return;
        }

        String listingId = args[2];
        List<String> bulkBuyList = plugin.getGUIManager().getBulkBuyList(player.getUniqueId());
        if (bulkBuyList.remove(listingId)) {
            plugin.getGUIManager().setBulkBuyList(player.getUniqueId(), bulkBuyList);
            MessageUtil.send(player, "bulkbuy-removed", Map.of("id", listingId));
        } else {
            MessageUtil.send(player, "bulkbuy-not-in-list", Map.of("id", listingId));
        }
    }

    private void handleList(Player player) {
        List<String> bulkBuyList = plugin.getGUIManager().getBulkBuyList(player.getUniqueId());
        if (bulkBuyList.isEmpty()) {
            MessageUtil.send(player, "bulkbuy-empty", null);
            return;
        }

        final double[] totalCost = { 0 };
        final int[] itemCount = { 0 };
        MessageUtil.send(player, "bulkbuy-list-header", Map.of("count", String.valueOf(bulkBuyList.size())));

        for (String listingId : bulkBuyList) {
            plugin.getAuctionManager().getListingById(listingId).ifPresent(listing -> {
                double price = listing.getBuyoutPrice() > 0 ? listing.getBuyoutPrice() : listing.getCurrentBid();
                MessageUtil.send(player, "bulkbuy-list-entry", Map.of(
                        "id", listingId,
                        "item", listing.getItem().getType().name(),
                        "price", plugin.getEconomyManager().format(price)));
            });
        }

        // Calculate total
        for (String listingId : bulkBuyList) {
            plugin.getAuctionManager().getListingById(listingId).ifPresent(listing -> {
                double price = listing.getBuyoutPrice() > 0 ? listing.getBuyoutPrice() : listing.getCurrentBid();
                totalCost[0] += price;
                itemCount[0] += listing.getItem().getAmount();
            });
        }

        MessageUtil.send(player, "bulkbuy-total", Map.of(
                "total", plugin.getEconomyManager().format(totalCost[0]),
                "items", String.valueOf(itemCount[0])));
    }

    private void handleClear(Player player) {
        plugin.getGUIManager().setBulkBuyList(player.getUniqueId(), new ArrayList<>());
        MessageUtil.send(player, "bulkbuy-cleared", null);
    }

    private void handleBuy(Player player) {
        List<String> bulkBuyList = plugin.getGUIManager().getBulkBuyList(player.getUniqueId());
        if (bulkBuyList.isEmpty()) {
            MessageUtil.send(player, "bulkbuy-empty", null);
            return;
        }

        // Calculate total cost
        final double[] totalCost = { 0 };
        List<AuctionListing> validListings = new ArrayList<>();

        for (String listingId : bulkBuyList) {
            plugin.getAuctionManager().getListingById(listingId).ifPresent(listing -> {
                if (!listing.isExpired() && listing.isActive()) {
                    double price = listing.getBuyoutPrice() > 0 ? listing.getBuyoutPrice() : listing.getCurrentBid();
                    totalCost[0] += price;
                    validListings.add(listing);
                }
            });
        }

        if (validListings.isEmpty()) {
            MessageUtil.send(player, "bulkbuy-no-valid", null);
            return;
        }

        // Check balance
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        if (balance < totalCost[0]) {
            MessageUtil.send(player, "insufficient-funds",
                    Map.of("amount", plugin.getEconomyManager().format(totalCost[0])));
            return;
        }

        // Check inventory space
        int emptySlots = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                emptySlots++;
            }
        }

        if (emptySlots < validListings.size()) {
            MessageUtil.send(player, "bulkbuy-inventory-full",
                    Map.of("needed", String.valueOf(validListings.size()), "available", String.valueOf(emptySlots)));
            return;
        }

        // Process purchases
        int bought = 0;
        for (AuctionListing listing : validListings) {
            double price = listing.getBuyoutPrice() > 0 ? listing.getBuyoutPrice() : listing.getCurrentBid();
            var result = plugin.getEconomyManager().withdraw(player.getUniqueId(), price);
            if (result.success()) {
                plugin.getAuctionManager().buyout(player, listing.getId());
                bought++;
            }
        }

        // Clear bulk buy list
        plugin.getGUIManager().setBulkBuyList(player.getUniqueId(), new ArrayList<>());
        MessageUtil.send(player, "bulkbuy-success",
                Map.of("count", String.valueOf(bought), "total", plugin.getEconomyManager().format(totalCost[0])));
    }
}
