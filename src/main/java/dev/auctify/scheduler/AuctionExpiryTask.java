package dev.auctify.scheduler;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Repeating task that checks for expired auctions every second (20 ticks).
 * Collects expired listings into a separate list first to avoid
 * ConcurrentModificationException, then resolves each one via the
 * AuctionManager.
 */
public class AuctionExpiryTask extends BukkitRunnable {

    private final Auctify plugin;

    /** Constructor. */
    public AuctionExpiryTask(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Called every 20 ticks (1 second). Collects and resolves expired listings.
     * Also handles auto-relist for expired auctions without bids.
     */
    @Override
    public void run() {
        // Collect expired listings into a snapshot to avoid concurrent modification
        List<AuctionListing> expired = plugin.getAuctionManager().getExpiredListings();

        // Resolve each expired listing
        for (AuctionListing listing : expired) {
            try {
                // Check auto-relist for expired auctions without bids
                if (shouldAutoRelist(listing)) {
                    plugin.getAuctionManager().autoRelistAuction(listing);
                } else {
                    plugin.getAuctionManager().resolveAuction(listing);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error resolving auction " + listing.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Checks if an expired auction should be auto-relisted.
     * Requirements: no bids placed, auto-relist enabled in config, under max relist
     * attempts.
     */
    private boolean shouldAutoRelist(AuctionListing listing) {
        var config = plugin.getConfig();
        if (!config.getBoolean("auto-relist.enabled", false))
            return false;
        if (listing.hasBids())
            return false; // Don't relist if there were bids

        // Check max relist attempts
        int maxRelists = config.getInt("auto-relist.max-attempts", 3);
        int relistCount = config.getInt("auto-relist.count." + listing.getId(), 0);
        return relistCount < maxRelists;
    }
}
