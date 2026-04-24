package dev.auctify.notification;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages auction notifications for players.
 * Handles expiration warnings, outbid alerts, buyout notifications, etc.
 */
public class NotificationManager {

    private final Auctify plugin;
    
    /** Track players who have been notified about specific events to avoid spam. */
    private final Map<UUID, Set<String>> notifiedEvents = new ConcurrentHashMap<>();
    
    /** Scheduled tasks for expiration warnings. */
    private final Map<String, BukkitTask> expirationTasks = new ConcurrentHashMap<>();
    
    /** Player notification preferences. */
    private final Map<UUID, NotificationPreferences> playerPreferences = new ConcurrentHashMap<>();

    public NotificationManager(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Send a notification to a player if they have the preference enabled.
     *
     * @param playerUUID the player's UUID
     * @param type the notification type
     * @param messageKey the locale message key
     * @param placeholders placeholder values for the message
     */
    public void notify(UUID playerUUID, NotificationType type, String messageKey, Map<String, String> placeholders) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }

        NotificationPreferences prefs = playerPreferences.get(playerUUID);
        if (prefs != null && !prefs.isEnabled(type)) {
            return;
        }

        MessageUtil.send(player, messageKey, placeholders);

        // Play notification sound if enabled
        if (plugin.getConfig().getBoolean("notifications.sound", true)) {
            playNotificationSound(player, type);
        }
    }

    /**
     * Send an outbid notification to the previous top bidder.
     *
     * @param listing the auction listing
     * @param previousBidderUUID the UUID of the previous top bidder
     * @param newBidAmount the new bid amount
     */
    public void notifyOutbid(AuctionListing listing, UUID previousBidderUUID, double newBidAmount) {
        String eventKey = "outbid:" + listing.getId();
        if (hasBeenNotified(previousBidderUUID, eventKey)) {
            return;
        }

        markNotified(previousBidderUUID, eventKey);

        notify(previousBidderUUID, NotificationType.OUTBID, "notification-outbid", Map.of(
                "item", listing.getItem().getType().name(),
                "amount", plugin.getEconomyManager().format(newBidAmount),
                "listing_id", listing.getId()
        ));
    }

    /**
     * Send a buyout notification to the seller.
     *
     * @param listing the auction listing
     * @param buyerUUID the UUID of the buyer
     * @param buyerName the name of the buyer
     */
    public void notifyBuyout(AuctionListing listing, UUID buyerUUID, String buyerName) {
        notify(listing.getSellerUUID(), NotificationType.BUYOUT, "notification-buyout", Map.of(
                "item", listing.getItem().getType().name(),
                "price", plugin.getEconomyManager().format(listing.getBuyoutPrice()),
                "buyer", buyerName,
                "listing_id", listing.getId()
        ));
    }

    /**
     * Send an auction won notification to the winner.
     *
     * @param listing the auction listing
     * @param winnerUUID the UUID of the winner
     */
    public void notifyAuctionWon(AuctionListing listing, UUID winnerUUID) {
        notify(winnerUUID, NotificationType.AUCTION_WON, "notification-auction-won", Map.of(
                "item", listing.getItem().getType().name(),
                "amount", plugin.getEconomyManager().format(listing.getCurrentBid()),
                "listing_id", listing.getId()
        ));
    }

    /**
     * Send an item sold notification to the seller.
     *
     * @param listing the auction listing
     * @param winnerUUID the UUID of the winner
     * @param winnerName the name of the winner
     * @param amount the final amount
     * @param tax the tax amount
     * @param net the net amount after tax
     */
    public void notifyItemSold(AuctionListing listing, UUID winnerUUID, String winnerName, double amount, double tax, double net) {
        notify(listing.getSellerUUID(), NotificationType.ITEM_SOLD, "notification-item-sold", Map.of(
                "item", listing.getItem().getType().name(),
                "amount", plugin.getEconomyManager().format(amount),
                "winner", winnerName,
                "tax", String.format("%.1f%%", tax),
                "net", plugin.getEconomyManager().format(net),
                "listing_id", listing.getId()
        ));
    }

    /**
     * Schedule an expiration warning notification for a listing.
     *
     * @param listing the auction listing
     * @param warningMinutes how many minutes before expiration to warn
     */
    public void scheduleExpirationWarning(AuctionListing listing, int warningMinutes) {
        long warningTime = listing.getEndTime() - (warningMinutes * 60L * 1000L);
        long delay = warningTime - System.currentTimeMillis();

        if (delay <= 0) {
            return; // Already past warning time
        }

        String taskKey = "expiration:" + listing.getId();
        
        // Cancel existing task if any
        BukkitTask existingTask = expirationTasks.get(taskKey);
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (listing.isActive() && !listing.isExpired()) {
                notify(listing.getSellerUUID(), NotificationType.EXPIRATION, "notification-expiration-warning", Map.of(
                        "item", listing.getItem().getType().name(),
                        "minutes", String.valueOf(warningMinutes),
                        "listing_id", listing.getId()
                ));
            }
            expirationTasks.remove(taskKey);
        }, delay / 50L); // Convert ms to ticks

        expirationTasks.put(taskKey, task);
    }

    /**
     * Cancel an expiration warning task for a listing.
     *
     * @param listingId the listing ID
     */
    public void cancelExpirationWarning(String listingId) {
        String taskKey = "expiration:" + listingId;
        BukkitTask task = expirationTasks.remove(taskKey);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Set notification preferences for a player.
     *
     * @param playerUUID the player's UUID
     * @param preferences the preferences
     */
    public void setPreferences(UUID playerUUID, NotificationPreferences preferences) {
        playerPreferences.put(playerUUID, preferences);
    }

    /**
     * Get notification preferences for a player.
     *
     * @param playerUUID the player's UUID
     * @return the preferences, or default if not set
     */
    public NotificationPreferences getPreferences(UUID playerUUID) {
        return playerPreferences.getOrDefault(playerUUID, new NotificationPreferences());
    }

    /**
     * Clear notification history for a player.
     *
     * @param playerUUID the player's UUID
     */
    public void clearNotificationHistory(UUID playerUUID) {
        notifiedEvents.remove(playerUUID);
    }

    private boolean hasBeenNotified(UUID playerUUID, String eventKey) {
        Set<String> events = notifiedEvents.get(playerUUID);
        return events != null && events.contains(eventKey);
    }

    private void markNotified(UUID playerUUID, String eventKey) {
        notifiedEvents.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet()).add(eventKey);
    }

    private void playNotificationSound(Player player, NotificationType type) {
        Sound sound = switch (type) {
            case OUTBID -> Sound.ENTITY_VILLAGER_NO;
            case BUYOUT, AUCTION_WON, ITEM_SOLD -> Sound.ENTITY_PLAYER_LEVELUP;
            case EXPIRATION -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            default -> Sound.BLOCK_NOTE_BLOCK_PLING;
        };

        float volume = (float) plugin.getConfig().getDouble("notifications.volume", 0.5);
        float pitch = (float) plugin.getConfig().getDouble("notifications.pitch", 1.0);

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Clean up expired notification history.
     */
    public void cleanup() {
        notifiedEvents.clear();
        expirationTasks.values().forEach(BukkitTask::cancel);
        expirationTasks.clear();
    }

    /**
     * Notification types.
     */
    public enum NotificationType {
        OUTBID,
        BUYOUT,
        AUCTION_WON,
        ITEM_SOLD,
        EXPIRATION
    }

    /**
     * Notification preferences for a player.
     */
    public static class NotificationPreferences {
        private boolean outbidEnabled = true;
        private boolean buyoutEnabled = true;
        private boolean auctionWonEnabled = true;
        private boolean itemSoldEnabled = true;
        private boolean expirationEnabled = true;

        public boolean isEnabled(NotificationType type) {
            return switch (type) {
                case OUTBID -> outbidEnabled;
                case BUYOUT -> buyoutEnabled;
                case AUCTION_WON -> auctionWonEnabled;
                case ITEM_SOLD -> itemSoldEnabled;
                case EXPIRATION -> expirationEnabled;
            };
        }

        public void setEnabled(NotificationType type, boolean enabled) {
            switch (type) {
                case OUTBID -> outbidEnabled = enabled;
                case BUYOUT -> buyoutEnabled = enabled;
                case AUCTION_WON -> auctionWonEnabled = enabled;
                case ITEM_SOLD -> itemSoldEnabled = enabled;
                case EXPIRATION -> expirationEnabled = enabled;
            }
        }
    }
}
