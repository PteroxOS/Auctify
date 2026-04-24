package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Handles player join events for pending deliveries and notifications.
 */
public class PlayerJoinListener implements Listener {

    private final Auctify plugin;

    public PlayerJoinListener(Auctify plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Deliver pending buy order items
        deliverPendingBuyOrders(player);
    }

    /**
     * Delivers pending buy order items to the player.
     */
    private void deliverPendingBuyOrders(Player player) {
        List<ItemStack> pendingItems = plugin.getStorageManager().getPendingBuyDeliveries(player.getUniqueId());
        
        if (pendingItems.isEmpty()) {
            return;
        }

        int delivered = 0;
        for (ItemStack item : pendingItems) {
            if (item == null || item.getType().isAir()) continue;
            
            // Try to add to inventory
            java.util.HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            
            if (!overflow.isEmpty()) {
                // Drop items if inventory full
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            delivered++;
        }

        // Clear pending deliveries after attempt
        plugin.getStorageManager().clearPendingBuyDeliveries(player.getUniqueId());

        if (delivered > 0) {
            MessageUtil.send(player, "buyorder-delivered", Map.of(
                "count", String.valueOf(delivered)
            ));
            plugin.getSoundManager().playSuccess(player);
        }
    }
}
