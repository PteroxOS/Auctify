package dev.auctify.auction;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Custom event fired when a bid is placed on an auction listing.
 * Other plugins can listen and cancel to prevent the bid.
 */
public class AuctifyBidEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionListing listing;
    private final Player bidder;
    private final double amount;
    private boolean cancelled;

    /**
     * @param listing the listing being bid on
     * @param bidder  the player placing the bid
     * @param amount  the bid amount
     */
    public AuctifyBidEvent(AuctionListing listing, Player bidder, double amount) {
        this.listing = listing;
        this.bidder = bidder;
        this.amount = amount;
        this.cancelled = false;
    }

    /** @return the listing */
    public AuctionListing getListing() { return listing; }

    /** @return the bidder */
    public Player getBidder() { return bidder; }

    /** @return the bid amount */
    public double getAmount() { return amount; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list */
    public static HandlerList getHandlerList() { return HANDLERS; }
}
