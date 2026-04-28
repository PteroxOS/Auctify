package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.gui.StatsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Listener for Stats GUI to prevent item taking and handle clicks. */
public class StatsGUIListener implements Listener {

    private final Auctify plugin;

    public StatsGUIListener(Auctify plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof StatsGUI.StatsHolder) {
            // C-1 CRITICAL FIX: Unconditionally cancel ALL clicks pertama
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);

            // C-1: Hanya proses LEFT atau RIGHT clicks untuk button interaction
            if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
                return;
            }

            // Handle back button click (slot 49)
            if (event.getSlot() == 49) {
                if (event.getWhoClicked() instanceof Player player) {
                    plugin.getAuctionGUI().open(player, 0, "ALL", "TIME_ASC", null);
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
