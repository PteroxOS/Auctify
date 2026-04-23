package dev.auctify.auction;

import dev.auctify.util.ColorUtil;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Data model representing a single active auction listing.
 * Holds all state for an auction including the item, pricing, bidding history,
 * and timing. Fields are immutable where possible, with synchronized mutators
 * for bid application to prevent concurrent bid corruption.
 *
 * <p>The stored {@link ItemStack} is always a defensive copy — the original
 * item is never mutated by this class.</p>
 */
public class AuctionListing {

    /** Unique 8-character ID derived from a UUID for this listing. */
    private final String id;

    /** UUID of the player who created this listing. */
    private final UUID sellerUUID;

    /** Display name of the seller at listing creation time. */
    private final String sellerName;

    /** Defensive copy of the auctioned item. Never returned directly — always cloned. */
    private final ItemStack item;

    /** The starting price set by the seller. */
    private final double startPrice;

    /** The buyout (instant-win) price, or 0 if buyout is disabled for this listing. */
    private final double buyoutPrice;

    /** The current highest bid amount. Starts equal to startPrice. */
    private volatile double currentBid;

    /** UUID of the current top bidder, or null if no bids have been placed. */
    private volatile UUID topBidderUUID;

    /** Display name of the current top bidder, or null if no bids. */
    private volatile String topBidderName;

    /** Full history of all bids placed on this listing, in chronological order. */
    private final List<BidRecord> bidHistory;

    /** Epoch milliseconds when this listing was created. */
    private final long createdAt;

    /** Epoch milliseconds when this listing expires. Recalculated on server start. */
    private volatile long endTime;

    /** Whether this listing is still active (not yet resolved). */
    private volatile boolean active;

    /**
     * Constructs a new AuctionListing with all required fields.
     * The item is stored as a defensive copy to prevent external mutation.
     *
     * @param id           unique 8-character ID
     * @param sellerUUID   UUID of the seller
     * @param sellerName   display name of the seller
     * @param item         the item being auctioned (a defensive copy is stored)
     * @param startPrice   the starting bid price
     * @param buyoutPrice  the buyout price, or 0 for no buyout
     * @param createdAt    epoch millis of creation
     * @param endTime      epoch millis when the auction expires
     */
    public AuctionListing(String id, UUID sellerUUID, String sellerName, ItemStack item,
                          double startPrice, double buyoutPrice, long createdAt, long endTime) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone(); // Defensive copy on creation
        this.startPrice = startPrice;
        this.buyoutPrice = buyoutPrice;
        this.currentBid = startPrice; // Current bid starts at start price
        this.topBidderUUID = null;
        this.topBidderName = null;
        this.bidHistory = Collections.synchronizedList(new ArrayList<>());
        this.createdAt = createdAt;
        this.endTime = endTime;
        this.active = true;
    }

    /**
     * Full constructor for rebuilding listings from storage (includes bid state).
     *
     * @param id             unique 8-character ID
     * @param sellerUUID     UUID of the seller
     * @param sellerName     display name of the seller
     * @param item           the item being auctioned
     * @param startPrice     the starting bid price
     * @param buyoutPrice    the buyout price, or 0 for no buyout
     * @param currentBid     the current highest bid
     * @param topBidderUUID  UUID of the top bidder, or null
     * @param topBidderName  name of the top bidder, or null
     * @param createdAt      epoch millis of creation
     * @param endTime        epoch millis of expiry
     */
    public AuctionListing(String id, UUID sellerUUID, String sellerName, ItemStack item,
                          double startPrice, double buyoutPrice, double currentBid,
                          UUID topBidderUUID, String topBidderName,
                          long createdAt, long endTime) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone(); // Defensive copy
        this.startPrice = startPrice;
        this.buyoutPrice = buyoutPrice;
        this.currentBid = currentBid;
        this.topBidderUUID = topBidderUUID;
        this.topBidderName = topBidderName;
        this.bidHistory = Collections.synchronizedList(new ArrayList<>());
        this.createdAt = createdAt;
        this.endTime = endTime;
        this.active = true;
    }

    /**
     * Applies a new bid to this listing. Thread-safe via synchronization.
     * Validates that the bid exceeds the current bid by at least the minimum increment.
     *
     * @param bidder      the UUID of the bidding player
     * @param bidderName  the display name of the bidder
     * @param amount      the bid amount
     * @param minIncrement the minimum bid increment from config
     * @throws IllegalArgumentException if the bid amount is too low
     */
    public synchronized void applyBid(UUID bidder, String bidderName, double amount, double minIncrement) {
        // Validate that the new bid exceeds the current bid by at least the min increment
        double minimumRequired = currentBid + minIncrement;

        // For the first bid, only require the bid to be >= start price
        if (!hasBids()) {
            minimumRequired = startPrice;
        }

        if (amount < minimumRequired) {
            throw new IllegalArgumentException(
                    "Bid of " + amount + " is too low. Minimum required: " + minimumRequired);
        }

        // Update the listing state
        this.currentBid = amount;
        this.topBidderUUID = bidder;
        this.topBidderName = bidderName;

        // Record the bid in history
        this.bidHistory.add(new BidRecord(bidder, bidderName, amount, System.currentTimeMillis()));
    }

    /**
     * Returns the number of seconds remaining until this auction expires.
     *
     * @return seconds remaining, minimum 0
     */
    public long getTimeRemainingSeconds() {
        return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
    }

    /**
     * Checks whether this auction has expired (timer has reached zero).
     *
     * @return true if the remaining time is 0 or less
     */
    public boolean isExpired() {
        return getTimeRemainingSeconds() <= 0;
    }

    /**
     * Checks whether any bids have been placed on this listing.
     *
     * @return true if there is a top bidder
     */
    public boolean hasBids() {
        return topBidderUUID != null;
    }

    /**
     * Builds a display-ready clone of the item with auction info lore.
     * Reads the lore format from config at call-time for hot-reload support.
     * Never modifies the original stored item.
     *
     * @param config the plugin's FileConfiguration to read display settings
     * @return a cloned ItemStack with auction lore appended
     */
    public ItemStack buildDisplayItem(FileConfiguration config) {
        // Always clone the item to prevent mutation of the stored copy
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();

        if (meta == null) {
            return displayItem;
        }

        // Read lore template from config at call-time
        List<String> loreTemplate = config.getStringList("display.lore");
        String noBidderPlaceholder = config.getString("display.no-bidder-placeholder", "§7No bids yet");

        // Build the display lore as Adventure Components
        List<Component> lore = new ArrayList<>();

        // Preserve any existing item lore (already as Components via Adventure API)
        if (meta.hasLore()) {
            List<Component> existingLore = meta.lore();
            if (existingLore != null) {
                lore.addAll(existingLore);
            }
        }

        // Determine the top bidder display string
        String topBidderDisplay = hasBids() ? topBidderName : noBidderPlaceholder;

        // Format the time remaining using the config time format
        String timeLeft = TimeUtil.formatSeconds(getTimeRemainingSeconds(), config);

        // Replace placeholders in each lore line from the template, convert to Components
        for (String line : loreTemplate) {
            String processed = line
                    .replace("{seller}", sellerName)
                    .replace("{start_price}", String.format("%.2f", startPrice))
                    .replace("{current_bid}", String.format("%.2f", currentBid))
                    .replace("{top_bidder}", topBidderDisplay)
                    .replace("{time_left}", timeLeft)
                    .replace("{listing_id}", id);
            lore.add(ColorUtil.toComponent(processed));
        }

        meta.lore(lore);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    /**
     * Returns a defensive copy of the stored item. Never exposes the internal reference.
     *
     * @return a clone of the auctioned ItemStack
     */
    public ItemStack getItem() {
        return item.clone();
    }

    /** @return the unique 8-character listing ID */
    public String getId() {
        return id;
    }

    /** @return the UUID of the seller */
    public UUID getSellerUUID() {
        return sellerUUID;
    }

    /** @return the display name of the seller */
    public String getSellerName() {
        return sellerName;
    }

    /** @return the starting price */
    public double getStartPrice() {
        return startPrice;
    }

    /** @return the buyout price, or 0 if no buyout */
    public double getBuyoutPrice() {
        return buyoutPrice;
    }

    /** @return the current highest bid amount */
    public double getCurrentBid() {
        return currentBid;
    }

    /** @return the UUID of the current top bidder, or null */
    public UUID getTopBidderUUID() {
        return topBidderUUID;
    }

    /** @return the name of the current top bidder, or null */
    public String getTopBidderName() {
        return topBidderName;
    }

    /**
     * Returns an unmodifiable view of the bid history.
     *
     * @return unmodifiable list of all bid records
     */
    public List<BidRecord> getBidHistory() {
        return Collections.unmodifiableList(new ArrayList<>(bidHistory));
    }

    /** @return the epoch millis when this listing was created */
    public long getCreatedAt() {
        return createdAt;
    }

    /** @return the epoch millis when this listing expires */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time. Used on server startup to recalculate from remaining seconds.
     * @param endTime the new absolute end time in epoch millis
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /** @return whether this listing is still active */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the active state of this listing.
     * Should only be called by AuctionManager during resolution.
     *
     * @param active the new active state
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
