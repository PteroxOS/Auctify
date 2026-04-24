package dev.auctify.storage;

import dev.auctify.auction.AuctionHistory;
import dev.auctify.auction.AuctionListing;
import dev.auctify.auction.BidRecord;
import dev.auctify.auction.BuyOrder;
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

    /** Ratings: sellerUUID -> list of [raterUUID, rating]. */
    private final Map<UUID, List<int[]>> ratings = new ConcurrentHashMap<>();

    /** Pending money refunds keyed by player UUID. */
    private final Map<UUID, List<PendingRefund>> pendingRefunds = new ConcurrentHashMap<>();

    /** Blacklisted players. */
    private final Set<UUID> blacklist = ConcurrentHashMap.newKeySet();

    /** Bid history: listingId -> list of BidRecord. */
    private final Map<String, List<BidRecord>> bidHistory = new ConcurrentHashMap<>();

    /**
     * Pending notifications: playerUUID -> list of [type, item, winner, amount,
     * net].
     */
    private final Map<UUID, List<String[]>> pendingNotifications = new ConcurrentHashMap<>();

    /** Buy orders: orderId -> BuyOrder. */
    private final Map<String, BuyOrder> buyOrders = new ConcurrentHashMap<>();

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

    // ─── Rating System ───────────────────────────────

    @Override
    public void saveRating(UUID sellerUUID, UUID raterUUID, int rating) {
        ratings.computeIfAbsent(sellerUUID, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new int[] { raterUUID.hashCode(), rating });
    }

    @Override
    public double getAverageRating(UUID sellerUUID) {
        List<int[]> r = ratings.get(sellerUUID);
        if (r == null || r.isEmpty())
            return -1;
        return r.stream().mapToInt(a -> a[1]).average().orElse(-1);
    }

    @Override
    public int getRatingCount(UUID sellerUUID) {
        List<int[]> r = ratings.get(sellerUUID);
        return r != null ? r.size() : 0;
    }

    @Override
    public boolean hasRated(UUID sellerUUID, UUID raterUUID) {
        List<int[]> r = ratings.get(sellerUUID);
        if (r == null)
            return false;
        int hash = raterUUID.hashCode();
        return r.stream().anyMatch(a -> a[0] == hash);
    }

    // ─── Blacklist System ────────────────────────────

    @Override
    public void addBlacklist(UUID playerUUID, String reason, String blacklistedBy) {
        blacklist.add(playerUUID);
    }

    @Override
    public void removeBlacklist(UUID playerUUID) {
        blacklist.remove(playerUUID);
    }

    @Override
    public boolean isBlacklisted(UUID playerUUID) {
        return blacklist.contains(playerUUID);
    }

    @Override
    public List<String[]> getBlacklist() {
        return blacklist.stream()
                .map(u -> new String[] { u.toString(), "N/A", "N/A", "0" })
                .collect(Collectors.toList());
    }

    @Override
    public boolean listingExists(String id) {
        return listings.containsKey(id);
    }

    @Override
    public void savePendingRefund(PendingRefund refund) {
        pendingRefunds.computeIfAbsent(refund.playerUUID(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(refund);
    }

    @Override
    public List<PendingRefund> getPendingRefunds(UUID playerUUID) {
        List<PendingRefund> list = pendingRefunds.getOrDefault(playerUUID, Collections.emptyList());
        // Return defensive copies as new list
        return List.copyOf(list);
    }

    @Override
    public void clearPendingRefunds(UUID playerUUID) {
        pendingRefunds.remove(playerUUID);
    }

    @Override
    public synchronized List<PendingRefund> claimAndClearRefunds(UUID playerUUID) {
        List<PendingRefund> refunds = pendingRefunds.remove(playerUUID);
        return refunds != null ? refunds : Collections.emptyList();
    }

    @Override
    public List<ItemStack> claimAndClearDeliveries(UUID playerUUID) {
        List<ItemStack> items = pendingDeliveries.remove(playerUUID);
        return items != null ? items : Collections.emptyList();
    }

    // ─── Bid History Implementation ─────────────────

    @Override
    public void recordBid(String listingId, UUID bidderUUID, String bidderName, double amount) {
        bidHistory.computeIfAbsent(listingId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new BidRecord(bidderUUID, bidderName, amount, System.currentTimeMillis()));
    }

    @Override
    public List<BidRecord> getBidHistory(String listingId) {
        List<BidRecord> bids = bidHistory.get(listingId);
        if (bids == null)
            return Collections.emptyList();
        // Return a copy sorted by timestamp descending
        return bids.stream()
                .sorted((a, b) -> Long.compare(b.timestamp(), a.timestamp()))
                .collect(Collectors.toList());
    }

    // ─── Pending Notification Implementation ────────

    @Override
    public void addPendingNotification(UUID playerUUID, String type, String itemName, String winnerName, String amount,
            String netAmount) {
        pendingNotifications.computeIfAbsent(playerUUID, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new String[] { type, itemName, winnerName, amount, netAmount });
    }

    @Override
    public List<String[]> getAndClearPendingNotifications(UUID playerUUID) {
        List<String[]> notifications = pendingNotifications.remove(playerUUID);
        return notifications != null ? notifications : Collections.emptyList();
    }

    @Override
    public double[] getPriceStats(String itemType) {
        // Not implemented for memory storage
        return null;
    }

    /** {@inheritDoc} */
    // ─── Buy Order Implementation ───────────────────

    @Override
    public void saveBuyOrder(BuyOrder order) {
        buyOrders.put(order.getId(), order);
    }

    @Override
    public void deleteBuyOrder(String orderId) {
        buyOrders.remove(orderId);
    }

    @Override
    public List<BuyOrder> getAllBuyOrders() {
        return new ArrayList<>(buyOrders.values().stream()
                .filter(BuyOrder::isActive)
                .filter(o -> !o.isExpired())
                .toList());
    }

    @Override
    public List<BuyOrder> getBuyOrdersByPlayer(UUID playerUUID) {
        return buyOrders.values().stream()
                .filter(o -> o.getBuyerUUID().equals(playerUUID))
                .filter(BuyOrder::isActive)
                .filter(o -> !o.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public void shutdown() {
        // No cleanup needed — data is transient
        listings.clear();
        history.clear();
        pendingDeliveries.clear();
        pendingRefunds.clear();
        bidHistory.clear();
        buyOrders.clear();
    }
}
