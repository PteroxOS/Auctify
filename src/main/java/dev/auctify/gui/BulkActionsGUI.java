package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Stream;

/**
 * GUI for performing bulk admin actions on listings.
 */
public class BulkActionsGUI {

    private final Auctify plugin;

    public BulkActionsGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Opens the bulk actions GUI. */
    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new BulkHolder(), 45,
                Component.text("Bulk Admin Actions").color(NamedTextColor.GOLD));

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        // Bulk cancel all listings
        inv.setItem(11, createItem(Material.BARRIER, "§c§lBulk Cancel All",
                "§7Cancel all active listings",
                "§7§cWARNING: This cannot be undone!",
                "",
                "§7Click to confirm"));

        // Bulk cancel expired listings
        inv.setItem(13, createItem(Material.CLOCK, "§e§lCancel Expired",
                "§7Cancel all expired listings",
                "§7Safe operation - only affects expired auctions",
                "",
                "§7Click to execute"));

        // Bulk extend all listings
        inv.setItem(15, createItem(Material.REPEATER, "§a§lBulk Extend",
                "§7Extend all listings by 1 hour",
                "§7Useful for server maintenance",
                "",
                "§7Click to execute"));

        // Bulk cancel by player
        inv.setItem(20, createItem(Material.PLAYER_HEAD, "§e§lCancel by Player",
                "§7Cancel all listings for a specific player",
                "§7Enter player name in chat",
                "",
                "§7Click to select player"));

        // Bulk cancel by category
        inv.setItem(22, createItem(Material.CHEST, "§e§lCancel by Category",
                "§7Cancel all listings in a category",
                "§7Select from available categories",
                "",
                "§7Click to select category"));

        // Bulk cancel under threshold
        inv.setItem(24, createItem(Material.GOLD_INGOT, "§e§lCancel Under Threshold",
                "§7Cancel listings below a price threshold",
                "§7Enter threshold amount",
                "",
                "§7Click to set threshold"));

        // Statistics
        int totalListings = plugin.getAuctionManager().getActiveListings().size();
        int expiredCount = (int) plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.isExpired())
                .count();

        inv.setItem(31, createItem(Material.BOOK, "§e§lStatistics",
                "§7Total Active Listings: §f" + totalListings,
                "§7Expired Listings: §c" + expiredCount,
                "§7Active Listings: §a" + (totalListings - expiredCount)));

        // Back button
        inv.setItem(40, createItem(Material.ARROW, "§cBack to Admin Panel"));

        player.openInventory(inv);
    }

    /** Executes bulk cancel all listings. */
    public void bulkCancelAll(Player player) {
        int cancelled = plugin.getAuctionManager().bulkCancelAdmin(player);
        player.sendMessage("§aCancelled §f" + cancelled + " §alistings.");
    }

    /** Executes bulk cancel expired listings. */
    public void bulkCancelExpired(Player player) {
        int cancelled = plugin.getAuctionManager().bulkCancelExpired(player);
        player.sendMessage("§aCancelled §f" + cancelled + " §aexpired listings.");
    }

    /** Executes bulk extend all listings. */
    public void bulkExtendAll(Player player) {
        int extended = plugin.getAuctionManager().bulkExtendAll(player, 3600); // 1 hour
        player.sendMessage("§aExtended §f" + extended + " §alistings by 1 hour.");
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.toComponent(name));
            if (lore.length > 0) {
                meta.lore(Stream.of(lore).map(ColorUtil::toComponent).collect(java.util.stream.Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static class BulkHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
