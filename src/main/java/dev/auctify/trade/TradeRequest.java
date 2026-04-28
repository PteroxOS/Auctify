package dev.auctify.trade;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a direct trade request between two players.
 */
public class TradeRequest {

    private final String id;
    private final UUID senderUUID;
    private final String senderName;
    private final UUID targetUUID;
    private final String targetName;
    private final ItemStack senderItem;
    private final ItemStack targetItem;
    private final double senderMoney;
    private final double targetMoney;
    private final long timestamp;
    private boolean accepted;
    private boolean cancelled;

    public TradeRequest(String id, UUID senderUUID, String senderName, UUID targetUUID, String targetName,
            ItemStack senderItem, ItemStack targetItem, double senderMoney, double targetMoney) {
        this.id = id;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.senderItem = senderItem;
        this.targetItem = targetItem;
        this.senderMoney = senderMoney;
        this.targetMoney = targetMoney;
        this.timestamp = System.currentTimeMillis();
        this.accepted = false;
        this.cancelled = false;
    }

    public String getId() {
        return id;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getTargetName() {
        return targetName;
    }

    public ItemStack getSenderItem() {
        return senderItem;
    }

    public ItemStack getTargetItem() {
        return targetItem;
    }

    public double getSenderMoney() {
        return senderMoney;
    }

    public double getTargetMoney() {
        return targetMoney;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - timestamp > timeoutMillis;
    }
}
