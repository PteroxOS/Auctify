package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.PriceHistory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** GUI for displaying price history trends of auction items. */
public class PriceHistoryGUI {

    // FIX-5: SimpleDateFormat is not thread-safe — use ThreadLocal
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("MMM dd, HH:mm"));
    private static final int GUI_SIZE = 54;

    private final Auctify plugin;
    private final String itemType;
    private final int page;

    public PriceHistoryGUI(Auctify plugin, String itemType, int page) {
        this.plugin = plugin;
        this.itemType = itemType;
        this.page = page;
    }

    /** Opens the price history GUI for the player. */
    public void open(Player player) {
        AuctifyHolder holder = new AuctifyHolder("PRICE_HISTORY");
        holder.setPage(page);

        // FIX H-1: Gunakan Adventure API Component daripada ChatColor deprecated
        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE,
                Component.text("Price History: " + itemType).color(NamedTextColor.DARK_GRAY));

        // Get price history data
        List<PriceHistory> history = plugin.getStorageManager().getPriceHistory(itemType, 45);

        // Calculate statistics
        if (!history.isEmpty()) {
            double avgPrice = history.stream().mapToDouble(PriceHistory::getFinalPrice).average().orElse(0);
            double minPrice = history.stream().mapToDouble(PriceHistory::getFinalPrice).min().orElse(0);
            double maxPrice = history.stream().mapToDouble(PriceHistory::getFinalPrice).max().orElse(0);

            // Add statistics display
            ItemStack statsItem = createStatsItem(avgPrice, minPrice, maxPrice, history.size());
            inventory.setItem(4, statsItem);
        }

        // Add price history entries
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, history.size());

        for (int i = startIndex; i < endIndex; i++) {
            PriceHistory entry = history.get(i);
            ItemStack item = createHistoryItem(entry);
            inventory.setItem(i - startIndex + 9, item); // Start after the stats row
        }

        // Add navigation buttons
        if (page > 0) {
            ItemStack prevButton = createNavigationButton("Previous Page", Material.ARROW);
            inventory.setItem(45, prevButton);
        }

        if ((page + 1) * 45 < history.size()) {
            ItemStack nextButton = createNavigationButton("Next Page", Material.ARROW);
            inventory.setItem(53, nextButton);
        }

        // Add close button
        ItemStack closeButton = createCloseButton();
        inventory.setItem(49, closeButton);

        player.openInventory(inventory);
    }

    /**
     * Creates a statistics display item showing price data.
     */
    private ItemStack createStatsItem(double avgPrice, double minPrice, double maxPrice, int count) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        // FIX H-1: Gunakan Adventure API Component daripada ChatColor deprecated
        meta.displayName(Component.text("Price Statistics").color(NamedTextColor.GOLD));

        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text("Total Sales: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(count)).color(NamedTextColor.WHITE)));
        lore.add(Component.text("Average: ").color(NamedTextColor.GRAY)
                .append(Component.text(plugin.getAuctionManager().getEconomy().format(avgPrice))
                        .color(NamedTextColor.GREEN)));
        lore.add(Component.text("Minimum: ").color(NamedTextColor.GRAY)
                .append(Component.text(plugin.getAuctionManager().getEconomy().format(minPrice))
                        .color(NamedTextColor.RED)));
        lore.add(Component.text("Maximum: ").color(NamedTextColor.GRAY)
                .append(Component.text(plugin.getAuctionManager().getEconomy().format(maxPrice))
                        .color(NamedTextColor.AQUA)));
        lore.add(Component.empty());
        lore.add(Component.text("Item: ").color(NamedTextColor.YELLOW)
                .append(Component.text(itemType).color(NamedTextColor.WHITE)));

        // FIX H-1: Gunakan lore(List<Component>) daripada setLore(List<String>) yang
        // deprecated
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates an item displaying a single price history entry.
     */
    private ItemStack createHistoryItem(PriceHistory entry) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        // FIX H-1: Gunakan Adventure API Component daripada ChatColor deprecated
        meta.displayName(Component.text(entry.getItemName()).color(NamedTextColor.WHITE));

        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text("Price: ").color(NamedTextColor.GRAY)
                .append(Component.text(plugin.getAuctionManager().getEconomy().format(entry.getFinalPrice()))
                        .color(NamedTextColor.GREEN)));
        lore.add(Component.text("Seller: ").color(NamedTextColor.GRAY)
                .append(Component.text(entry.getSellerName()).color(NamedTextColor.YELLOW)));
        lore.add(Component.text("Winner: ").color(NamedTextColor.GRAY)
                .append(Component.text(entry.getWinnerName()).color(NamedTextColor.YELLOW)));
        // FIX-5: Use ThreadLocal.get() to access SimpleDateFormat
        lore.add(Component.text("Date: ").color(NamedTextColor.GRAY)
                .append(Component.text(DATE_FORMAT.get().format(new Date(entry.getTimestamp())))
                        .color(NamedTextColor.WHITE)));
        lore.add(Component.text("ID: ").color(NamedTextColor.GRAY)
                .append(Component.text(entry.getId()).color(NamedTextColor.DARK_GRAY)));

        // FIX H-1: Gunakan lore(List<Component>) daripada setLore(List<String>) yang
        // deprecated
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates a navigation button.
     */
    private ItemStack createNavigationButton(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // FIX H-1: Gunakan Adventure API Component daripada ChatColor deprecated
        meta.displayName(Component.text(name).color(NamedTextColor.YELLOW));
        meta.lore(new ArrayList<>());
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates a close button.
     */
    private static ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        // FIX H-1: Gunakan Adventure API Component daripada ChatColor deprecated
        meta.displayName(Component.text("Close").color(NamedTextColor.RED));
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Opens the price history GUI for a specific item type.
     *
     * @param plugin   the plugin instance
     * @param player   the player to show the GUI to
     * @param itemType the item material type to show history for
     */
    public static void openPriceHistory(Auctify plugin, Player player, String itemType) {
        new PriceHistoryGUI(plugin, itemType, 0).open(player);
    }

    /**
     * Opens the price history GUI showing all recent sales.
     *
     * @param plugin the plugin instance
     * @param player the player to show the GUI to
     */
    public static void openAllHistory(Auctify plugin, Player player) {
        AuctifyHolder holder = new AuctifyHolder("PRICE_HISTORY_ALL");

        // FIX H-1: Gunakan Adventure API Component daripada ChatColor deprecated
        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE,
                Component.text("Recent Price History").color(NamedTextColor.DARK_GRAY));

        // Get all recent price history
        List<PriceHistory> history = plugin.getStorageManager().getAllPriceHistory(45);

        // Add entries
        for (int i = 0; i < Math.min(history.size(), 45); i++) {
            PriceHistory entry = history.get(i);
            ItemStack item = createAllHistoryItem(entry, plugin);
            inventory.setItem(i, item);
        }

        // Add close button
        ItemStack closeButton = createCloseButton();
        inventory.setItem(49, closeButton);

        player.openInventory(inventory);
    }

    /**
     * Creates an item for the all-history view.
     */
    private static ItemStack createAllHistoryItem(PriceHistory entry, Auctify plugin) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        // FIX H-1: Gunakan Adventure API Component daripada ChatColor deprecated
        meta.displayName(Component.text(entry.getItemName()).color(NamedTextColor.WHITE));

        java.util.List<Component> lore = new java.util.ArrayList<>();
        lore.add(Component.text("Price: ").color(NamedTextColor.GRAY)
                .append(Component.text(plugin.getAuctionManager().getEconomy().format(entry.getFinalPrice()))
                        .color(NamedTextColor.GREEN)));
        lore.add(Component.text("Material: ").color(NamedTextColor.GRAY)
                .append(Component.text(entry.getItemMaterial()).color(NamedTextColor.YELLOW)));
        // FIX-5: Use ThreadLocal.get() to access SimpleDateFormat
        lore.add(Component.text("Date: ").color(NamedTextColor.GRAY)
                .append(Component.text(DATE_FORMAT.get().format(new Date(entry.getTimestamp())))
                        .color(NamedTextColor.WHITE)));

        // FIX H-1: Gunakan lore(List<Component>) daripada setLore(List<String>) yang
        // deprecated
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }
}
