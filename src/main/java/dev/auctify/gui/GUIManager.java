package dev.auctify.gui;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Tracks which GUI each player currently has open and manages auto-refresh tasks.
 * Ensures proper cleanup when players close GUIs or disconnect.
 */
public class GUIManager {

    /** Maps player UUID to the type of GUI they have open ("MAIN", "CONFIRM", "DETAIL"). */
    private final Map<UUID, String> openGUIs = new HashMap<>();

    /** Maps player UUID to their auto-refresh task for cancellation on close. */
    private final Map<UUID, BukkitTask> refreshTasks = new HashMap<>();

    /** Maps player UUID to the listing ID they're currently viewing (for confirm/detail). */
    private final Map<UUID, String> viewingListing = new HashMap<>();

    /** Maps player UUID to their current page number in the main GUI. */
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    /**
     * Marks a player as having an Auctify GUI open.
     *
     * @param player  the player
     * @param guiType the GUI type identifier ("MAIN", "CONFIRM", "DETAIL")
     */
    public void markOpen(Player player, String guiType) {
        openGUIs.put(player.getUniqueId(), guiType);
    }

    /**
     * Marks a player's GUI as closed and removes their tracking entry.
     *
     * @param player the player
     */
    public void markClosed(Player player) {
        openGUIs.remove(player.getUniqueId());
        viewingListing.remove(player.getUniqueId());
    }

    /**
     * Gets the type of GUI a player currently has open.
     *
     * @param player the player
     * @return an Optional containing the GUI type, or empty if none is open
     */
    public Optional<String> getOpenGUI(Player player) {
        return Optional.ofNullable(openGUIs.get(player.getUniqueId()));
    }

    /**
     * Registers an auto-refresh BukkitTask for a player. Cancels any existing task first.
     *
     * @param player the player
     * @param task   the repeating task to track
     */
    public void registerRefreshTask(Player player, BukkitTask task) {
        cancelRefreshTask(player);
        refreshTasks.put(player.getUniqueId(), task);
    }

    /**
     * Cancels and removes a player's auto-refresh task.
     *
     * @param player the player
     */
    public void cancelRefreshTask(Player player) {
        BukkitTask task = refreshTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Sets which listing a player is currently viewing (for confirm/detail GUIs).
     *
     * @param player    the player
     * @param listingId the listing ID
     */
    public void setViewingListing(Player player, String listingId) {
        viewingListing.put(player.getUniqueId(), listingId);
    }

    /**
     * Gets which listing a player is currently viewing.
     *
     * @param player the player
     * @return an Optional with the listing ID, or empty
     */
    public Optional<String> getViewingListing(Player player) {
        return Optional.ofNullable(viewingListing.get(player.getUniqueId()));
    }

    /**
     * Gets the player's current page number in the main GUI.
     *
     * @param player the player
     * @return the page number (0-indexed), defaults to 0
     */
    public int getPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Sets the player's current page number.
     *
     * @param player the player
     * @param page   the page number (0-indexed)
     */
    public void setPage(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
    }

    /**
     * Full cleanup for a player: closes GUI state, cancels refresh, removes page tracking.
     *
     * @param player the player to clean up
     */
    public void cleanup(Player player) {
        markClosed(player);
        cancelRefreshTask(player);
        playerPages.remove(player.getUniqueId());
    }
}
