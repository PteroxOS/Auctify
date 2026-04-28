package dev.auctify.auction;

import org.bukkit.Material;

/**
 * Represents a saved listing template with common settings.
 */
public class ListingTemplate {

    private final String id;
    private final String name;
    private final double startPrice;
    private final double buyoutPrice;
    private final int duration;
    private final Material itemType;

    public ListingTemplate(String id, String name, double startPrice, double buyoutPrice, int duration, Material itemType) {
        this.id = id;
        this.name = name;
        this.startPrice = startPrice;
        this.buyoutPrice = buyoutPrice;
        this.duration = duration;
        this.itemType = itemType;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getBuyoutPrice() {
        return buyoutPrice;
    }

    public int getDuration() {
        return duration;
    }

    public Material getItemType() {
        return itemType;
    }
}
