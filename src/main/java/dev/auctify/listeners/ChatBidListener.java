package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.ItemUtil;
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
 * <p>
 * THREAD SAFETY: This event fires on an async thread. All Bukkit API calls
 * (like placing bids) are dispatched back to the main thread via
 * {@code Bukkit.getScheduler().runTask(...)}.
 * </p>
 */
public class ChatBidListener implements Listener {

    private final Auctify plugin;

    /** Maps player UUID to the listing ID they're bidding on. Thread-safe. */
    private final Map<UUID, String> awaitingBid = new ConcurrentHashMap<>();

    /**
     * Maps player UUID to the timestamp when bid-input mode started. Used for
     * timeout cleanup.
     */
    private final Map<UUID, Long> bidInputStartTime = new ConcurrentHashMap<>();

    private final int bidTimeoutSeconds; // 🔧 CONFIGURABLE via config.yml: gui.bid-input-timeout

    /** @param plugin the main plugin instance */
    public ChatBidListener(Auctify plugin) {
        this.plugin = plugin;
        // 🔧 CONFIGURABLE: bid-input timeout in seconds (default 30)
        this.bidTimeoutSeconds = plugin.getConfig().getInt("gui.bid-input-timeout", 30);
        // Schedule periodic cleanup of expired bid-input sessions
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpiredBidInputs, 20L * 30, 20L * 30);
    }

    /**
     * Puts a player into bid-input mode for a specific listing.
     *
     * @param player    the player
     * @param listingId the listing they want to bid on
     */
    public void startBidInput(Player player, String listingId) {
        awaitingBid.put(player.getUniqueId(), listingId);
        bidInputStartTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Removes expired bid-input entries (older than bidTimeoutSeconds).
     */
    private void cleanupExpiredBidInputs() {
        long now = System.currentTimeMillis();
        long timeoutMs = bidTimeoutSeconds * 1000L;
        awaitingBid.entrySet().removeIf(entry -> {
            Long start = bidInputStartTime.get(entry.getKey());
            if (start != null && now - start > timeoutMs) {
                bidInputStartTime.remove(entry.getKey());
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    // Get listing ID and item name for the timeout message
                    String listingId = entry.getValue();
                    var listingOpt = plugin.getAuctionManager().getListingById(listingId);
                    String itemName = listingOpt.map(l -> ItemUtil.getDisplayName(l.getItem())).orElse("Unknown");
                    Bukkit.getScheduler().runTask(plugin,
                            () -> MessageUtil.send(player, "chat-bid-timeout", Map.of("item", itemName)));
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Removes a player from bid-input mode.
     *
     * @param player the player
     */
    public void cancelBidInput(Player player) {
        awaitingBid.remove(player.getUniqueId());
        bidInputStartTime.remove(player.getUniqueId());
    }

    /**
     * Removes a player from bid-input mode by UUID (for quit cleanup).
     *
     * @param uuid the player's UUID
     */
    public void cancelBidInput(UUID uuid) {
        awaitingBid.remove(uuid);
        bidInputStartTime.remove(uuid);
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
        if (listingId == null)
            return;

        // Cancel the chat event to prevent public message
        event.setCancelled(true);

        // Extract plain text from the Adventure component
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // Check for cancellation
        if (message.equalsIgnoreCase("cancel")) {
            awaitingBid.remove(player.getUniqueId());
            // Send cancel message on main thread
            Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "chat-cancelled", null));
            return;
        }

        // Parse the bid amount
        double amount;
        try {
            amount = Double.parseDouble(message);
        } catch (NumberFormatException e) {
            // Re-prompt the player — invalid number
            Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "chat-invalid-number", null));
            return; // Keep them in bid mode
        }

        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "invalid-price", null));
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
                bidInputStartTime.remove(player.getUniqueId());
            } else {
                // Bid failed (e.g. too low). The AuctionManager already sent the error message.
                // We just remind them they can still type or cancel.
                MessageUtil.send(player, "chat-bid-retry", null);
            }
        });
    }
}
