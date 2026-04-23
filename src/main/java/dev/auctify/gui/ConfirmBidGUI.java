package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Confirmation GUI shown before a player places a bid.
 * 3-row inventory with the item in the center, confirm on the left, cancel on the right.
 * All display text is loaded from the locale file via MessageUtil.
 */
public class ConfirmBidGUI {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public ConfirmBidGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the bid confirmation GUI for a specific listing.
     *
     * @param player  the player viewing the confirmation
     * @param listing the listing they want to bid on
     */
    public void open(Player player, AuctionListing listing) {
        AuctifyHolder holder = new AuctifyHolder("CONFIRM");
        holder.setListingId(listing.getId());
        Inventory inv = Bukkit.createInventory(holder, 27,
                ColorUtil.toComponent(MessageUtil.get("gui-confirm-title")));

        var config = plugin.getConfig();
        double minIncrement = config.getDouble("bidding.min-increment", 10);
        double minBid = listing.hasBids() ? listing.getCurrentBid() + minIncrement : listing.getStartPrice();
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());

        // Row 1: Display the item in center (slot 4)
        inv.setItem(4, listing.buildDisplayItem(config));

        // Row 2: Confirm button (slot 11)
        inv.setItem(11, buildItem(Material.LIME_WOOL,
                MessageUtil.get("gui-confirm-bid"),
                MessageUtil.get("gui-confirm-min-bid", Map.of("min_bid", plugin.getEconomyManager().format(minBid))),
                MessageUtil.get("gui-confirm-balance", Map.of("balance", plugin.getEconomyManager().format(balance))),
                "",
                MessageUtil.get("gui-confirm-click")));

        // Info item (slot 13)
        String topBidder = listing.hasBids() ? listing.getTopBidderName() : MessageUtil.get("gui-bid-no-bids");
        String buyoutLine = listing.getBuyoutPrice() > 0
                ? MessageUtil.get("gui-bid-buyout-line", Map.of("buyout", plugin.getEconomyManager().format(listing.getBuyoutPrice())))
                : MessageUtil.get("gui-bid-no-buyout");
        inv.setItem(13, buildItem(Material.PAPER,
                MessageUtil.get("gui-bid-info-title"),
                MessageUtil.get("gui-bid-current", Map.of("current_bid", plugin.getEconomyManager().format(listing.getCurrentBid()))),
                MessageUtil.get("gui-bid-top-bidder", Map.of("bidder", topBidder)),
                MessageUtil.get("gui-bid-min-increment", Map.of("increment", plugin.getEconomyManager().format(minIncrement))),
                "",
                buyoutLine));

        // Cancel button (slot 15)
        inv.setItem(15, buildItem(Material.RED_WOOL,
                MessageUtil.get("gui-cancel-button"),
                MessageUtil.get("gui-cancel-lore")));

        // Buyout button (slot 22)
        if (listing.getBuyoutPrice() > 0 && config.getBoolean("bidding.allow-buyout", true)) {
            inv.setItem(22, buildItem(Material.GOLD_INGOT,
                    MessageUtil.get("gui-buyout-title"),
                    MessageUtil.get("gui-buyout-price", Map.of("price", plugin.getEconomyManager().format(listing.getBuyoutPrice()))),
                    "",
                    MessageUtil.get("gui-buyout-click")));
        }

        // Fill remaining slots with filler
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        plugin.getGUIManager().markOpen(player, "CONFIRM");
        plugin.getGUIManager().setViewingListing(player, listing.getId());
        player.openInventory(inv);
    }

    /**
     * Builds an ItemStack with an Adventure Component display name and lore.
     */
    private ItemStack buildItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.toComponent(name));
            if (lore.length > 0) {
                meta.lore(Stream.of(lore)
                        .map(ColorUtil::toComponent)
                        .collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
