package dev.auctify.auction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a historical record of a completed auction sale.
 * Used for price tracking and trend analysis.
 */
public class PriceHistory {

    private final String id;
    private final String itemMaterial;
    private final String itemName;
    private final double finalPrice;
    private final String sellerName;
    private final String winnerName;
    private final long timestamp;

    public PriceHistory(String id, String itemMaterial, String itemName, double finalPrice,
                       String sellerName, String winnerName, long timestamp) {
        this.id = id;
        this.itemMaterial = itemMaterial;
        this.itemName = itemName;
        this.finalPrice = finalPrice;
        this.sellerName = sellerName;
        this.winnerName = winnerName;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getItemMaterial() {
        return itemMaterial;
    }

    public String getItemName() {
        return itemName;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
