package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles player join and quit events for Auctify.
 * On quit: cleans up GUI state and chat bid tracking.
 * On join: delivers any pending items from auctions won while offline.
 */
public class PlayerQuitListener implements Listener {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public PlayerQuitListener(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Cleans up all player state when they disconnect:
     * GUI tracking, refresh tasks, and chat bid mode.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up GUI state
        plugin.getGUIManager().cleanup(player);

        // Remove from chat bid mode to prevent memory leak
        plugin.getChatBidListener().cancelBidInput(player);
    }

    /**
     * Delivers pending items when a player joins the server.
     * Items are accumulated from auctions that resolved while the player was offline.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check for pending deliveries asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ItemStack> pending = plugin.getAuctionManager().claimPendingDeliveries(player.getUniqueId());

            if (!pending.isEmpty()) {
                // Deliver items on the main thread safely
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return; // Player left during query
                    for (ItemStack item : pending) {
                        if (player.getInventory().firstEmpty() == -1) {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        } else {
                            player.getInventory().addItem(item);
                        }
                    }
                    MessageUtil.sendRaw(player, "§aYou have received §f" + pending.size()
                            + " §aitem(s) from auctions that ended while you were offline!");
                });
            }
        });
    }
}
