package dev.auctify.auction;

import java.util.UUID;

/**
 * Represents an auto-bid configuration for a player on a specific listing. When
 * the player is outbid, the system will automatically bid up to their max
 * amount.
 */
public class AutoBid {

    private final UUID playerUUID;
    private final String playerName;
    private final String listingId;
    private final double maxBidAmount;
    private final long createdAt;

    public AutoBid(UUID playerUUID, String playerName, String listingId, double maxBidAmount) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.listingId = listingId;
        this.maxBidAmount = maxBidAmount;
        this.createdAt = System.currentTimeMillis();
    }

    public AutoBid(UUID playerUUID, String playerName, String listingId, double maxBidAmount, long createdAt) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.listingId = listingId;
        this.maxBidAmount = maxBidAmount;
        this.createdAt = createdAt;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getListingId() {
        return listingId;
    }

    public double getMaxBidAmount() {
        return maxBidAmount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Check if this auto-bid can place a bid at the given amount.
     *
     * @param currentBid the current bid amount
     * @return true if maxBidAmount is greater than currentBid
     */
    public boolean canBid(double currentBid) {
        return maxBidAmount > currentBid;
    }
}
