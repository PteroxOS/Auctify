package dev.auctify.scheduler;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Repeating task that checks for expired auctions every second (20 ticks).
 * Collects expired listings into a separate list first to avoid ConcurrentModificationException,
 * then resolves each one via the AuctionManager.
 */
public class AuctionExpiryTask extends BukkitRunnable {

    private final Auctify plugin;

    /**
     * @param plugin the main plugin instance
     */
    public AuctionExpiryTask(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Called every 20 ticks (1 second). Collects and resolves expired listings.
     */
    @Override
    public void run() {
        // Collect expired listings into a snapshot to avoid concurrent modification
        List<AuctionListing> expired = plugin.getAuctionManager().getExpiredListings();

        // Resolve each expired listing
        for (AuctionListing listing : expired) {
            try {
                plugin.getAuctionManager().resolveAuction(listing);
            } catch (Exception e) {
                plugin.getLogger().severe("Error resolving auction " + listing.getId() + ": " + e.getMessage());
            }
        }
    }
}
