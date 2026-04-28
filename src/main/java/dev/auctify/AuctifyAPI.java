package dev.auctify;

import dev.auctify.auction.AuctionListing;
import dev.auctify.auction.AuctionManager;
import dev.auctify.economy.EconomyManager;
import dev.auctify.storage.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API facade for Auctify. Provides static access to common auction
 * operations so other plugins can integrate without depending on internal
 * classes. Initialized by the main plugin class on enable.
 */
public final class AuctifyAPI {

    /** Reference to the internal AuctionManager. Set on plugin enable. */
    private static AuctionManager auctionManager;

    /** Reference to the main plugin instance. */
    private static Auctify plugin;

    /** Private constructor to prevent instantiation. */
    private AuctifyAPI() {
        throw new UnsupportedOperationException("API class cannot be instantiated.");
    }

    /**
     * Initializes the API with the plugin's internal managers. Called once during
     * plugin enable.
     */
    static void init(Auctify pluginInstance) {
        plugin = pluginInstance;
        auctionManager = pluginInstance.getAuctionManager();
    }

    // ─── Query Methods ─────────────────────────────────────────────────────

    /** Returns an unmodifiable list of all currently active auction listings. */
    public static List<AuctionListing> getActiveListings() {
        checkInitialized();
        return Collections.unmodifiableList(auctionManager.getActiveListings());
    }

    /** Looks up a specific auction listing by its unique ID. */
    public static Optional<AuctionListing> getListingById(String id) {
        checkInitialized();
        return auctionManager.getListingById(id);
    }

    /** Returns all active listings for a specific player. */
    public static List<AuctionListing> getPlayerListings(UUID playerUUID) {
        checkInitialized();
        return auctionManager.getActiveListings().stream()
                .filter(l -> l.getSellerUUID().equals(playerUUID))
                .filter(l -> l.isActive())
                .toList();
    }

    /** Returns the total number of active listings. */
    public static int getActiveListingCount() {
        checkInitialized();
        return auctionManager.getActiveListings().size();
    }

    // ─── Economy Methods ────────────────────────────────────────────────────

    /** Checks whether the economy integration (Vault) is available. */
    public static boolean isEconomyAvailable() {
        checkInitialized();
        return plugin.getEconomyManager().isAvailable();
    }

    /** Gets a player's balance. Returns 0 if economy is unavailable. */
    public static double getBalance(UUID playerUUID) {
        checkInitialized();
        if (!isEconomyAvailable()) {
            return 0;
        }
        return plugin.getEconomyManager().getBalance(playerUUID);
    }

    /** Formats a monetary amount according to the economy plugin. */
    public static String formatMoney(double amount) {
        checkInitialized();
        if (!isEconomyAvailable()) {
            return String.format("%.2f", amount);
        }
        return plugin.getEconomyManager().format(amount);
    }

    // ─── Auction Operations ───────────────────────────────────────────────────

    /**
     * Creates a new auction listing programmatically.
     * 
     * @param seller      The player selling the item
     * @param item        The item to sell
     * @param startPrice  Starting bid price
     * @param buyoutPrice Buyout price (0 for no buyout)
     * @param duration    Duration in minutes
     * @return CompletableFuture containing the listing ID, or empty if failed
     */
    public static CompletableFuture<Optional<String>> createListing(Player seller, ItemStack item,
            double startPrice, double buyoutPrice, int duration) {
        checkInitialized();
        CompletableFuture<Optional<String>> future = new CompletableFuture<>();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String listingId = auctionManager.createListing(seller, item, startPrice, buyoutPrice, duration);
            future.complete(Optional.ofNullable(listingId));
        });

        return future;
    }

    /**
     * Cancels an auction listing programmatically.
     * 
     * @param listingId The ID of the listing to cancel
     * @return CompletableFuture containing success status
     */
    public static CompletableFuture<Boolean> cancelListing(String listingId) {
        checkInitialized();
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Find the listing owner for permission check
            Optional<AuctionListing> listing = auctionManager.getListingById(listingId);
            if (listing.isEmpty()) {
                future.complete(false);
                return;
            }

            // For API usage, we'll allow cancellation without player context
            // In production, you'd want proper permission handling
            boolean success = auctionManager.cancelListing(null, listingId);
            future.complete(success);
        });

        return future;
    }

    /**
     * Places a bid on an auction programmatically.
     * 
     * @param bidder    The player placing the bid
     * @param listingId The ID of the listing
     * @param amount    The bid amount
     * @return CompletableFuture containing success status
     */
    public static CompletableFuture<Boolean> placeBid(Player bidder, String listingId, double amount) {
        checkInitialized();
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            boolean success = auctionManager.placeBid(bidder, listingId, amount);
            future.complete(success);
        });

        return future;
    }

    // ─── Storage Access ─────────────────────────────────────────────────────

    /** Gets the storage manager for direct database access. */
    public static StorageManager getStorageManager() {
        checkInitialized();
        return plugin.getStorageManager();
    }

    /** Gets the economy manager for direct economy access. */
    public static EconomyManager getEconomyManager() {
        checkInitialized();
        return plugin.getEconomyManager();
    }

    // ─── Utility Methods ─────────────────────────────────────────────────────

    /** Gets the plugin instance for advanced integration. */
    public static Auctify getPlugin() {
        checkInitialized();
        return plugin;
    }

    /** Checks if a player is blacklisted. */
    public static boolean isBlacklisted(UUID playerUUID) {
        checkInitialized();
        return plugin.getStorageManager().isBlacklisted(playerUUID);
    }

    /** Checks if the plugin is enabled and API is ready. */
    public static boolean isEnabled() {
        return plugin != null && plugin.isEnabled();
    }

    /** Verifies that the API has been initialized before use. */
    private static void checkInitialized() {
        if (auctionManager == null || plugin == null) {
            throw new IllegalStateException("AuctifyAPI has not been initialized. Is Auctify enabled?");
        }
    }
}
