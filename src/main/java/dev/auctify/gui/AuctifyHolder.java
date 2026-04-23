package dev.auctify.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom InventoryHolder that marks an inventory as belonging to Auctify.
 * This is the most reliable way to detect Auctify GUIs because it survives
 * across all inventory events regardless of GUIManager state.
 *
 * <p>Every GUI created by Auctify (Main, Confirm, Detail, Manage) must use
 * this holder so that {@link dev.auctify.listeners.GUIClickListener} can
 * unconditionally cancel all clicks, preventing item theft.</p>
 */
public class AuctifyHolder implements InventoryHolder {

    /** The type of GUI this holder represents. */
    private final String guiType;

    /** Optional: The listing ID being viewed in this GUI. */
    private String listingId;

    /** Optional: The current page number for paginated GUIs. */
    private int page;

    /** Optional: The current category filter. */
    private String category = "ALL";

    /** Optional: The current sort mode (TIME_ASC, TIME_DESC, PRICE_ASC, PRICE_DESC, BIDS). */
    private String sortMode = "TIME_ASC";

    /** Optional: Target player UUID for admin views. */
    private String targetPlayerUUID;

    /**
     * Creates a new AuctifyHolder for a specific GUI type.
     *
     * @param guiType the GUI type identifier (e.g., "MAIN", "CONFIRM", "DETAIL", "MANAGE", "CLAIM", "ADMIN", "SHULKER", "RATE")
     */
    public AuctifyHolder(String guiType) {
        this.guiType = guiType;
    }

    public String getGuiType() {
        return guiType;
    }

    public String getListingId() {
        return listingId;
    }

    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSortMode() {
        return sortMode;
    }

    public void setSortMode(String sortMode) {
        this.sortMode = sortMode;
    }

    public String getTargetPlayerUUID() {
        return targetPlayerUUID;
    }

    public void setTargetPlayerUUID(String targetPlayerUUID) {
        this.targetPlayerUUID = targetPlayerUUID;
    }

    /**
     * Not used — Bukkit requires this method but we never need to retrieve
     * the inventory from the holder.
     */
    @Override
    public @NotNull Inventory getInventory() {
        // This is intentionally not implemented as we never retrieve
        // the inventory from the holder side.
        throw new UnsupportedOperationException("AuctifyHolder does not track its inventory.");
    }
}
