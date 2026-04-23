package dev.auctify.listeners;

import dev.auctify.Auctify;
import dev.auctify.gui.AuctifyHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Cleans up GUI tracking state when a player closes an Auctify inventory.
 * Uses {@link AuctifyHolder} for reliable detection of Auctify GUIs.
 */
public class InventoryCloseListener implements Listener {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public InventoryCloseListener(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when any inventory is closed. If the inventory has an AuctifyHolder,
     * performs cleanup of refresh tasks and GUI tracking state.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Only clean up if the closed inventory was an Auctify GUI
        if (event.getInventory().getHolder() instanceof AuctifyHolder) {
            plugin.getGUIManager().cleanup(player);
        }
    }
}
