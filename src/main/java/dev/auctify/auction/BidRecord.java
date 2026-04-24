package dev.auctify.auction;

import java.util.UUID;

/** Immutable record representing a single bid placed on an auction listing. */
public record BidRecord(UUID bidderUUID, String bidderName, double amount, long timestamp) {
}
