package dev.auctify.auction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a buy order where a player requests to buy an item at a specific
 * price. Similar to auction but inverse — buyer creates the order and sellers
 * can fill it.
 */
public class BuyOrder {

    private final String id;
    private final UUID buyerUUID;
    private final String buyerName;
    private final Material itemType;
    private final int amount;
    private final double pricePerUnit;
    private final long createdAt;
    private final long expiryTime;
    private volatile boolean active;

    /** Creates a new buy order. */
    public BuyOrder(String id, UUID buyerUUID, String buyerName, Material itemType,
            int amount, double pricePerUnit, long createdAt, long expiryTime) {
        this.id = id;
        this.buyerUUID = buyerUUID;
        this.buyerName = buyerName;
        this.itemType = itemType;
        this.amount = amount;
        this.pricePerUnit = pricePerUnit;
        this.createdAt = createdAt;
        this.expiryTime = expiryTime;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public UUID getBuyerUUID() {
        return buyerUUID;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public Material getItemType() {
        return itemType;
    }

    public int getAmount() {
        return amount;
    }

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public double getTotalPrice() {
        return pricePerUnit * amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    /**
     * Checks if an item stack matches this buy order's requirements.
     */
    public boolean matchesItem(ItemStack item) {
        return item != null && item.getType() == itemType && item.getAmount() >= amount;
    }

    @Override
    public String toString() {
        return "BuyOrder{" + id + " buyer=" + buyerName + " item=" + itemType + " amount=" + amount + " price="
                + pricePerUnit + "}";
    }
}
