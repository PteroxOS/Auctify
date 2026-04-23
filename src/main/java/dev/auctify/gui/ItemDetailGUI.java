package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.auction.BidRecord;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.MessageUtil;
import dev.auctify.util.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Detailed item view GUI opened on right-click of a listing.
 * Shows full item metadata, bid history, and a back button.
 * All display text is loaded from the locale file via MessageUtil.
 */
public class ItemDetailGUI {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public ItemDetailGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the detail view for a specific listing.
     *
     * @param player  the viewing player
     * @param listing the listing to inspect
     */
    public void open(Player player, AuctionListing listing) {
        AuctifyHolder holder = new AuctifyHolder("DETAIL");
        holder.setListingId(listing.getId());
        Inventory inv = Bukkit.createInventory(holder, 27,
                ColorUtil.toComponent(MessageUtil.get("gui-detail-title")));

        var config = plugin.getConfig();

        // Center slot: the actual item (slot 13)
        inv.setItem(13, listing.getItem());

        // Seller info panel (slot 10)
        inv.setItem(10, buildItem(Material.PLAYER_HEAD,
                MessageUtil.get("gui-detail-seller-title"),
                MessageUtil.get("gui-detail-seller", Map.of("seller", listing.getSellerName())),
                MessageUtil.get("gui-detail-start-price", Map.of("price", plugin.getEconomyManager().format(listing.getStartPrice()))),
                MessageUtil.get("gui-detail-listing-id", Map.of("id", listing.getId())),
                "",
                MessageUtil.get("gui-detail-time-left", Map.of("time", TimeUtil.formatSeconds(listing.getTimeRemainingSeconds(), config)))));

        // Bid history panel (slot 16)
        List<String> historyLore = new ArrayList<>();
        List<BidRecord> bids = listing.getBidHistory();

        if (bids.isEmpty()) {
            historyLore.add(MessageUtil.get("gui-detail-no-bids"));
        } else {
            int start = Math.max(0, bids.size() - 5);
            for (int i = bids.size() - 1; i >= start; i--) {
                BidRecord bid = bids.get(i);
                historyLore.add("§e" + bid.bidderName() + " §7— §a" + plugin.getEconomyManager().format(bid.amount()));
            }
            if (bids.size() > 5) {
                historyLore.add(MessageUtil.get("gui-detail-more-bids", Map.of("count", String.valueOf(bids.size() - 5))));
            }
        }
        inv.setItem(16, buildItem(Material.WRITABLE_BOOK,
                MessageUtil.get("gui-detail-bid-history"),
                historyLore.toArray(new String[0])));

        // Buyout info (slot 4)
        if (listing.getBuyoutPrice() > 0) {
            inv.setItem(4, buildItem(Material.GOLD_INGOT,
                    MessageUtil.get("gui-detail-buyout-title"),
                    MessageUtil.get("gui-detail-buyout-price", Map.of("price", plugin.getEconomyManager().format(listing.getBuyoutPrice())))));
        }

        // Back button (slot 22)
        inv.setItem(22, buildItem(Material.BARRIER, MessageUtil.get("gui-detail-back")));

        // Fill remaining
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }

        plugin.getGUIManager().markOpen(player, "DETAIL");
        plugin.getGUIManager().setViewingListing(player, listing.getId());
        player.openInventory(inv);
    }

    /**
     * Builds an ItemStack with Adventure Component display name and lore.
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
