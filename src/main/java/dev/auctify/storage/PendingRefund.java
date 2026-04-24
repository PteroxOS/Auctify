package dev.auctify.storage;

import java.util.UUID;

/**
 * Represents a pending money refund owed to a player. Used when
 * economy.deposit() fails during auction resolution. Delivered to the player on
 * their next login.
 */
public record PendingRefund(
        UUID playerUUID,
        double amount,
        String reason,
        long createdAt) {
}
