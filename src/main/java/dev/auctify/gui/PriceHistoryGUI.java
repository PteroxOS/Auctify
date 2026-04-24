package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.PriceHistory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * GUI for displaying price history trends of auction items.
 */
public class PriceHistoryGUI {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, HH:mm");
    private static final int GUI_SIZE = 54;

    private final Auctify plugin;
    private final String itemType;
    private final int page;

    public PriceHistoryGUI(Auctify plugin, String itemType, int page) {
        this.plugin = plugin;
        this.itemType = itemType;
        this.page = page;
    }

    /**
     * Opens the price history GUI for the player.
     *
     * @param player the player to show the GUI to
     */
    public void open(Player player) {
        AuctifyHolder holder = new AuctifyHolder("PRICE_HISTORY");
        holder.setPage(page);

        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE,
                ChatColor.DARK_GRAY + "Price History: " + itemType);

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

        meta.setDisplayName(ChatColor.GOLD + "Price Statistics");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Total Sales: " + ChatColor.WHITE + count);
        lore.add(ChatColor.GRAY + "Average: " + ChatColor.GREEN
                + plugin.getAuctionManager().getEconomy().format(avgPrice));
        lore.add(ChatColor.GRAY + "Minimum: " + ChatColor.RED
                + plugin.getAuctionManager().getEconomy().format(minPrice));
        lore.add(ChatColor.GRAY + "Maximum: " + ChatColor.AQUA
                + plugin.getAuctionManager().getEconomy().format(maxPrice));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Item: " + ChatColor.WHITE + itemType);

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates an item displaying a single price history entry.
     */
    private ItemStack createHistoryItem(PriceHistory entry) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.WHITE + entry.getItemName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.GREEN
                + plugin.getAuctionManager().getEconomy().format(entry.getFinalPrice()));
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.YELLOW + entry.getSellerName());
        lore.add(ChatColor.GRAY + "Winner: " + ChatColor.YELLOW + entry.getWinnerName());
        lore.add(ChatColor.GRAY + "Date: " + ChatColor.WHITE + DATE_FORMAT.format(new Date(entry.getTimestamp())));
        lore.add(ChatColor.GRAY + "ID: " + ChatColor.DARK_GRAY + entry.getId());

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates a navigation button.
     */
    private ItemStack createNavigationButton(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.YELLOW + name);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates a close button.
     */
    private static ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.RED + "Close");
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

        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE,
                ChatColor.DARK_GRAY + "Recent Price History");

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

        meta.setDisplayName(ChatColor.WHITE + entry.getItemName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.GREEN
                + plugin.getAuctionManager().getEconomy().format(entry.getFinalPrice()));
        lore.add(ChatColor.GRAY + "Material: " + ChatColor.YELLOW + entry.getItemMaterial());
        lore.add(ChatColor.GRAY + "Date: " + ChatColor.WHITE + DATE_FORMAT.format(new Date(entry.getTimestamp())));

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }
}
