package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.MessageUtil;
import dev.auctify.util.TimeUtil;
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
 * Management GUI for a seller's own listing. Allows them to view stats and
 * cancel the listing. All display text is loaded from the locale file via
 * MessageUtil.
 */
public class ManageListingGUI {

    private final Auctify plugin;

    public ManageListingGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, AuctionListing listing) {
        AuctifyHolder holder = new AuctifyHolder("MANAGE");
        holder.setListingId(listing.getId());
        Inventory inv = Bukkit.createInventory(holder, 27,
                ColorUtil.toComponent(MessageUtil.get("gui-manage-title")));

        var config = plugin.getConfig();

        // Slot 4: The Item
        inv.setItem(4, listing.buildDisplayItem(config));

        // Slot 11: Info
        String topBidder = listing.hasBids() ? listing.getTopBidderName() : MessageUtil.get("gui-bid-no-bids");
        String buyoutDisplay = listing.getBuyoutPrice() > 0
                ? plugin.getEconomyManager().format(listing.getBuyoutPrice())
                : MessageUtil.get("gui-manage-no-buyout");
        inv.setItem(11, buildItem(Material.PAPER,
                MessageUtil.get("gui-manage-stats-title"),
                MessageUtil.get("gui-manage-current-bid",
                        Map.of("bid", plugin.getEconomyManager().format(listing.getCurrentBid()))),
                MessageUtil.get("gui-manage-top-bidder", Map.of("bidder", topBidder)),
                MessageUtil.get("gui-manage-buyout-price", Map.of("price", buyoutDisplay)),
                "",
                MessageUtil.get("gui-manage-time-left", Map.of("time",
                        TimeUtil.formatSeconds((listing.getEndTime() - System.currentTimeMillis()) / 1000, config)))));

        // Slot 15: Cancel Listing
        inv.setItem(15, buildItem(Material.RED_WOOL,
                MessageUtil.get("gui-manage-cancel-title"),
                MessageUtil.get("gui-manage-cancel-lore-1"),
                MessageUtil.get("gui-manage-cancel-lore-2"),
                MessageUtil.get("gui-manage-cancel-lore-3")));

        // Slot 22: Back Button
        inv.setItem(22, buildItem(Material.ARROW, MessageUtil.get("gui-manage-back")));

        // Filler
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        plugin.getGUIManager().markOpen(player, "MANAGE");
        plugin.getGUIManager().setViewingListing(player, listing.getId());
        player.openInventory(inv);
    }

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
