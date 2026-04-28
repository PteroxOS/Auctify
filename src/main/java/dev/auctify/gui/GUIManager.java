package dev.auctify.gui;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Tracks which GUI each player currently has open and manages auto-refresh
 * tasks. Ensures proper cleanup when players close GUIs or disconnect.
 */
public class GUIManager {

    /**
     * Maps player UUID to the type of GUI they have open ("MAIN", "CONFIRM",
     * "DETAIL").
     */
    private final Map<UUID, String> openGUIs = new HashMap<>();

    /** Maps player UUID to their auto-refresh task for cancellation on close. */
    private final Map<UUID, BukkitTask> refreshTasks = new HashMap<>();

    /**
     * Maps player UUID to the listing ID they're currently viewing (for
     * confirm/detail).
     */
    private final Map<UUID, String> viewingListing = new HashMap<>();

    /** Maps player UUID to their current page number in the main GUI. */
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    /** Maps player UUID to their bulk buy list (listing IDs). */
    private final Map<UUID, List<String>> bulkBuyLists = new HashMap<>();

    /** Marks a player as having an Auctify GUI open. */
    public void markOpen(Player player, String guiType) {
        openGUIs.put(player.getUniqueId(), guiType);
    }

    /** Marks a player's GUI as closed and removes their tracking entry. */
    public void markClosed(Player player) {
        openGUIs.remove(player.getUniqueId());
        viewingListing.remove(player.getUniqueId());
    }

    /** Gets the type of GUI a player currently has open. */
    public Optional<String> getOpenGUI(Player player) {
        return Optional.ofNullable(openGUIs.get(player.getUniqueId()));
    }

    /**
     * Registers an auto-refresh task for a player, cancelling any existing task.
     */
    public void registerRefreshTask(Player player, BukkitTask task) {
        cancelRefreshTask(player);
        refreshTasks.put(player.getUniqueId(), task);
    }

    /** Cancels and removes a player's auto-refresh task. */
    public void cancelRefreshTask(Player player) {
        BukkitTask task = refreshTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Sets which listing a player is currently viewing (for confirm/detail GUIs).
     */
    public void setViewingListing(Player player, String listingId) {
        viewingListing.put(player.getUniqueId(), listingId);
    }

    /** Gets which listing a player is currently viewing. */
    public Optional<String> getViewingListing(Player player) {
        return Optional.ofNullable(viewingListing.get(player.getUniqueId()));
    }

    /** Gets the player's current page number in the main GUI. */
    public int getPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 0);
    }

    /** Sets the player's current page number. */
    public void setPage(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
    }

    /**
     * Full cleanup for a player: closes GUI state, cancels refresh, removes page
     * tracking.
     * H-5 HIGH FIX: Memastikan viewingListing juga di-cleanup via markClosed()
     */
    public void cleanup(Player player) {
        markClosed(player); // H-5: Ini juga menghapus viewingListing via markClosed()
        cancelRefreshTask(player);
        playerPages.remove(player.getUniqueId());
        bulkBuyLists.remove(player.getUniqueId());
        // H-5: viewingListing sudah dihapus di markClosed() - jangan hapus di sini
        // untuk menghindari double-remove yang mungkin tidak di-sync
    }

    /** Gets the player's bulk buy list. */
    public List<String> getBulkBuyList(UUID playerUUID) {
        return bulkBuyLists.computeIfAbsent(playerUUID, k -> new ArrayList<>());
    }

    /** Sets the player's bulk buy list. */
    public void setBulkBuyList(UUID playerUUID, List<String> list) {
        bulkBuyLists.put(playerUUID, list);
    }
}
