package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.storage.PendingRefund;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

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
     * Delivers pending items and refunds when a player joins the server.
     * Items are accumulated from auctions that resolved while the player was
     * offline.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if setup wizard should show (first-run admin)
        plugin.getSetupWizard().onPlayerJoin(player);

        // Check for pending deliveries asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ItemStack> pending = plugin.getAuctionManager().claimPendingDeliveries(player.getUniqueId());
            // Atomic fetch-and-clear for refunds (prevents double-delivery)
            List<PendingRefund> refunds = plugin.getStorageManager().claimAndClearRefunds(player.getUniqueId());

            if (!pending.isEmpty() || !refunds.isEmpty()) {
                // Deliver on the main thread safely
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline())
                        return; // Player left during query

                    // Deliver items
                    if (!pending.isEmpty()) {
                        int dropped = 0;
                        for (ItemStack item : pending) {
                            java.util.HashMap<Integer, ItemStack> overflow = player.getInventory()
                                    .addItem(item.clone());
                            if (!overflow.isEmpty()) {
                                // Drop on ground if full
                                for (ItemStack drop : overflow.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), drop.clone());
                                    dropped++;
                                    MessageUtil.send(player, "pending-item-dropped",
                                            Map.of("item", ItemUtil.getDisplayName(drop)));
                                }
                            }
                        }
                        MessageUtil.send(player, "pending-items-received",
                                Map.of("count", String.valueOf(pending.size())));
                    }

                    // Deliver pending refunds
                    if (!refunds.isEmpty()) {
                        double totalRefunded = 0;
                        for (PendingRefund refund : refunds) {
                            if (plugin.getEconomyManager().isAvailable()) {
                                plugin.getEconomyManager().deposit(player.getUniqueId(), refund.amount());
                                totalRefunded += refund.amount();
                            }
                        }
                        MessageUtil.send(player, "pending-refunds-received",
                                Map.of("amount", plugin.getEconomyManager().format(totalRefunded)));
                        // Log each refund for audit trail
                        refunds.forEach(r -> plugin.getLogger().info(
                                "[Auctify] Delivered pending refund of " + plugin.getEconomyManager().format(r.amount())
                                        + " to " + player.getName() + " — reason: " + r.reason()));
                    }
                });
            }
        });
    }
}
