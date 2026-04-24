package dev.auctify.auction;

import dev.auctify.Auctify;
import dev.auctify.economy.EconomyManager;
import dev.auctify.economy.TransactionResult;
import dev.auctify.storage.PendingRefund;
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
 * Central coordinator for all auction operations.
 * All bid/buyout/cancel operations are synchronized per-listing to prevent race conditions.
 */

/**
 * Central coordinator for all auction operations: creating listings, placing
 * bids,
 * buyouts, cancellations, and auction resolution. Holds the in-memory map of
 * active
 * listings and delegates persistence to {@link StorageManager}.
 */
public class AuctionManager {

    private final Auctify plugin;
    private final Logger logger;
    private final EconomyManager economy;
    private final StorageManager storage;

    /** Active listings indexed by ID for O(1) lookup. Thread-safe. */
    private final Map<String, AuctionListing> activeListings = new ConcurrentHashMap<>();

    /** Per-player lock to prevent TOCTOU item duplication on /ac claim. */
    private final Set<UUID> claimingPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Constructs the AuctionManager and loads non-expired listings from storage.
     * Does NOT resolve expired listings here — call
     * {@link #resolveExpiredOnStartup()}
     * after all managers (especially EconomyManager) are fully initialized.
     */
    public AuctionManager(Auctify plugin, EconomyManager economy, StorageManager storage) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.economy = economy;
        this.storage = storage;

        // Load persisted listings into memory (do NOT resolve expired yet)
        List<AuctionListing> persisted = storage.getAllListings();
        for (AuctionListing listing : persisted) {
            activeListings.put(listing.getId(), listing);
        }
        logger.info("Loaded " + activeListings.size() + " listings from storage.");
    }

    /**
     * Resolves listings that expired while the server was offline.
     * Must be called AFTER all managers are initialized (economy hooked).
     */
    public void resolveExpiredOnStartup() {
        List<AuctionListing> expired = activeListings.values().stream()
                .filter(AuctionListing::isExpired)
                .collect(Collectors.toList());
        for (AuctionListing listing : expired) {
            resolveAuction(listing);
        }
        if (!expired.isEmpty()) {
            logger.info("Resolved " + expired.size() + " listings that expired during downtime.");
        }
    }

    /**
     * Safely deposits money, logging failures and saving to pending refunds.
     */
    private void safeDeposit(UUID playerUUID, double amount, String reason) {
        TransactionResult r = economy.deposit(playerUUID, amount);
        if (!r.success()) {
            logger.severe("[Auctify] CRITICAL: Failed to deposit " + economy.format(amount)
                    + " to " + playerUUID + " — reason: " + r.reason()
                    + ". Saving to pending refunds.");
            storage.savePendingRefund(new PendingRefund(playerUUID, amount, reason, System.currentTimeMillis()));
        }
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

        // Item blacklist check (admin configurable)
        String materialName = item.getType().name();
        var blacklistedMaterials = config.getStringList("blacklist.materials");
        String bypassPerm = config.getString("blacklist.bypass-permission", "auctify.admin.blacklist.bypass");
        if (blacklistedMaterials.contains(materialName) && !seller.hasPermission(bypassPerm)) {
            MessageUtil.send(seller, "blacklisted-material", Map.of("material", materialName));
            return null;
        }

        // Max listings check based on permissions (operators bypass all limits)
        int maxListings = getMaxListingsForPlayer(seller);
        long currentCount = activeListings.values().stream()
                .filter(l -> l.getSellerUUID().equals(seller.getUniqueId()) && l.isActive())
                .count();
        if (currentCount >= maxListings) {
            MessageUtil.send(seller, "max-listings-reached", Map.of("max", String.valueOf(maxListings)));
            return null;
        }

        // Listing fee deduction
        if (config.getBoolean("listing-fee.enabled", false) && !seller.hasPermission("auctify.bypass.fee")) {
            double listingFeePercent = config.getDouble("listing-fee.percent", 0);
            double listingFeeMin = config.getDouble("listing-fee.min", 0);
            double listingFeeMax = config.getDouble("listing-fee.max", 0);

            double fee = Math.max(listingFeeMin, startPrice * listingFeePercent / 100);
            if (listingFeeMax > 0)
                fee = Math.min(fee, listingFeeMax);

            if (fee > 0) {
                var feeResult = economy.withdraw(seller.getUniqueId(), fee);
                if (!feeResult.success()) {
                    MessageUtil.send(seller, "listing-fee-insufficient", Map.of("fee", economy.format(fee)));
                    return null;
                }
                MessageUtil.send(seller, "listing-fee-deducted", Map.of("fee", economy.format(fee)));
            }
        }

        // Validate price range and guard against NaN/Infinity exploits
        double minPrice = config.getDouble("bidding.min-start-price", 1.0);
        double maxPrice = config.getDouble("bidding.max-start-price", 1000000.0);
        if (Double.isNaN(startPrice) || Double.isInfinite(startPrice) || startPrice < minPrice
                || (maxPrice > 0 && startPrice > maxPrice)) {
            MessageUtil.send(seller, "invalid-price", null);
            return null;
        }
        if (buyoutPrice > 0 && (Double.isNaN(buyoutPrice) || Double.isInfinite(buyoutPrice))) {
            MessageUtil.send(seller, "invalid-price", null);
            return null;
        }

        // Validate buyout price against multiplier
        if (buyoutPrice > 0) {
            double multiplier = config.getDouble("bidding.buyout-min-multiplier", 1.5);
            if (buyoutPrice < startPrice * multiplier) {
                MessageUtil.send(seller, "buyout-min-price", Map.of("min", economy.format(startPrice * multiplier)));
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

        // Generate unique 8-char ID with collision check
        String id;
        int attempts = 0;
        do {
            if (++attempts > 10) {
                logger.severe("Could not generate unique listing ID after 10 attempts.");
                return null;
            }
            id = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (activeListings.containsKey(id) || storage.listingExists(id));

        long now = System.currentTimeMillis();
        long endTime = now + (durationSeconds * 1000L);

        // Create the listing (stores a defensive copy of the item)
        AuctionListing listing = new AuctionListing(
                id, seller.getUniqueId(), seller.getName(),
                item, startPrice, buyoutPrice, now, endTime);

        // Store tax exempt status at creation time (MEDIUM-2 fix)
        listing.setTaxExempt(seller.hasPermission("auctify.bypass.tax"));

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

        // Get item name for notifications
        String itemName = ItemUtil.getDisplayName(item);

        // Log the transaction
        plugin.getLoggerManager().logListing(seller.getName(), itemName, startPrice, id);

        // Play sound
        plugin.getSoundManager().playListingCreated(seller);

        // Notify seller
        MessageUtil.send(seller, "listing-created", Map.of("listing_id", id));

        // Broadcast if enabled
        if (config.getBoolean("general.broadcast-listings", true)) {
            if (buyoutPrice > 0) {
                MessageUtil.broadcast("listing-broadcast-buyout",
                        Map.of("seller", seller.getName(), "item", itemName, "buyout", economy.format(buyoutPrice)));
            } else {
                MessageUtil.broadcast("listing-broadcast",
                        Map.of("seller", seller.getName(), "item", itemName));
            }
        }

        // Discord webhook notification
        plugin.getDiscordWebhookUtil().sendNewListingEmbed(
                seller.getName(),
                itemName,
                economy.format(startPrice),
                economy.format(buyoutPrice));

        // Schedule expiration warning if enabled
        int warningMinutes = config.getInt("notifications.expiration-warning-minutes", 5);
        int durationMinutes = durationSeconds / 60;
        if (warningMinutes > 0 && durationMinutes > warningMinutes) {
            plugin.getNotificationManager().scheduleExpirationWarning(listing, warningMinutes);
        }

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

        if (!bidder.hasPermission("auctify.bid")) {
            MessageUtil.send(bidder, "no-permission", null);
            return false;
        }
        if (storage.isBlacklisted(bidder.getUniqueId())) {
            MessageUtil.send(bidder, "blacklisted", null);
            return false;
        }
        if (listing.isBinOnly()) {
            MessageUtil.send(bidder, "bin-only", null);
            return false;
        }
        if (listing.getSellerUUID().equals(bidder.getUniqueId())) {
            MessageUtil.send(bidder, "bid-own-listing", null);
            return false;
        }
        if (!listing.canBid(bidder.getUniqueId())) {
            MessageUtil.send(bidder, "auction-private", null);
            return false;
        }
        if (bidder.getUniqueId().equals(listing.getTopBidderUUID())) {
            MessageUtil.send(bidder, "already-top-bidder", null);
            return false;
        }
        if (!economy.isAvailable()) {
            MessageUtil.send(bidder, "economy-not-found", null);
            return false;
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            MessageUtil.send(bidder, "invalid-price", null);
            return false;
        }

        var config = plugin.getConfig();
        double minIncrement = config.getDouble("bidding.min-increment", 10);

        // Variables for tracking previous bidder (needed outside lock for sound)
        UUID previousBidder = null;
        double previousAmount = 0;
        boolean hadPreviousBid = false;

        // CRITICAL-1: Entire bid flow under per-listing lock
        synchronized (listing) {
            if (!listing.isActive() || listing.isExpired()) {
                MessageUtil.send(bidder, "listing-not-found", Map.of("listing_id", listingId));
                return false;
            }

            // Recalculate minBid INSIDE the lock (may have changed)
            double minBid = listing.hasBids() ? listing.getCurrentBid() + minIncrement : listing.getStartPrice();
            if (amount < minBid) {
                MessageUtil.send(bidder, "bid-too-low", Map.of("min_bid", economy.format(minBid)));
                return false;
            }

            TransactionResult withdrawResult = economy.withdraw(bidder.getUniqueId(), amount);
            if (!withdrawResult.success()) {
                MessageUtil.send(bidder, "insufficient-funds", Map.of("amount", economy.format(amount)));
                return false;
            }

            // Save previous state for rollback
            previousBidder = listing.getTopBidderUUID();
            previousAmount = listing.getCurrentBid();
            hadPreviousBid = listing.hasBids();

            // HIGH-1: Fire event BEFORE mutating listing state so cancellation is clean
            AuctifyBidEvent event = new AuctifyBidEvent(listing, bidder, amount);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                economy.deposit(bidder.getUniqueId(), amount);
                return false;
            }

            try {
                listing.applyBid(bidder.getUniqueId(), bidder.getName(), amount, minIncrement);
            } catch (IllegalArgumentException e) {
                economy.deposit(bidder.getUniqueId(), amount);
                MessageUtil.send(bidder, "bid-too-low", Map.of("min_bid", economy.format(minBid)));
                return false;
            }

            // Refund previous top bidder
            if (hadPreviousBid && previousBidder != null) {
                safeDeposit(previousBidder, previousAmount, "Outbid refund on listing " + listingId);
                // Send outbid notification
                plugin.getNotificationManager().notifyOutbid(listing, previousBidder, amount);

                // Check for auto-bid and trigger if configured
                triggerAutoBid(listing, previousBidder);
            }

            storage.saveListing(listing);
        } // end synchronized

        // Record bid in history
        storage.recordBid(listingId, bidder.getUniqueId(), bidder.getName(), amount);

        // Sniping protection: extend auction if bid placed near end
        long timeRemaining = listing.getEndTime() - System.currentTimeMillis();
        int snipingThreshold = config.getInt("sniping-protection.threshold-seconds", 30) * 1000;
        int snipingExtension = config.getInt("sniping-protection.extend-seconds", 30) * 1000;
        if (snipingThreshold > 0 && snipingExtension > 0 && timeRemaining < snipingThreshold) {
            listing.setEndTime(listing.getEndTime() + snipingExtension);
            MessageUtil.broadcast("auction-sniping-extended", Map.of(
                    "id", listingId,
                    "seconds", String.valueOf(snipingExtension / 1000)));
        }

        MessageUtil.send(bidder, "bid-success",
                Map.of("item", ItemUtil.getDisplayName(listing.getItem())));
        if (config.getBoolean("general.broadcast-bids", true)) {
            MessageUtil.broadcast("bid-broadcast", Map.of(
                    "bidder", bidder.getName(),
                    "amount", economy.format(amount),
                    "item", ItemUtil.getDisplayName(listing.getItem())));
        }

        // Log the bid
        plugin.getLoggerManager().logBid(bidder.getName(), listing.getSellerName(),
                ItemUtil.getDisplayName(listing.getItem()), amount, listingId);

        // Play success sound
        plugin.getSoundManager().playBidSuccess(bidder);

        // Play outbid sound to previous bidder
        if (hadPreviousBid && previousBidder != null) {
            Player prevPlayer = Bukkit.getPlayer(previousBidder);
            if (prevPlayer != null && prevPlayer.isOnline()) {
                plugin.getSoundManager().playOutbid(prevPlayer);
            }
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
            MessageUtil.send(buyer, "buyout-not-set", null);
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
            MessageUtil.send(buyer, "insufficient-funds", Map.of("amount", economy.format(price)));
            return false;
        }

        // CRITICAL-2: Deactivate inside synchronized block BEFORE resolving to prevent
        // double-resolve if AuctionExpiryTask fires between applyBid and
        // resolveAuction.
        synchronized (listing) {
            if (!listing.isActive() || listing.isExpired()) {
                // Listing became inactive while we were withdrawing money — refund buyer
                economy.deposit(buyer.getUniqueId(), price);
                MessageUtil.send(buyer, "listing-not-found", Map.of("listing_id", listingId));
                return false;
            }

            // Refund any existing top bidder
            if (listing.hasBids() && listing.getTopBidderUUID() != null) {
                safeDeposit(listing.getTopBidderUUID(), listing.getCurrentBid(),
                        "Outbid refund on buyout for listing " + listingId);
                Player prevBidder = Bukkit.getPlayer(listing.getTopBidderUUID());
                if (prevBidder != null && prevBidder.isOnline()) {
                    MessageUtil.send(prevBidder, "auction-lost",
                            Map.of("item", ItemUtil.getDisplayName(listing.getItem())));
                }
            }

            listing.setActive(false);
            activeListings.remove(listingId);
        }

        // resolveAuction is safe to call outside the lock now — listing is deactivated
        listing.applyBid(buyer.getUniqueId(), buyer.getName(), price, 0);
        resolveAuction(listing);

        // Send buyout notification to seller
        plugin.getNotificationManager().notifyBuyout(listing, buyer.getUniqueId(), buyer.getName());

        MessageUtil.send(buyer, "buyout-success",
                Map.of("item", ItemUtil.getDisplayName(listing.getItem()),
                        "amount", economy.format(price)));

        // Log the buyout
        plugin.getLoggerManager().logBuyout(buyer.getName(), listing.getSellerName(),
                ItemUtil.getDisplayName(listing.getItem()), price, listingId);

        // Play sounds
        plugin.getSoundManager().playBuyoutSuccess(buyer);
        Player seller = Bukkit.getPlayer(listing.getSellerUUID());
        if (seller != null && seller.isOnline()) {
            plugin.getSoundManager().playMoneyReceived(seller);
        }

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

        // MEDIUM-3: Acquire per-listing lock before deactivating to prevent race with
        // bid
        synchronized (listing) {
            if (!listing.isActive()) { // re-check inside lock
                MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId));
                return false;
            }
            listing.setActive(false);
            activeListings.remove(listingId);

            // Refund top bidder if any
            if (listing.hasBids() && listing.getTopBidderUUID() != null) {
                safeDeposit(listing.getTopBidderUUID(), listing.getCurrentBid(),
                        "Cancel refund for listing " + listingId);
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
                    listing.getStartPrice(), 0, 0, System.currentTimeMillis(), "CANCELLED"));
        }

        MessageUtil.send(player, "listing-cancelled", null);
        plugin.getLogger().info("Player " + player.getName() + " cancelled listing " + listingId);

        // Log the cancellation
        plugin.getLoggerManager().logCancel(player.getName(),
                ItemUtil.getDisplayName(listing.getItem()), listingId);

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
        if (!listing.isActive())
            return;
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
                    listing.getStartPrice(), 0, 0, System.currentTimeMillis(), "EXPIRED"));

            // Discord webhook notification for expired auction
            plugin.getDiscordWebhookUtil().sendExpiredEmbed(
                    listing.getSellerName(),
                    ItemUtil.getDisplayName(listing.getItem()),
                    economy.format(listing.getStartPrice()));

            // Log the expired auction
            plugin.getLoggerManager().logExpired(listing.getSellerName(),
                    ItemUtil.getDisplayName(listing.getItem()), listing.getId());
        } else {
            // Has a winner — deliver item and pay seller
            UUID winnerUUID = listing.getTopBidderUUID();
            String winnerName = listing.getTopBidderName();
            double finalPrice = listing.getCurrentBid();

            // Deliver item to winner
            deliverItem(winnerUUID, listing.getItem());

            // Calculate tax
            double taxPercent = config.getDouble("tax.percent", 0);
            double taxAmount = 0;

            // MEDIUM-2: Use stored tax exempt status from listing creation time
            boolean bypassTax = listing.isTaxExempt();

            if (!bypassTax && taxPercent > 0) {
                taxAmount = finalPrice * (taxPercent / 100.0);
            }

            double netAmount = finalPrice - taxAmount;

            // HIGH-3: Pay seller with safe deposit (logs + pending refund on failure)
            safeDeposit(listing.getSellerUUID(), netAmount,
                    "Auction sale payment for listing " + listing.getId());

            // Handle tax destination
            if (taxAmount > 0) {
                String taxDest = config.getString("tax.destination", "void");
                if ("server-account".equalsIgnoreCase(taxDest)) {
                    String taxAccount = "server"; // Static server account name
                    TransactionResult taxResult = economy.depositToAccount(taxAccount, taxAmount);
                    if (taxResult.success()) {
                        logger.info("[Auctify] Tax of " + economy.format(taxAmount)
                                + " deposited to account '" + taxAccount + "'.");
                    }
                    // On failure, tax is voided — already logged inside depositToAccount()
                }
                // "void" = tax is deleted, no action needed
            }

            // Notify winner and play sound
            Player winner = Bukkit.getPlayer(winnerUUID);
            if (winner != null && winner.isOnline()) {
                MessageUtil.send(winner, "auction-won",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem()),
                                "amount", economy.format(finalPrice)));
                plugin.getSoundManager().playAuctionWon(winner);
            }

            // Send auction won notification
            plugin.getNotificationManager().notifyAuctionWon(listing, winnerUUID);

            // Log the successful sale
            plugin.getLoggerManager().logSale(listing.getSellerName(), winnerName,
                    ItemUtil.getDisplayName(listing.getItem()), finalPrice, listing.getId());

            // Notify seller (they may be offline — check current online status)
            Player sellerPlayer = Bukkit.getPlayer(listing.getSellerUUID());
            if (sellerPlayer != null && sellerPlayer.isOnline()) {
                plugin.getSoundManager().playAuctionSold(sellerPlayer);
                MessageUtil.send(sellerPlayer, "auction-sold",
                        Map.of("item", ItemUtil.getDisplayName(listing.getItem()),
                                "winner", winnerName,
                                "amount", economy.format(finalPrice),
                                "tax", String.format("%.1f", taxPercent),
                                "net", economy.format(netAmount)));
            } else {
                // Save pending notification for offline seller
                storage.addPendingNotification(listing.getSellerUUID(), "AUCTION_SOLD",
                        ItemUtil.getDisplayName(listing.getItem()), winnerName,
                        economy.format(finalPrice), economy.format(netAmount));
            }

            // Send item sold notification
            plugin.getNotificationManager().notifyItemSold(listing, winnerUUID, winnerName, finalPrice, taxPercent,
                    netAmount);

            // Record price history
            dev.auctify.auction.PriceHistory priceHistory = new dev.auctify.auction.PriceHistory(
                    listing.getId(),
                    listing.getItem().getType().name(),
                    dev.auctify.util.ItemUtil.getDisplayName(listing.getItem()),
                    finalPrice,
                    listing.getSellerName(),
                    winnerName,
                    System.currentTimeMillis());
            storage.savePriceHistory(priceHistory);

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
                    economy.format(finalPrice));

            // Save history
            storage.saveHistory(new AuctionHistory(
                    listing.getId(), listing.getSellerUUID(), listing.getSellerName(),
                    winnerUUID, winnerName, ItemUtil.serializeToBase64(listing.getItem()),
                    listing.getStartPrice(), finalPrice, taxAmount,
                    System.currentTimeMillis(), "SOLD"));
        }

        // Remove from persistent storage
        storage.deleteListing(listing.getId());
    }

    /**
     * Delivers an item to a player. If online, adds to inventory (drops at feet if
     * full).
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
                    // LOW-2: Log every dropped item for audit trail
                    logger.warning("[Auctify] Item dropped for " + player.getName()
                            + ": " + ItemUtil.getDisplayName(drop) + " x" + drop.getAmount()
                            + " at " + player.getLocation().toVector());
                }
                MessageUtil.send(player, "inventory-full-items-dropped", null);
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
        // MEDIUM-4: Use atomic claimAndClear to prevent TOCTOU duplication
        if (!claimingPlayers.add(playerUUID)) {
            return Collections.emptyList(); // already claiming
        }
        try {
            return storage.claimAndClearDeliveries(playerUUID);
        } finally {
            claimingPlayers.remove(playerUUID);
        }
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
     * Gets the economy manager.
     *
     * @return the EconomyManager
     */
    public EconomyManager getEconomy() {
        return economy;
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
                .filter(l -> {
                    String itemName = ItemUtil.getDisplayName(l.getItem()).toLowerCase();
                    String sellerName = l.getSellerName().toLowerCase();
                    String material = l.getItem().getType().name().toLowerCase().replace("_", " ");
                    return itemName.contains(lower)
                            || sellerName.contains(lower)
                            || material.contains(lower);
                })
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
        int saved = 0;
        int failed = 0;
        for (AuctionListing listing : activeListings.values()) {
            if (!listing.isActive())
                continue;
            try {
                storage.saveListing(listing);
                saved++;
            } catch (Exception e) {
                failed++;
                logger.severe("[Auctify] Failed to save listing " + listing.getId() + ": " + e.getMessage());
            }
        }
        if (failed > 0) {
            logger.severe("[Auctify] " + failed + " listings failed to save!");
        }
        return saved;
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

    /**
     * Gets the maximum number of listings allowed for a player based on
     * permissions.
     * Permission tiers: auctify.listings.unlimited (ops), .10, .5, default 3
     */
    public int getMaxListingsForPlayer(Player player) {
        if (player.isOp() || player.hasPermission("auctify.listings.unlimited")) {
            return Integer.MAX_VALUE; // Unlimited
        }
        if (player.hasPermission("auctify.listings.10")) {
            return 10;
        }
        if (player.hasPermission("auctify.listings.5")) {
            return 5;
        }
        // Default: 3 listings
        return 3;
    }

    /**
     * Extends the expiry time of an auction if it has no bids.
     * Seller can extend to keep the auction alive longer.
     *
     * @param player    the seller
     * @param listingId the auction ID
     * @param minutes   how many minutes to extend
     * @return true if extended successfully
     */
    public boolean extendAuction(Player player, String listingId, int minutes) {
        AuctionListing listing = activeListings.get(listingId);
        if (listing == null || !listing.isActive() || listing.isExpired()) {
            MessageUtil.send(player, "listing-not-found", Map.of("listing_id", listingId));
            return false;
        }
        if (!listing.getSellerUUID().equals(player.getUniqueId())) {
            MessageUtil.send(player, "not-your-listing", null);
            return false;
        }
        if (listing.hasBids()) {
            MessageUtil.send(player, "cannot-extend-has-bids", null);
            return false;
        }

        // Get max extension limit from config
        int maxExtension = plugin.getConfig().getInt("extension.max-minutes", 60);
        if (minutes > maxExtension) {
            MessageUtil.send(player, "extension-too-long", Map.of("max", String.valueOf(maxExtension)));
            return false;
        }

        // Extend the auction
        long newEndTime = listing.getEndTime() + (minutes * 60 * 1000L);
        listing.setEndTime(newEndTime);
        storage.saveListing(listing);

        MessageUtil.send(player, "auction-extended",
                Map.of("id", listingId,
                        "minutes", String.valueOf(minutes)));
        return true;
    }

    /**
     * Cancels all active listings owned by a player.
     * Items are returned to the player's inventory.
     *
     * @param player the player whose auctions to cancel
     * @return number of auctions cancelled
     */
    public int bulkCancelAuctions(Player player) {
        UUID playerUUID = player.getUniqueId();
        List<AuctionListing> toCancel = activeListings.values().stream()
                .filter(l -> l.isActive() && !l.isExpired())
                .filter(l -> l.getSellerUUID().equals(playerUUID))
                .filter(l -> !l.hasBids()) // Can't cancel if has bids
                .collect(Collectors.toList());

        int count = 0;
        for (AuctionListing listing : toCancel) {
            if (cancelListing(player, listing.getId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Auto-relists an expired auction with a discount.
     * Used when auto-relist is enabled and auction expired without bids.
     *
     * @param listing the expired listing to relist
     */
    public void autoRelistAuction(AuctionListing listing) {
        var config = plugin.getConfig();
        double discountPercent = config.getDouble("auto-relist.discount-percent", 10);

        // Calculate discounted price
        double newStartPrice = Math.max(1, listing.getStartPrice() * (1 - discountPercent / 100));
        double newBuyoutPrice = listing.getBuyoutPrice() > 0
                ? Math.max(1, listing.getBuyoutPrice() * (1 - discountPercent / 100))
                : 0;

        // Get current relist count and increment
        String relistKey = "auto-relist.count." + listing.getId();
        int relistCount = config.getInt(relistKey, 0);
        config.set(relistKey, relistCount + 1);
        plugin.saveConfig();

        // Create new listing with same item but new prices
        Player seller = Bukkit.getPlayer(listing.getSellerUUID());
        if (seller != null && seller.isOnline()) {
            String newId = createListing(seller, listing.getItem().clone(), newStartPrice, newBuyoutPrice,
                    config.getInt("general.default-duration", 300));
            if (newId != null) {
                MessageUtil.send(seller, "auto-relist-success", Map.of(
                        "old_id", listing.getId(),
                        "new_id", newId,
                        "discount", String.valueOf(discountPercent)));
            }
        }

        // Resolve the old listing as expired
        listing.setActive(false);
        activeListings.remove(listing.getId());
        storage.saveListing(listing);

        // Return item to seller via storage
        storage.savePendingDelivery(listing.getSellerUUID(), listing.getItem().clone());
    }

    /**
     * Triggers auto-bid for a player who was just outbid.
     *
     * @param listing      the auction listing
     * @param outbidPlayer the UUID of the player who was outbid
     */
    private void triggerAutoBid(AuctionListing listing, UUID outbidPlayer) {
        dev.auctify.auction.AutoBid autoBid = storage.getAutoBid(listing.getId(), outbidPlayer);
        if (autoBid == null) {
            return; // No auto-bid configured
        }

        // Check if auto-bid can still bid
        if (!autoBid.canBid(listing.getCurrentBid())) {
            // Auto-bid exhausted - delete it
            storage.deleteAutoBid(listing.getId(), outbidPlayer);
            Player player = Bukkit.getPlayer(outbidPlayer);
            if (player != null && player.isOnline()) {
                MessageUtil.send(player, "autobid-exhausted", Map.of(
                        "item", dev.auctify.util.ItemUtil.getDisplayName(listing.getItem()),
                        "max_bid", economy.format(autoBid.getMaxBidAmount())));
            }
            return;
        }

        // Calculate bid amount (current bid + min increment, up to max)
        double minIncrement = plugin.getConfig().getDouble("general.min-bid-increment", 1.0);
        double newBidAmount = listing.getCurrentBid() + minIncrement;
        double maxBidAmount = autoBid.getMaxBidAmount();

        // Cap at max bid amount
        if (newBidAmount > maxBidAmount) {
            newBidAmount = maxBidAmount;
        }

        // Check if new bid is valid
        if (newBidAmount <= listing.getCurrentBid()) {
            return; // Bid would be invalid
        }

        // Check if player has sufficient funds
        Player bidder = Bukkit.getPlayer(outbidPlayer);
        if (bidder == null || !bidder.isOnline()) {
            return; // Player offline, can't auto-bid
        }

        // Place the bid automatically
        double balance = economy.getBalance(outbidPlayer);
        if (balance < newBidAmount) {
            // Insufficient funds - delete auto-bid
            storage.deleteAutoBid(listing.getId(), outbidPlayer);
            MessageUtil.send(bidder, "autobid-insufficient-funds", Map.of(
                    "amount", economy.format(newBidAmount)));
            return;
        }

        // Place the bid
        boolean success = placeBid(bidder, listing.getId(), newBidAmount);
        if (success) {
            MessageUtil.send(bidder, "autobid-placed", Map.of(
                    "item", dev.auctify.util.ItemUtil.getDisplayName(listing.getItem()),
                    "amount", economy.format(newBidAmount),
                    "remaining", economy.format(maxBidAmount - newBidAmount)));
        } else {
            // Bid failed - delete auto-bid
            storage.deleteAutoBid(listing.getId(), outbidPlayer);
        }
    }
}
