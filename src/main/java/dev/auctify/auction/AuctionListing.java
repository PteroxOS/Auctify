package dev.auctify.auction;

import dev.auctify.util.ColorUtil;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
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
 * Data model representing a single active auction listing. Thread-safe with
 * synchronized bid application to prevent concurrent bid corruption. The stored
 * ItemStack is always a defensive copy to prevent external mutation.
 */
public class AuctionListing {

    /** Unique 8-character listing ID. */
    private final String id;

    /** UUID of the seller. */
    private final UUID sellerUUID;

    /** Display name of the seller. */
    private final String sellerName;

    /** Defensive copy of the auctioned item (never returned directly). */
    private final ItemStack item;

    /** Starting price set by the seller. */
    private final double startPrice;

    /** Buyout (instant-win) price, or 0 if disabled. */
    private final double buyoutPrice;

    /** Current highest bid amount. */
    private volatile double currentBid;

    /** UUID of the current top bidder, or null if no bids. */
    private volatile UUID topBidderUUID;

    /** Display name of the current top bidder, or null if no bids. */
    private volatile String topBidderName;

    /** Full bid history in chronological order. */
    private final List<BidRecord> bidHistory;

    /** Epoch milliseconds when this listing was created. */
    private final long createdAt;

    /** Epoch milliseconds when this listing expires. */
    private volatile long endTime;

    /** Whether this listing is still active. */
    private volatile boolean active;

    /** Whether the seller had tax bypass permission at creation. */
    private volatile boolean taxExempt;

    /** Whether this is a BIN-only listing (no bidding allowed). */
    private final boolean binOnly;

    /** Whitelisted player UUIDs for private auctions (null = public). */
    private volatile List<UUID> whitelist = null;

    /**
     * Constructs a new AuctionListing.
     * The item is stored as a defensive copy to prevent external mutation.
     */
    public AuctionListing(String id, UUID sellerUUID, String sellerName, ItemStack item,
            double startPrice, double buyoutPrice, long createdAt, long endTime) {
        this(id, sellerUUID, sellerName, item, startPrice, buyoutPrice, createdAt, endTime, false);
    }

    public AuctionListing(String id, UUID sellerUUID, String sellerName, ItemStack item,
            double startPrice, double buyoutPrice, long createdAt, long endTime, boolean binOnly) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.startPrice = startPrice;
        this.buyoutPrice = buyoutPrice;
        this.currentBid = startPrice;
        this.topBidderUUID = null;
        this.topBidderName = null;
        this.bidHistory = Collections.synchronizedList(new ArrayList<>());
        this.createdAt = createdAt;
        this.endTime = endTime;
        this.active = true;
        this.binOnly = binOnly;
        this.taxExempt = false;
    }

    /**
     * Full constructor for rebuilding listings from storage (includes bid state).
     */
    public AuctionListing(String id, UUID sellerUUID, String sellerName, ItemStack item,
            double startPrice, double buyoutPrice, double currentBid,
            UUID topBidderUUID, String topBidderName,
            long createdAt, long endTime) {
        this(id, sellerUUID, sellerName, item, startPrice, buyoutPrice, currentBid,
                topBidderUUID, topBidderName, createdAt, endTime, false);
    }

    public AuctionListing(String id, UUID sellerUUID, String sellerName, ItemStack item,
            double startPrice, double buyoutPrice, double currentBid,
            UUID topBidderUUID, String topBidderName,
            long createdAt, long endTime, boolean binOnly) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.startPrice = startPrice;
        this.buyoutPrice = buyoutPrice;
        this.currentBid = currentBid;
        this.topBidderUUID = topBidderUUID;
        this.topBidderName = topBidderName;
        this.bidHistory = Collections.synchronizedList(new ArrayList<>());
        this.createdAt = createdAt;
        this.endTime = endTime;
        this.active = true;
        this.binOnly = binOnly;
        this.taxExempt = false;
    }

    /**
     * Applies a new bid to this listing. Thread-safe via synchronization.
     * Validates that the bid exceeds the current bid by at least the minimum
     * increment.
     */
    public synchronized void applyBid(UUID bidder, String bidderName, double amount, double minIncrement) {
        // Validate that the new bid exceeds the current bid by at least the min
        // increment
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

    /** Returns seconds remaining until this auction expires (minimum 0). */
    public long getTimeRemainingSeconds() {
        return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
    }

    /** Returns true if this auction has expired. */
    public boolean isExpired() {
        return getTimeRemainingSeconds() <= 0;
    }

    /** Returns true if any bids have been placed on this listing. */
    public boolean hasBids() {
        return topBidderUUID != null;
    }

    /**
     * Builds a display-ready clone of the item with auction info lore.
     * Reads lore format from config at call-time for hot-reload support.
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
        String noBidderDefault = "§7" + MessageUtil.getMessage("no-bids-yet");
        String noBidderPlaceholder = config.getString("display.no-bidder-placeholder",
                noBidderDefault.isEmpty() ? "§7No bids yet" : noBidderDefault);

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

        // Format buyout price display
        String buyoutDisplay = (buyoutPrice > 0) ? String.format("%.2f", buyoutPrice)
                : "§7" + MessageUtil.getMessage("buyout-not-available");

        // Replace placeholders in each lore line from the template, convert to
        // Components
        for (String line : loreTemplate) {
            String processed = line
                    .replace("{seller}", sellerName)
                    .replace("{start_price}", String.format("%.2f", startPrice))
                    .replace("{current_bid}", String.format("%.2f", currentBid))
                    .replace("{buyout}", buyoutDisplay)
                    .replace("{buyout_price}", buyoutDisplay)
                    .replace("{top_bidder}", topBidderDisplay)
                    .replace("{time_left}", timeLeft)
                    .replace("{listing_id}", id);
            lore.add(ColorUtil.toComponent(processed));
        }

        meta.lore(lore);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    /** Returns a defensive copy of the stored item. */
    public ItemStack getItem() {
        return item.clone();
    }

    /** Returns the unique listing ID. */
    public String getId() {
        return id;
    }

    /** Returns the UUID of the seller. */
    public UUID getSellerUUID() {
        return sellerUUID;
    }

    /** Returns the display name of the seller. */
    public String getSellerName() {
        return sellerName;
    }

    /** Returns the starting price. */
    public double getStartPrice() {
        return startPrice;
    }

    /** Returns the buyout price, or 0 if no buyout. */
    public double getBuyoutPrice() {
        return buyoutPrice;
    }

    /** Returns the current highest bid amount. */
    public double getCurrentBid() {
        return currentBid;
    }

    /** Returns the UUID of the current top bidder, or null. */
    public UUID getTopBidderUUID() {
        return topBidderUUID;
    }

    /** Returns the name of the current top bidder, or null. */
    public String getTopBidderName() {
        return topBidderName;
    }

    /** Returns an unmodifiable view of the bid history. */
    public List<BidRecord> getBidHistory() {
        return Collections.unmodifiableList(new ArrayList<>(bidHistory));
    }

    /** Returns the epoch millis when this listing was created. */
    public long getCreatedAt() {
        return createdAt;
    }

    /** Returns the epoch millis when this listing expires. */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time (used on server startup to recalculate from remaining
     * seconds).
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /** Returns whether this listing is still active. */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the active state of this listing (should only be called by
     * AuctionManager).
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** Returns true if this is a BIN-only listing. */
    public boolean isBinOnly() {
        return binOnly;
    }

    /** Returns whether the seller had tax bypass at listing creation. */
    public boolean isTaxExempt() {
        return taxExempt;
    }

    /** Sets the tax exempt status (should be called at listing creation time). */
    public void setTaxExempt(boolean taxExempt) {
        this.taxExempt = taxExempt;
    }

    /**
     * Rolls back the most recent bid, restoring the previous bidder state.
     * Used when an AuctifyBidEvent is cancelled after applyBid() has mutated
     * listing state.
     */
    public synchronized void rollbackBid(UUID previousBidder, double previousAmount, boolean hadBid) {
        if (hadBid) {
            this.topBidderUUID = previousBidder;
            // Restore name from bid history if possible
            this.topBidderName = null;
            for (int i = bidHistory.size() - 1; i >= 0; i--) {
                if (bidHistory.get(i).bidderUUID().equals(previousBidder)) {
                    this.topBidderName = bidHistory.get(i).bidderName();
                    break;
                }
            }
            this.currentBid = previousAmount;
        } else {
            this.topBidderUUID = null;
            this.topBidderName = null;
            this.currentBid = startPrice;
        }
        // Remove the last bid record (the one being rolled back)
        if (!bidHistory.isEmpty()) {
            bidHistory.remove(bidHistory.size() - 1);
        }
    }

    // ─── Whitelist Management ───

    /** Sets the whitelist for this auction (null or empty = public). */
    public void setWhitelist(List<UUID> whitelist) {
        this.whitelist = whitelist != null && !whitelist.isEmpty() ? new ArrayList<>(whitelist) : null;
    }

    /** Returns the whitelist for this auction (null if public). */
    public List<UUID> getWhitelist() {
        return whitelist != null ? new ArrayList<>(whitelist) : null;
    }

    /** Returns true if this auction is private (has a whitelist). */
    public boolean isPrivate() {
        return whitelist != null && !whitelist.isEmpty();
    }

    /** Returns true if the player is allowed to bid on this auction. */
    public boolean canBid(UUID playerUUID) {
        if (!isPrivate())
            return true;
        return whitelist.contains(playerUUID);
    }
}
