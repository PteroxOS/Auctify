package dev.auctify.storage;

import dev.auctify.auction.AuctionHistory;
import dev.auctify.auction.AuctionListing;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Interface defining the contract for auction data persistence backends.
 * Implementations include in-memory (development/testing), SQLite (file-based),
 * and MySQL (production-scale) storage.
 *
 * <p>All implementations must be safe to call from the main thread. Implementations
 * that perform I/O (SQLite, MySQL) should handle asynchronous operations internally
 * and block appropriately, or the caller should wrap calls in CompletableFuture.</p>
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
     * Retrieves the auction history for a specific player, ordered by most recent first.
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
     * Gracefully shuts down the storage backend (closes connections, flushes buffers).
     * Called during plugin disable.
     */
    void shutdown();
}
