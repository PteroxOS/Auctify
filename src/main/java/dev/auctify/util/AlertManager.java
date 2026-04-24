package dev.auctify.util;

import dev.auctify.Auctify;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Manages auction alerts via Title and Action Bar for important events.
 */
public class AlertManager {

    private final Auctify plugin;
    private final boolean titlesEnabled;
    private final boolean actionBarEnabled;

    public AlertManager(Auctify plugin) {
        this.plugin = plugin;
        this.titlesEnabled = plugin.getConfig().getBoolean("alerts.titles.enabled", true);
        this.actionBarEnabled = plugin.getConfig().getBoolean("alerts.actionbar.enabled", true);
    }

    /**
     * Sends a title and subtitle to a player.
     */
    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (!titlesEnabled || player == null || !player.isOnline()) return;

        try {
            Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50),
                Duration.ofMillis(stay * 50),
                Duration.ofMillis(fadeOut * 50)
            );

            Component titleComponent = MiniMessage.miniMessage().deserialize(title);
            Component subtitleComponent = MiniMessage.miniMessage().deserialize(subtitle);

            Title t = Title.title(titleComponent, subtitleComponent, times);
            player.showTitle(t);
        } catch (Exception e) {
            // Fallback for older versions
            player.sendTitle(title.replace("<", "").replace(">", ""), subtitle.replace("<", "").replace(">", ""), fadeIn, stay, fadeOut);
        }
    }

    /**
     * Sends an action bar message to a player.
     */
    private void sendActionBar(Player player, String message) {
        if (!actionBarEnabled || player == null || !player.isOnline()) return;

        try {
            Component msg = MiniMessage.miniMessage().deserialize(message);
            player.sendActionBar(msg);
        } catch (Exception e) {
            // Ignore errors for older versions
        }
    }

    // Alert: Player was outbid
    public void alertOutbid(Player player, String itemName, double newBid, String newBidder) {
        sendTitle(player,
            "<red>⚠ Outbid!",
            "<yellow>" + itemName + " <gray>bid: <green>$" + String.format("%.2f", newBid),
            10, 50, 20);

        sendActionBar(player, "<red>⚠ You were outbid on " + itemName + " by " + newBidder);

        // Also play sound
        plugin.getSoundManager().playOutbid(player);
    }

    // Alert: Player won auction
    public void alertAuctionWon(Player player, String itemName, double finalPrice) {
        sendTitle(player,
            "<green>🎉 Auction Won!",
            "<yellow>" + itemName + " <gray>for <green>$" + String.format("%.2f", finalPrice),
            10, 70, 30);

        sendActionBar(player, "<green>✓ Check your mailbox with /ac claim");
    }

    // Alert: Player's auction was sold
    public void alertAuctionSold(Player player, String itemName, String buyer, double price) {
        sendTitle(player,
            "<green>💰 Item Sold!",
            "<yellow>" + itemName + " <gray>sold to <aqua>" + buyer + " <gray>for <green>$" + String.format("%.2f", price),
            10, 60, 20);

        sendActionBar(player, "<green>+$" + String.format("%.2f", price) + " <gray>Check /ac claim");
    }

    // Alert: Someone bid on player's watched auction
    public void alertNewBidOnWatchlist(Player player, String itemName, double bidAmount, String bidder) {
        sendActionBar(player, "<yellow>🔔 " + bidder + " bid <green>$" + String.format("%.2f", bidAmount) + " <yellow>on " + itemName);
    }

    // Alert: Auction ending soon (last 60 seconds)
    public void alertAuctionEnding(Player player, String itemName, int secondsLeft) {
        if (secondsLeft <= 10) {
            sendActionBar(player, "<red>⏰ " + itemName + " ends in " + secondsLeft + "s!");
        } else {
            sendActionBar(player, "<yellow>⏰ " + itemName + " ending soon...");
        }
    }

    // Broadcast alert: Rare/High value item listed
    public void broadcastHighValueListing(String seller, String itemName, double price) {
        if (!plugin.getConfig().getBoolean("alerts.broadcast-high-value", true)) return;

        double threshold = plugin.getConfig().getDouble("alerts.high-value-threshold", 10000);
        if (price < threshold) return;

        Component message = MiniMessage.miniMessage().deserialize(
            "<gold>⚡ <aqua>" + seller + " <gray>listed <gold>" + itemName +
            " <gray>for <green>$" + String.format("%.2f", price) + "<gray>! <yellow>/ac open"
        );

        Bukkit.broadcast(message, "auctify.use");
    }
}
