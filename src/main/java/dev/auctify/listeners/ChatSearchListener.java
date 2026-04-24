package dev.auctify.listeners;

import dev.auctify.Auctify;
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
 * Listens for chat messages from players in search-input mode.
 * When a player is awaiting search input, their chat message is intercepted
 * and used as the search query.
 */
public class ChatSearchListener implements Listener {

    private final Auctify plugin;
    private final Map<UUID, Long> searchInputStartTime = new ConcurrentHashMap<>();
    private final int searchTimeoutSeconds = 30;

    public ChatSearchListener(Auctify plugin) {
        this.plugin = plugin;
        // Cleanup task every 30 seconds
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupExpiredSearchInputs, 20L * 30, 20L * 30);
    }

    /**
     * Puts a player into search-input mode.
     *
     * @param player the player
     */
    public void startSearchInput(Player player) {
        searchInputStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        MessageUtil.send(player, "search-prompt", null);
    }

    /**
     * Removes expired search-input entries.
     */
    private void cleanupExpiredSearchInputs() {
        long now = System.currentTimeMillis();
        long timeoutMs = searchTimeoutSeconds * 1000L;
        searchInputStartTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > timeoutMs) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "search-timeout", null));
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Removes a player from search-input mode.
     *
     * @param player the player
     */
    public void cancelSearchInput(Player player) {
        searchInputStartTime.remove(player.getUniqueId());
    }

    /**
     * Checks if a player is currently in search-input mode.
     *
     * @param player the player
     * @return true if awaiting search input
     */
    public boolean isAwaitingSearch(Player player) {
        return searchInputStartTime.containsKey(player.getUniqueId());
    }

    /**
     * Intercepts chat messages from players in search-input mode.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!searchInputStartTime.containsKey(player.getUniqueId()))
            return;

        // Cancel the chat event to prevent public message
        event.setCancelled(true);

        // Extract plain text from the Adventure component
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // Check for cancellation
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("batal")) {
            searchInputStartTime.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "search-cancelled", null));
            return;
        }

        if (message.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, "search-empty", null));
            return;
        }

        // Valid search query - process it
        final String query = message;
        searchInputStartTime.remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            // Perform search
            plugin.getAuctionGUI().open(player, 0, "ALL", "TIME_ASC", query);
            MessageUtil.send(player, "search-results", Map.of("query", query));
        });
    }
}
