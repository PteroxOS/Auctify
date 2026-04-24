package dev.auctify;

import dev.auctify.auction.AuctionListing;
import dev.auctify.auction.AuctionManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    /** Checks whether the economy integration (Vault) is available. */
    public static boolean isEconomyAvailable() {
        checkInitialized();
        return plugin.getEconomyManager().isAvailable();
    }

    /** Verifies that the API has been initialized before use. */
    private static void checkInitialized() {
        if (auctionManager == null || plugin == null) {
            throw new IllegalStateException("AuctifyAPI has not been initialized. Is Auctify enabled?");
        }
    }

    // TODO: Add PlaceholderAPI support for leaderboard placeholders
    // TODO: Add Discord webhook notification on auction end
}
