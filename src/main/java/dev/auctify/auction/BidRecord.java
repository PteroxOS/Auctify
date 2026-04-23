package dev.auctify.auction;

import java.util.UUID;

/**
 * Immutable record representing a single bid placed on an auction listing.
 * Stores the bidder's identity, the bid amount, and the timestamp when the bid was placed.
 * These records are accumulated in an {@link AuctionListing}'s bid history for display
 * in the item detail GUI and for auditing purposes.
 *
 * @param bidderUUID the UUID of the player who placed the bid
 * @param bidderName the display name of the bidder at the time of bidding
 * @param amount     the bid amount in the server's currency
 * @param timestamp  the epoch milliseconds when the bid was placed
 */
public record BidRecord(UUID bidderUUID, String bidderName, double amount, long timestamp) {
}
