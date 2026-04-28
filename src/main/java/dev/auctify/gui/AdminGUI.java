package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Admin moderation GUI for managing the auction house. Allows admins to view
 * all listings, force cancel, and manage blacklist.
 */
public class AdminGUI {

    private final Auctify plugin;

    public AdminGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Opens the admin panel GUI. */
    public void open(Player player, int page) {
        AuctifyHolder holder = new AuctifyHolder("ADMIN");
        holder.setPage(page);
        Inventory inv = Bukkit.createInventory(holder, 54,
                ColorUtil.toComponent(MessageUtil.get("gui-admin-title")));

        var config = plugin.getConfig();

        // Get ALL active listings (admin sees everything)
        List<AuctionListing> listings = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.isActive())
                .sorted((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()))
                .collect(Collectors.toList());

        int itemsPerPage = 45; // 5 rows of items
        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / itemsPerPage));
        if (page >= totalPages)
            page = totalPages - 1;
        if (page < 0)
            page = 0;

        // Fill listing slots
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, listings.size());
        for (int i = startIndex; i < endIndex; i++) {
            AuctionListing listing = listings.get(i);
            ItemStack display = listing.buildDisplayItem(config);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null)
                    lore = new java.util.ArrayList<>();
                lore.add(ColorUtil.toComponent(""));
                lore.add(ColorUtil.toComponent(MessageUtil.get("gui-admin-click-cancel")));
                lore.add(ColorUtil.toComponent("§8ID: " + listing.getId()));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(i - startIndex, display);
        }

        // Fill empty slots
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = endIndex - startIndex; i < 45; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        // Bottom nav row
        for (int i = 45; i < 54; i++)
            inv.setItem(i, filler);

        // Nav buttons
        if (page > 0) {
            inv.setItem(45, buildItem(Material.ARROW, MessageUtil.get("gui-previous-page")));
        }

        // Stats (slot 49)
        int totalListings = listings.size();
        int blacklistCount = plugin.getStorageManager().getBlacklist().size();
        inv.setItem(49, buildItem(Material.BOOK,
                MessageUtil.get("gui-admin-stats"),
                MessageUtil.get("gui-admin-total-listings", Map.of("count", String.valueOf(totalListings))),
                MessageUtil.get("gui-admin-blacklist-count", Map.of("count", String.valueOf(blacklistCount))),
                MessageUtil.get("gui-admin-page",
                        Map.of("current", String.valueOf(page + 1), "total", String.valueOf(totalPages)))));

        // Blacklist management (slot 47)
        inv.setItem(47, buildItem(Material.BARRIER,
                MessageUtil.get("gui-admin-blacklist-title"),
                MessageUtil.get("gui-admin-blacklist-lore")));

        // Player history viewer (slot 48)
        inv.setItem(48, buildItem(Material.PLAYER_HEAD,
                "§e§lPlayer History",
                "§7View auction history for a player",
                "§7Click to search player"));

        // Audit log (slot 50)
        inv.setItem(50, buildItem(Material.WRITABLE_BOOK,
                "§e§lAudit Log",
                "§7View economy transaction logs",
                "§7Click to open audit log"));

        // Bulk actions (slot 51)
        inv.setItem(51, buildItem(Material.CHEST,
                "§e§lBulk Actions",
                "§7Perform bulk admin operations",
                "§7Bulk cancel, extend, etc."));

        // Back to main GUI (slot 53)
        if (page < totalPages - 1) {
            inv.setItem(53, buildItem(Material.ARROW, MessageUtil.get("gui-next-page")));
        }

        plugin.getGUIManager().markOpen(player, "ADMIN");
        player.openInventory(inv);
    }

    private ItemStack buildItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.toComponent(name));
            if (lore.length > 0) {
                meta.lore(Stream.of(lore).map(ColorUtil::toComponent).collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
