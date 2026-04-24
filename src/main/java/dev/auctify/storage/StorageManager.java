package dev.auctify.storage;

import dev.auctify.auction.AuctionHistory;
import dev.auctify.auction.AuctionListing;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Interface defining the contract for auction data persistence backends.
 * Implementations include in-memory (development/testing), SQLite (file-based),
 * and MySQL (production-scale) storage.
 *
 * <p>
 * All implementations must be safe to call from the main thread.
 * Implementations
 * that perform I/O (SQLite, MySQL) should handle asynchronous operations
 * internally
 * and block appropriately, or the caller should wrap calls in
 * CompletableFuture.
 * </p>
 */
public interface StorageManager {

    /**
     * Initializes the storage backend (creates tables, opens connections, etc.).
     * Must be called once during plugin startup after construction.
     */
    void initialize();

    /**
     * Saves or updates an active auction listing in persistent storage.
     *
     * @param listing the listing to save
     */
    void saveListing(AuctionListing listing);

    /**
     * Deletes an active listing from persistent storage by its ID.
     *
     * @param id the listing ID to delete
     */
    void deleteListing(String id);

    /**
     * Retrieves all active listings from persistent storage.
     * Used on plugin startup to reload listings into memory.
     *
     * @return a list of all persisted active listings
     */
    List<AuctionListing> getAllListings();

    /**
     * Saves a completed auction record to the history table.
     *
     * @param history the auction history record to save
     */
    void saveHistory(AuctionHistory history);

    /**
     * Retrieves the auction history for a specific player, ordered by most recent
     * first.
     *
     * @param playerUUID the UUID of the player (as seller or winner)
     * @param limit      the maximum number of records to return
     * @return a list of auction history records
     */
    List<AuctionHistory> getHistory(UUID playerUUID, int limit);

    /**
     * Saves an item for later delivery to an offline player.
     * Used when a player wins or has their listing cancelled while offline.
     *
     * @param playerUUID the UUID of the player to receive the item
     * @param item       the ItemStack to deliver
     */
    void savePendingDelivery(UUID playerUUID, ItemStack item);

    /**
     * Retrieves all pending item deliveries for a player.
     *
     * @param playerUUID the UUID of the player
     * @return a list of ItemStacks waiting for delivery
     */
    List<ItemStack> getPendingDeliveries(UUID playerUUID);

    /**
     * Clears all pending deliveries for a player after successful delivery.
     *
     * @param playerUUID the UUID of the player
     */
    void clearPendingDeliveries(UUID playerUUID);

    /**
     * Gracefully shuts down the storage backend (closes connections, flushes
     * buffers).
     * Called during plugin disable.
     */
    void shutdown();

    // ─── Listing Existence Check ────────────────────

    /**
     * Checks whether a listing with the given ID exists in storage.
     * Used to prevent ID collisions during listing creation.
     *
     * @param id the listing ID to check
     * @return true if a listing with that ID exists
     */
    boolean listingExists(String id);

    // ─── Pending Refunds ────────────────────────────

    /**
     * Saves a pending refund record for delivery on next player login.
     *
     * @param refund the PendingRefund record to save
     */
    void savePendingRefund(PendingRefund refund);

    /**
     * Returns all pending refunds for the given player.
     *
     * @param playerUUID the UUID of the player
     * @return list of PendingRefund records
     */
    List<PendingRefund> getPendingRefunds(UUID playerUUID);

    /**
     * Deletes all pending refunds for the given player (call after delivering
     * them).
     *
     * @param playerUUID the UUID of the player
     */
    void clearPendingRefunds(UUID playerUUID);

    /**
     * Atomically fetches and clears pending refunds in one operation (prevents
     * double-delivery).
     *
     * @param playerUUID the UUID of the player
     * @return list of refunds that were pending, now cleared from storage
     */
    default List<PendingRefund> claimAndClearRefunds(UUID playerUUID) {
        List<PendingRefund> refunds = getPendingRefunds(playerUUID);
        if (!refunds.isEmpty())
            clearPendingRefunds(playerUUID);
        return refunds;
    }

    // ─── Atomic Claim ───────────────────────────────

    /**
     * Atomically fetches and clears all pending deliveries for a player.
     * Prevents TOCTOU duplication if called concurrently.
     *
     * @param playerUUID the UUID of the player
     * @return list of items that were pending, now cleared from storage
     */
    default List<ItemStack> claimAndClearDeliveries(UUID playerUUID) {
        List<ItemStack> items = getPendingDeliveries(playerUUID);
        if (!items.isEmpty())
            clearPendingDeliveries(playerUUID);
        return items;
    }

    // ─── Rating System ───────────────────────────────

    /**
     * Saves a player rating for a seller.
     */
    void saveRating(UUID sellerUUID, UUID raterUUID, int rating);

    /**
     * Gets the average rating for a seller.
     * 
     * @return the average rating (1-5), or -1 if no ratings
     */
    double getAverageRating(UUID sellerUUID);

    /**
     * Gets the total number of ratings a seller has received.
     */
    int getRatingCount(UUID sellerUUID);

    /**
     * Checks if a player has already rated a specific seller.
     */
    boolean hasRated(UUID sellerUUID, UUID raterUUID);

    // ─── Blacklist System ────────────────────────────

    /**
     * Adds a player to the blacklist.
     */
    void addBlacklist(UUID playerUUID, String reason, String blacklistedBy);

    /**
     * Removes a player from the blacklist.
     */
    void removeBlacklist(UUID playerUUID);

    /**
     * Checks if a player is blacklisted.
     */
    boolean isBlacklisted(UUID playerUUID);

    /**
     * Gets all blacklisted player UUIDs.
     */
    java.util.List<String[]> getBlacklist();

    // ─── Backup System ──────────────────────────────

    /**
     * Performs a backup of the storage data (implementation-dependent).
     * For SQLite: copies the database file to backup folder.
     * For MySQL: may perform a logical dump or rely on external backup.
     * For Memory: no-op.
     *
     * @return true if backup was successful, false otherwise
     */
    default boolean backup() {
        return true; // Default no-op for in-memory or unsupported backends
    }

    // ─── Bid History System ───────────────────────────

    /**
     * Records a bid placed on an auction listing.
     */
    void recordBid(String listingId, UUID bidderUUID, String bidderName, double amount);

    /**
     * Retrieves the bid history for a specific auction listing.
     */
    java.util.List<dev.auctify.auction.BidRecord> getBidHistory(String listingId);

    // ─── Price Statistics ──────────────────────────

    /**
     * Gets price statistics for an item type from auction history.
     * Returns [avgPrice, minPrice, maxPrice, count] or null if no data.
     */
    double[] getPriceStats(String itemType);

    // ─── Pending Notification System ────────────────

    /**
     * Adds a pending notification for a player who was offline when their auction
     * ended.
     */
    void addPendingNotification(UUID playerUUID, String type, String itemName, String winnerName, String amount,
            String netAmount);

    /**
     * Gets and clears pending notifications for a player.
     */
    java.util.List<String[]> getAndClearPendingNotifications(UUID playerUUID);

    // ─── Buy Order System ───────────────────────────

    /**
     * Saves a buy order to storage.
     */
    void saveBuyOrder(dev.auctify.auction.BuyOrder order);

    /**
     * Deletes a buy order from storage.
     */
    void deleteBuyOrder(String orderId);

    /**
     * Gets all active buy orders.
     */
    java.util.List<dev.auctify.auction.BuyOrder> getAllBuyOrders();

    /**
     * Gets buy orders for a specific player.
     */
    java.util.List<dev.auctify.auction.BuyOrder> getBuyOrdersByPlayer(UUID playerUUID);

    // ─── Watchlist System ───────────────────────────

    /**
     * Adds a listing to player's watchlist.
     */
    void addToWatchlist(UUID playerUUID, String listingId);

    /**
     * Removes a listing from player's watchlist.
     */
    void removeFromWatchlist(UUID playerUUID, String listingId);

    /**
     * Checks if a listing is in player's watchlist.
     */
    boolean isInWatchlist(UUID playerUUID, String listingId);

    /**
     * Gets all watchlist entries for a player.
     */
    java.util.List<String> getWatchlist(UUID playerUUID);

    /**
     * Clears all watchlist entries for a player.
     */
    void clearWatchlist(UUID playerUUID);
}
