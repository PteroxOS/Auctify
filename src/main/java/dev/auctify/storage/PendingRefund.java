package dev.auctify.storage;

import java.util.UUID;

/**
 * Represents a pending money refund owed to a player.
 * Used when economy.deposit() fails during auction resolution.
 * Delivered to the player on their next login.
 *
 * @param playerUUID who is owed the money
 * @param amount     how much
 * @param reason     human-readable audit trail e.g. "Auction ABC123 resolution"
 * @param createdAt  epoch millis — for future expiry/cleanup if needed
 */
public record PendingRefund(
    UUID playerUUID,
    double amount,
    String reason,
    long createdAt
) {
}
