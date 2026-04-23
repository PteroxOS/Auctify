package dev.auctify.storage;

import dev.auctify.auction.AuctionHistory;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link StorageManager}.
 * Data is lost on server restart — suitable for development and testing only.
 * All operations are thread-safe via concurrent collections.
 */
public class MemoryStorage implements StorageManager {

    /** Active listings indexed by listing ID for O(1) lookup. */
    private final Map<String, AuctionListing> listings = new ConcurrentHashMap<>();

    /** Completed auction history records, stored in insertion order. */
    private final List<AuctionHistory> history = Collections.synchronizedList(new ArrayList<>());

    /** Pending item deliveries keyed by player UUID. */
    private final Map<UUID, List<ItemStack>> pendingDeliveries = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        // No initialization needed for in-memory storage
    }

    /** {@inheritDoc} */
    @Override
    public void saveListing(AuctionListing listing) {
        listings.put(listing.getId(), listing);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteListing(String id) {
        listings.remove(id);
    }

    /** {@inheritDoc} */
    @Override
    public List<AuctionListing> getAllListings() {
        return new ArrayList<>(listings.values());
    }

    /** {@inheritDoc} */
    @Override
    public void saveHistory(AuctionHistory record) {
        history.add(record);
    }

    /**
     * {@inheritDoc}
     * Filters history by player UUID (as seller or winner) and returns
     * the most recent records up to the specified limit.
     */
    @Override
    public List<AuctionHistory> getHistory(UUID playerUUID, int limit) {
        return history.stream()
                // Match records where the player was either the seller or the winner
                .filter(h -> h.sellerUUID().equals(playerUUID)
                        || (h.winnerUUID() != null && h.winnerUUID().equals(playerUUID)))
                // Sort by resolution time descending (most recent first)
                .sorted(Comparator.comparingLong(AuctionHistory::resolvedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public void savePendingDelivery(UUID playerUUID, ItemStack item) {
        pendingDeliveries.computeIfAbsent(playerUUID,
                k -> Collections.synchronizedList(new ArrayList<>())).add(item.clone());
    }

    /** {@inheritDoc} */
    @Override
    public List<ItemStack> getPendingDeliveries(UUID playerUUID) {
        List<ItemStack> items = pendingDeliveries.getOrDefault(playerUUID, List.of());
        // Return defensive copies
        return items.stream().map(ItemStack::clone).collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public void clearPendingDeliveries(UUID playerUUID) {
        pendingDeliveries.remove(playerUUID);
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        // No cleanup needed — data is transient
        listings.clear();
        history.clear();
        pendingDeliveries.clear();
    }
}
