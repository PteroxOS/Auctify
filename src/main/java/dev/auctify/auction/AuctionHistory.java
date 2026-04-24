package dev.auctify.auction;

import java.util.UUID;

/**
 * Immutable record representing a completed auction. Created when an auction is
 * resolved (sold, expired, or cancelled) and persisted to the history table for
 * player lookup via /ac history.
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
                String reason) {
}
