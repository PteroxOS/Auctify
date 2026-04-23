package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.MessageUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for chat messages from players in bid-input mode.
 * When a player is awaiting bid input, their chat message is intercepted,
 * parsed as a bid amount, and the bid is placed on the main thread.
 *
 * <p>THREAD SAFETY: This event fires on an async thread. All Bukkit API calls
 * (like placing bids) are dispatched back to the main thread via
 * {@code Bukkit.getScheduler().runTask(...)}.</p>
 */
public class ChatBidListener implements Listener {

    private final Auctify plugin;

    /** Maps player UUID to the listing ID they're bidding on. Thread-safe. */
    private final Map<UUID, String> awaitingBid = new ConcurrentHashMap<>();

    /** @param plugin the main plugin instance */
    public ChatBidListener(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Puts a player into bid-input mode for a specific listing.
     *
     * @param player    the player
     * @param listingId the listing they want to bid on
     */
    public void startBidInput(Player player, String listingId) {
        awaitingBid.put(player.getUniqueId(), listingId);
    }

    /**
     * Removes a player from bid-input mode.
     *
     * @param player the player
     */
    public void cancelBidInput(Player player) {
        awaitingBid.remove(player.getUniqueId());
    }

    /**
     * Removes a player from bid-input mode by UUID (for quit cleanup).
     *
     * @param uuid the player's UUID
     */
    public void cancelBidInput(UUID uuid) {
        awaitingBid.remove(uuid);
    }

    /**
     * Checks if a player is currently in bid-input mode.
     *
     * @param player the player
     * @return true if awaiting bid input
     */
    public boolean isAwaitingBid(Player player) {
        return awaitingBid.containsKey(player.getUniqueId());
    }

    /**
     * Intercepts chat messages from players in bid-input mode.
     * Runs on an async thread — dispatches Bukkit API calls to main thread.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String listingId = awaitingBid.get(player.getUniqueId());

        // Only intercept if this player is in bid mode
        if (listingId == null) return;

        // Cancel the chat event to prevent public message
        event.setCancelled(true);

        // Extract plain text from the Adventure component
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // Check for cancellation
        if (message.equalsIgnoreCase("cancel")) {
            awaitingBid.remove(player.getUniqueId());
            // Send cancel message on main thread
            Bukkit.getScheduler().runTask(plugin, () ->
                    MessageUtil.send(player, "chat-cancelled", null));
            return;
        }

        // Parse the bid amount
        double amount;
        try {
            amount = Double.parseDouble(message);
        } catch (NumberFormatException e) {
            // Re-prompt the player — invalid number
            Bukkit.getScheduler().runTask(plugin, () ->
                    MessageUtil.send(player, "chat-invalid-number", null));
            return; // Keep them in bid mode
        }

        if (amount <= 0) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    MessageUtil.send(player, "invalid-price", null));
            return; // Keep in bid mode
        }

        // Do NOT remove from awaiting map yet.
        // Dispatch bid placement to the main thread.
        final double bidAmount = amount;
        final String finalListingId = listingId;
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean success = plugin.getAuctionManager().placeBid(player, finalListingId, bidAmount);
            if (success) {
                // Bid succeeded, remove from chat prompt mode
                awaitingBid.remove(player.getUniqueId());
            } else {
                // Bid failed (e.g. too low). The AuctionManager already sent the error message.
                // We just remind them they can still type or cancel.
                MessageUtil.send(player, "chat-bid-retry", null);
            }
        });
    }
}
