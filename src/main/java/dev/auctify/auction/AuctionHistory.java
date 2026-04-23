package dev.auctify.auction;

import java.util.UUID;

/**
 * Immutable record representing a completed auction.
 * Created when an auction is resolved (either sold, expired, or cancelled) and
 * persisted to the history table for player lookup via {@code /ac history}.
 *
 * @param id          the unique listing ID (8-char string)
 * @param sellerUUID  the UUID of the seller who created the listing
 * @param sellerName  the display name of the seller
 * @param winnerUUID  the UUID of the auction winner, or null if no bids
 * @param winnerName  the display name of the winner, or null if no bids
 * @param itemData    the Base64-encoded item data for display in history
 * @param startPrice  the initial starting price of the auction
 * @param finalPrice  the final sale price (highest bid or buyout price)
 * @param taxAmount   the tax amount deducted from the sale price
 * @param resolvedAt  the epoch milliseconds when the auction was resolved
 * @param reason      the reason for resolution (e.g., "SOLD", "EXPIRED", "CANCELLED")
 */
public record AuctionHistory(
        String id,
        UUID sellerUUID,
        String sellerName,
        UUID winnerUUID,
        String winnerName,
        String itemData,
        double startPrice,
        double finalPrice,
        double taxAmount,
        long resolvedAt,
        String reason
) {
}
