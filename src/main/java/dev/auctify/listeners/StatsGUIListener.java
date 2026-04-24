package dev.auctify.listeners;

import dev.auctify.gui.StatsGUI;
import dev.auctify.gui.AuctionGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Listener for Stats GUI to prevent item taking and handle clicks.
 */
public class StatsGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof StatsGUI.StatsHolder) {
            event.setCancelled(true);
            
            // Handle back button click (slot 49)
            if (event.getSlot() == 49) {
                // Open main auction GUI
                if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
                    // This would need plugin instance, for now just close
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof StatsGUI.StatsHolder) {
            event.setCancelled(true);
        }
    }
}
