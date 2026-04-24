package dev.auctify.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Custom event fired when a new auction listing is about to be created. Other
 * plugins can listen and cancel to prevent the listing.
 */
public class AuctifyListingCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionListing listing;
    private final Player seller;
    private boolean cancelled;

    /** Constructor. */
    public AuctifyListingCreateEvent(AuctionListing listing, Player seller) {
        this.listing = listing;
        this.seller = seller;
        this.cancelled = false;
    }

    public AuctionListing getListing() {
        return listing;
    }

    public Player getSeller() {
        return seller;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handler list */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
