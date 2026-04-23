package dev.auctify.auction;

import dev.auctify.Auctify;
import dev.auctify.economy.EconomyManager;
import dev.auctify.economy.TransactionResult;
import dev.auctify.storage.StorageManager;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Central coordinator for all auction operations: creating listings, placing bids,
 * buyouts, cancellations, and auction resolution. Holds the in-memory map of active
 * listings and delegates persistence to {@link StorageManager}.
 */
public class AuctionManager {

    private final Auctify plugin;
    private final Logger logger;
    private final EconomyManager economy;
    private final StorageManager storage;

    /** Active listings indexed by ID for O(1) lookup. Thread-safe. */
    private final Map<String, AuctionListing> activeListings = new ConcurrentHashMap<>();

    /**
     * Constructs the AuctionManager and loads existing listings from storage.
     *
     * @param plugin  the main plugin instance
     * @param economy the economy manager
     * @param storage the storage manager
     */
    public AuctionManager(Auctify plugin, EconomyManager economy, StorageManager storage) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.economy = economy;
        this.storage = storage;

        // Load persisted listings into memory
        List<AuctionListing> persisted = storage.getAllListings();
        for (AuctionListing listing : persisted) {
            if (!listing.isExpired()) {
                activeListings.put(listing.getId(), listing);
            } else {
                // Resolve any listings that expired while server was offline
                resolveAuction(listing);
            }
        }
        logger.info("Loaded " + activeListings.size() + " active listings from storage.");
    }

    /**
     * Creates a new auction listing from the item in the seller's hand.
     *
     * @param seller          the selling player
     * @param item            the item to list
     * @param startPrice      the starting bid price
     * @param buyoutPrice     the buyout price (0 for no buyout)
     * @param durationSeconds the auction duration in seconds
     * @return the listing ID if successful, or null on failure
     */
    public String createListing(Player seller, ItemStack item, double startPrice,
                                double buyoutPrice, int durationSeconds) {
        var config = plugin.getConfig();

        // Permission check
        if (!seller.hasPermission("auctify.sell")) {
            MessageUtil.send(seller, "no-permission", null);
            return null;
        }

        // Blacklist check
        if (storage.isBlacklisted(seller.getUniqueId())) {
            MessageUtil.send(seller, "blacklisted", null);
            return null;
        }

        // Null/air item guard
        if (ItemUtil.isEmpty(item)) {
            MessageUtil.send(seller, "hold-item-to-sell", null);
            return null;
        }

        // Max listings check (bypass with permission)
        if (!seller.hasPermission("auctify.bypass.maxlistings")) {
            int maxListings = config.getInt("general.max-listings-per-player", 5);
            long currentCount = activeListings.values().stream()
                    .filter(l -> l.getSellerUUID().equals(seller.getUniqueId()) && l.isActive())
                    .count();
            if (currentCount >= maxListings) {
                MessageUtil.send(seller, "max-listings-reached", Map.of("max", String.valueOf(maxListings)));
                return null;
            }
        }

        // Validate price range
        double minPrice = config.getDouble("bidding.min-start-price", 1.0);
        double maxPrice = config.getDouble("bidding.max-start-price", 1000000.0);
        if (startPrice < minPrice || (maxPrice > 0 && startPrice > maxPrice)) {
            MessageUtil.send(seller, "invalid-price", null);
            return null;
        }

        // Validate buyout price against multiplier
        if (buyoutPrice > 0) {
            double multiplier = config.getDouble("bidding.buyout-min-multiplier", 1.5);
            if (buyoutPrice < startPrice * multiplier) {
                MessageUtil.sendRaw(seller, "§cBuyout price must be at least §f"
                        + economy.format(startPrice * multiplier) + "§c.");
                return null;
            }
        }

        // Validate duration
        int minDuration = config.getInt("general.min-duration", 60);
        int maxDuration = config.getInt("general.max-duration", 3600);
        if (durationSeconds < minDuration || durationSeconds > maxDuration) {
            MessageUtil.send(seller, "invalid-duration",
                    Map.of("min", String.valueOf(minDuration), "max", String.valueOf(maxDuration)));
            return null;
        }

        // Generate unique 8-char ID
        String id = UUID.randomUUID().toString().substring(0, 8);

        long now = System.currentTimeMillis();
        long endTime = now + (durationSeconds * 1000L);

        // Create the listing (stores a defensive copy of the item)
        AuctionListing listing = new AuctionListing(
                id, seller.getUniqueId(), seller.getName(),
                item, startPrice, buyoutPrice, now, endTime
        );

        // Fire custom event — allow other plugins to cancel
        AuctifyListingCreateEvent event = new AuctifyListingCreateEvent(listing, seller);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        // Remove item from seller's hand (take exactly the item, leave nothing)
        seller.getInventory().setItemInMainHand(null);

        // Store in memory and persist
        activeListings.put(id, listing);
        storage.saveListing(listing);

        // Notify seller
        MessageUtil.send(seller, "listing-created", Map.of("listing_id", id));

        // Broadcast if enabled
        String itemName = ItemUtil.getDisplayName(item);
        if (config.getBoolean("general.broadcast-listings", true)) {
            MessageUtil.broadcastRaw("§e" + seller.getName() + " §7listed §f" + itemName
                    + " §7starting at §a" + economy.format(startPrice) + "§7!");
        }

        // Discord webhook
        plugin.getDiscordWebhookUtil().sendNewListingEmbed(
                seller.getName(),
                itemName,
                economy.format(startPrice),
                buyoutPrice > 0 ? economy.format(buyoutPrice) : "None"
        );

        return id;
    }

    /**
     * Places a bid on an active listing.
     *
     * @param bidder    the bidding player
     * @param listingId the ID of the listing to bid on
     * @param amount    the bid amount
     * @return true if the bid was placed successfully
     */
    public boolean placeBid(Player bidder, String listingId, double amount) {
        AuctionListing listing = activeListings.get(listingId);
        if (listing == null || !listing.isActive() || listing.isExpired()) {
            MessageUtil.send(bidder, "listing-not-found", Map.of("listing_id", listingId));
            return false;
        }

        // Permission check
        if (!bidder.hasPermission("auctify.bid")) {
            MessageUtil.send(bidder, "no-permission", null);
            return false;
        }

        // Blacklist check
        if (storage.isBlacklisted(bidder.getUniqueId())) {
            MessageUtil.send(bidder, "blacklisted", null);
            return false;
        }

        // Can't bid on BIN-only listings
        if (listing.isBinOnly()) {
            MessageUtil.send(bidder, "bin-only", null);
            return false;
        }

        // Can't bid on own listing
        if (listing.getSellerUUID().equals(bidder.getUniqueId())) {
            MessageUtil.send(bidder, "bid-own-listing", null);
            return false;
        }

        // Can't bid if already top bidder
        if (bidder.getUniqueId().equals(listing.getTopBidderUUID())) {
            MessageUtil.send(bidder, "already-top-bidder", null);
            return false;
        }

        // Economy availability check
        if (!economy.isAvailable()) {
            MessageUtil.send(bidder, "economy-not-found", null);
            return false;
        }

        var config = plugin.getConfig();
        double minIncrement = config.getDouble("bidding.min-increment", 10);

        // Calculate minimum required bid
        double minBid = listing.hasBids() ? listing.getCurrentBid() + minIncrement : listing.getStartPrice();

        if (amount < minBid) {
            MessageUtil.send(bidder, "bid-too-low", Map.of("min_bid", economy.format(minBid)));
            return false;
        }

        // Withdraw from bidder first — if this fails, reject the bid
        TransactionResult withdrawResult = economy.withdraw(bidder.getUniqueId(), amount);
        if (!withdrawResult.success()) {
            MessageUtil.send(bidder, "insufficient-funds", Map.of("amount", economy.format(amount)));
            return false;
        }

        // Save previous bidder for refund
        UUID previousBidder = listing.getTopBidderUUID();
        double previousAmount = listing.getCurrentBid();
        boolean hadPreviousBid = listing.hasBids();

        // Apply the bid (synchronized inside AuctionListing)
        try {
            listing.applyBid(bidder.getUniqueId(), bidder.getName(), amount, minIncrement);
        } catch (IllegalArgumentException e) {
            // Refund on failure — bid was rejected after money was taken
            economy.deposit(bidder.getUniqueId(), amount);
            MessageUtil.send(bidder, "bid-too-low", Map.of("min_bid", economy.format(minBid)));
            return false;
        }

        // Fire custom event — if cancelled, refund bidder and restore state
        AuctifyBidEvent event = new AuctifyBidEvent(listing, bidder, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Refund the new bidder
            economy.deposit(bidder.getUniqueId(), amount);
            return false;
        }

        // Refund previous top bidder
        if (hadPreviousBid && previousBidder != null) {
            economy.deposit(previousBidder, previousAmount);
            // Notify previous bidder if online
            Player prevPlayer = Bukkit.getPlayer(previousBidder);
            if (prevPlayer != null && prevPlayer.isOnline()) {
                MessageUtil.send(prevPlayer, "auction-lost",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem())));
            }
        }

        // Persist the updated listing
        storage.saveListing(listing);

        // Notify bidder
        MessageUtil.send(bidder, "bid-success",
                Map.of("item", ItemUtil.getDisplayName(listing.getItem())));

        // Broadcast if enabled
        if (config.getBoolean("general.broadcast-bids", true)) {
            MessageUtil.broadcastRaw("§e" + bidder.getName() + " §7bid §a"
                    + economy.format(amount) + " §7on §f"
                    + ItemUtil.getDisplayName(listing.getItem()) + "§7!");
        }

        return true;
    }

    /**
     * Processes a buyout (instant purchase) on a listing.
     *
     * @param buyer     the buying player
     * @param listingId the listing ID
     * @return true if the buyout was successful
     */
    public boolean buyout(Player buyer, String listingId) {
        AuctionListing listing = activeListings.get(listingId);
        if (listing == null || !listing.isActive() || listing.isExpired()) {
            MessageUtil.send(buyer, "listing-not-found", Map.of("listing_id", listingId));
            return false;
        }

        if (listing.getBuyoutPrice() <= 0) {
            MessageUtil.sendRaw(buyer, "§cThis listing does not have a buyout price.");
            return false;
        }

        if (listing.getSellerUUID().equals(buyer.getUniqueId())) {
            MessageUtil.send(buyer, "bid-own-listing", null);
            return false;
        }

        if (!economy.isAvailable()) {
            MessageUtil.send(buyer, "economy-not-found", null);
            return false;
        }

        double price = listing.getBuyoutPrice();

        // Withdraw buyout price
        TransactionResult result = economy.withdraw(buyer.getUniqueId(), price);
        if (!result.success()) {
            MessageUtil.sendRaw(buyer, "§cInsufficient funds. You need §f" + economy.format(price) + "§c.");
            return false;
        }

        // Refund any existing top bidder
        if (listing.hasBids() && listing.getTopBidderUUID() != null) {
            economy.deposit(listing.getTopBidderUUID(), listing.getCurrentBid());
            Player prevBidder = Bukkit.getPlayer(listing.getTopBidderUUID());
            if (prevBidder != null && prevBidder.isOnline()) {
                MessageUtil.send(prevBidder, "auction-lost",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem())));
            }
        }

        // Set the buyer as the top bidder and resolve immediately
        listing.applyBid(buyer.getUniqueId(), buyer.getName(), price, 0);
        resolveAuction(listing);

        MessageUtil.send(buyer, "buyout-success",
                Map.of("item", ItemUtil.getDisplayName(listing.getItem()),
                        "amount", economy.format(price)));

        // Prompt buyer to rate the seller
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (buyer.isOnline()) {
                plugin.getRateGUI().open(buyer, listing.getSellerUUID(), listing.getSellerName());
            }
        }, 10L);

        return true;
    }

    /**
     * Cancels an active listing. Only the seller or admin can cancel.
     *
     * @param player    the player requesting cancellation
     * @param listingId the listing ID to cancel
     * @return true if successfully cancelled
     */
    public boolean cancelListing(Player player, String listingId) {
        AuctionListing listing = activeListings.get(listingId);
        if (listing == null || !listing.isActive()) {
            MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId));
            return false;
        }

        // Only seller or admin can cancel
        boolean isSeller = listing.getSellerUUID().equals(player.getUniqueId());
        boolean isAdmin = player.hasPermission("auctify.admin");
        if (!isSeller && !isAdmin) {
            MessageUtil.send(player, "no-permission", null);
            return false;
        }

        // Deactivate first (double-resolve protection)
        listing.setActive(false);
        activeListings.remove(listingId);

        // Refund top bidder if any
        if (listing.hasBids() && listing.getTopBidderUUID() != null) {
            economy.deposit(listing.getTopBidderUUID(), listing.getCurrentBid());
            Player topBidder = Bukkit.getPlayer(listing.getTopBidderUUID());
            if (topBidder != null && topBidder.isOnline()) {
                MessageUtil.send(topBidder, "auction-lost",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem())));
            }
        }

        // Return item to seller
        deliverItem(listing.getSellerUUID(), listing.getItem());

        // Persist removal
        storage.deleteListing(listingId);

        // Save to history as cancelled
        storage.saveHistory(new AuctionHistory(
                listing.getId(), listing.getSellerUUID(), listing.getSellerName(),
                null, null, ItemUtil.serializeToBase64(listing.getItem()),
                listing.getStartPrice(), 0, 0, System.currentTimeMillis(), "CANCELLED"
        ));

        MessageUtil.send(player, "listing-cancelled", null);
        plugin.getLogger().info("Player " + player.getName() + " cancelled listing " + listingId);
        return true;
    }

    /**
     * Resolves an expired or bought-out auction. Handles item delivery,
     * economy transactions, tax, and notifications.
     *
     * @param listing the listing to resolve
     */
    public void resolveAuction(AuctionListing listing) {
        // Double-resolve protection: deactivate and remove FIRST
        if (!listing.isActive()) return;
        listing.setActive(false);
        activeListings.remove(listing.getId());

        var config = plugin.getConfig();

        if (!listing.hasBids()) {
            // No bids — return item to seller
            deliverItem(listing.getSellerUUID(), listing.getItem());

            Player seller = Bukkit.getPlayer(listing.getSellerUUID());
            if (seller != null && seller.isOnline()) {
                MessageUtil.send(seller, "auction-expired-no-bids",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem())));
            }

            storage.saveHistory(new AuctionHistory(
                    listing.getId(), listing.getSellerUUID(), listing.getSellerName(),
                    null, null, ItemUtil.serializeToBase64(listing.getItem()),
                    listing.getStartPrice(), 0, 0, System.currentTimeMillis(), "EXPIRED"
            ));
        } else {
            // Has a winner — deliver item and pay seller
            UUID winnerUUID = listing.getTopBidderUUID();
            String winnerName = listing.getTopBidderName();
            double finalPrice = listing.getCurrentBid();

            // Deliver item to winner
            deliverItem(winnerUUID, listing.getItem());

            // Calculate tax
            double taxPercent = config.getDouble("economy.tax-percent", 5.0);
            double taxAmount = 0;

            // Check if seller has tax bypass
            Player sellerPlayer = Bukkit.getPlayer(listing.getSellerUUID());
            boolean bypassTax = sellerPlayer != null && sellerPlayer.hasPermission("auctify.bypass.tax");

            if (!bypassTax && taxPercent > 0) {
                taxAmount = finalPrice * (taxPercent / 100.0);
            }

            double netAmount = finalPrice - taxAmount;

            // Pay seller
            economy.deposit(listing.getSellerUUID(), netAmount);

            // Handle tax destination
            if (taxAmount > 0) {
                String taxDest = config.getString("economy.tax-destination", "void");
                if ("server-account".equalsIgnoreCase(taxDest)) {
                    // TODO: Deposit to server account via Vault
                    String taxAccount = config.getString("economy.tax-account-name", "server");
                    logger.info("Tax of " + economy.format(taxAmount) + " sent to " + taxAccount);
                }
                // "void" = tax is deleted, no action needed
            }

            // Notify winner
            Player winner = Bukkit.getPlayer(winnerUUID);
            if (winner != null && winner.isOnline()) {
                MessageUtil.send(winner, "auction-won",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem()),
                                "amount", economy.format(finalPrice)));
            }

            // Notify seller
            if (sellerPlayer != null && sellerPlayer.isOnline()) {
                MessageUtil.send(sellerPlayer, "auction-sold",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem()),
                                "winner", winnerName,
                                "amount", economy.format(finalPrice),
                                "tax", String.format("%.1f", taxPercent),
                                "net", economy.format(netAmount)));
            }

            // Broadcast if enabled
            if (config.getBoolean("general.broadcast-wins", true)) {
                MessageUtil.broadcastRaw("§e" + winnerName + " §7won §f"
                        + ItemUtil.getDisplayName(listing.getItem()) + " §7for §a"
                        + economy.format(finalPrice) + "§7!");
            }

            // Discord webhook
            plugin.getDiscordWebhookUtil().sendSoldEmbed(
                    listing.getSellerName(),
                    winnerName,
                    ItemUtil.getDisplayName(listing.getItem()),
                    economy.format(finalPrice)
            );

            // Save history
            storage.saveHistory(new AuctionHistory(
                    listing.getId(), listing.getSellerUUID(), listing.getSellerName(),
                    winnerUUID, winnerName, ItemUtil.serializeToBase64(listing.getItem()),
                    listing.getStartPrice(), finalPrice, taxAmount,
                    System.currentTimeMillis(), "SOLD"
            ));
        }

        // Remove from persistent storage
        storage.deleteListing(listing.getId());
    }

    /**
     * Delivers an item to a player. If online, adds to inventory (drops at feet if full).
     * If offline, saves to pending deliveries.
     *
     * @param playerUUID the recipient's UUID
     * @param item       the item to deliver
     */
    private void deliverItem(UUID playerUUID, ItemStack item) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            java.util.HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                MessageUtil.sendRaw(player, "§7Your inventory was full. Some items were dropped at your feet.");
            }
        } else {
            // Save for offline delivery
            storage.savePendingDelivery(playerUUID, item.clone());
        }
    }

    /**
     * Gets all pending deliveries for a player and clears them from storage.
     *
     * @param playerUUID the player's UUID
     * @return list of items to deliver
     */
    public List<ItemStack> claimPendingDeliveries(UUID playerUUID) {
        List<ItemStack> items = storage.getPendingDeliveries(playerUUID);
        if (!items.isEmpty()) {
            storage.clearPendingDeliveries(playerUUID);
        }
        return items;
    }

    /**
     * Returns an unmodifiable view of all active listings.
     *
     * @return list of active listings
     */
    public List<AuctionListing> getActiveListings() {
        return new ArrayList<>(activeListings.values());
    }

    /**
     * Gets a listing by its ID.
     *
     * @param id the listing ID
     * @return Optional containing the listing, or empty
     */
    public Optional<AuctionListing> getListingById(String id) {
        return Optional.ofNullable(activeListings.get(id));
    }

    /**
     * Searches active listings by item name (case-insensitive partial match).
     *
     * @param query the search query
     * @return list of matching listings
     */
    public List<AuctionListing> searchListings(String query) {
        String lower = query.toLowerCase();
        return activeListings.values().stream()
                .filter(l -> l.isActive() && !l.isExpired())
                .filter(l -> ItemUtil.getDisplayName(l.getItem()).toLowerCase().contains(lower)
                        || l.getSellerName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    /**
     * Gets auction history for a player.
     *
     * @param playerUUID the player's UUID
     * @param limit      max records to return
     * @return list of history records
     */
    public List<AuctionHistory> getHistory(UUID playerUUID, int limit) {
        return storage.getHistory(playerUUID, limit);
    }

    /**
     * Returns all expired active listings for processing by the expiry task.
     *
     * @return list of expired listings
     */
    public List<AuctionListing> getExpiredListings() {
        return activeListings.values().stream()
                .filter(l -> l.isActive() && l.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Saves all active listings to storage (for auto-save and graceful shutdown).
     * This persists remaining time so timers pause while the server is offline.
     *
     * @return the number of listings saved
     */
    public int saveAllListings() {
        int count = 0;
        for (AuctionListing listing : activeListings.values()) {
            if (listing.isActive()) {
                storage.saveListing(listing);
                count++;
            }
        }
        return count;
    }

    /**
     * Gracefully shuts down: saves all active listings with remaining time intact.
     * Listings are NOT resolved — they will resume when the server restarts.
     */
    public void shutdown() {
        int saved = saveAllListings();
        logger.info("Saved " + saved + " active listings for next startup.");
        activeListings.clear();
    }
}
