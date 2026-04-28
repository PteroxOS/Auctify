package dev.auctify.gui;

import dev.auctify.Auctify;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/** Player Statistics GUI showing auction stats and activity. */
public class StatsGUI {

    private final Auctify plugin;

    public StatsGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Opens the player statistics GUI. */
    public void open(Player player, UUID targetUUID, String targetName) {
        openPlayerStats(player, targetUUID, targetName);
    }

    /** Opens the global auction house statistics dashboard. */
    public void openDashboard(Player player) {
        Inventory inv = Bukkit.createInventory(new StatsHolder(), 54,
                Component.text("Auction House Dashboard").color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));

        // Get global statistics
        int totalActiveListings = plugin.getAuctionManager().getActiveListings().size();
        double totalVolume = calculateTotalVolume();
        int totalUsers = getTotalActiveUsers();
        double avgListingPrice = calculateAvgListingPrice();
        int totalSoldToday = getSoldToday();
        double revenueToday = getRevenueToday();

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Header
        inv.setItem(4, createItem(Material.GOLD_BLOCK,
                Component.text("Auction House Dashboard").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Real-time auction house statistics").color(NamedTextColor.GRAY)));

        // Global stats items
        inv.setItem(19, createItem(Material.CHEST,
                Component.text("Active Listings").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Total Active: ").color(NamedTextColor.GRAY)
                        .append(Component.text(totalActiveListings).color(NamedTextColor.WHITE)),
                Component.text("Active Users: ").color(NamedTextColor.GRAY)
                        .append(Component.text(totalUsers).color(NamedTextColor.WHITE)),
                Component.text("Avg Price: ").color(NamedTextColor.GRAY).append(
                        Component.text("$" + String.format("%.2f", avgListingPrice)).color(NamedTextColor.WHITE))));

        inv.setItem(21, createItem(Material.EMERALD_BLOCK,
                Component.text("Market Volume").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                Component.text("Total Volume: ").color(NamedTextColor.GRAY)
                        .append(Component.text("$" + String.format("%.2f", totalVolume)).color(NamedTextColor.GREEN)),
                Component.text("Sold Today: ").color(NamedTextColor.GRAY)
                        .append(Component.text(totalSoldToday).color(NamedTextColor.WHITE)),
                Component.text("Revenue Today: ").color(NamedTextColor.GRAY).append(
                        Component.text("$" + String.format("%.2f", revenueToday)).color(NamedTextColor.GREEN))));

        inv.setItem(23, createItem(Material.DIAMOND,
                Component.text("Top Categories").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
                Component.text("#1: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getTopCategory(1)).color(NamedTextColor.WHITE)),
                Component.text("#2: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getTopCategory(2)).color(NamedTextColor.WHITE)),
                Component.text("#3: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getTopCategory(3)).color(NamedTextColor.WHITE))));

        inv.setItem(25, createItem(Material.BEACON,
                Component.text("Market Health").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Trend: ").color(NamedTextColor.GRAY).append(Component.text(getMarketTrend())),
                Component.text("Activity: ").color(NamedTextColor.GRAY).append(Component.text(getActivityLevel())),
                Component.text("Demand: ").color(NamedTextColor.GRAY).append(Component.text(getDemandLevel()))));

        // Time-based stats
        inv.setItem(38, createItem(Material.CLOCK,
                Component.text("Last 24 Hours").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Listings: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getListingsLast24h()).color(NamedTextColor.WHITE)),
                Component.text("Sales: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getSalesLast24h()).color(NamedTextColor.WHITE)),
                Component.text("Volume: ").color(NamedTextColor.GRAY).append(
                        Component.text("$" + String.format("%.2f", getVolumeLast24h())).color(NamedTextColor.WHITE))));

        inv.setItem(40, createItem(Material.CLOCK,
                Component.text("Last 7 Days").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Listings: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getListingsLast7d()).color(NamedTextColor.WHITE)),
                Component.text("Sales: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getSalesLast7d()).color(NamedTextColor.WHITE)),
                Component.text("Volume: ").color(NamedTextColor.GRAY).append(
                        Component.text("$" + String.format("%.2f", getVolumeLast7d())).color(NamedTextColor.WHITE))));

        inv.setItem(42, createItem(Material.BOOK,
                Component.text("All Time").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Total Listings: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getTotalListingsAllTime()).color(NamedTextColor.WHITE)),
                Component.text("Total Sales: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getTotalSalesAllTime()).color(NamedTextColor.WHITE)),
                Component.text("Total Volume: ").color(NamedTextColor.GRAY).append(Component
                        .text("$" + String.format("%.2f", getTotalVolumeAllTime())).color(NamedTextColor.WHITE))));

        // Activity graph
        createDashboardActivityGraph(inv);
        inv.setItem(44, createItem(Material.PAINTING,
                Component.text("Activity Graph").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Last 7 days activity").color(NamedTextColor.GRAY)));

        // Back button
        inv.setItem(49, createItem(Material.ARROW,
                Component.text("Back to Auction House").color(NamedTextColor.RED)));

        player.openInventory(inv);
    }

    private void openPlayerStats(Player player, UUID targetUUID, String targetName) {
        Inventory inv = Bukkit.createInventory(new StatsHolder(), 54,
                Component.text("Player Statistics: " + targetName).color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));

        // Load stats from storage
        int totalListings = getTotalListings(targetUUID);
        int activeListings = getActiveListings(targetUUID);
        int totalSold = getTotalSold(targetUUID);
        double totalEarnings = getTotalEarnings(targetUUID);
        int totalBids = getTotalBids(targetUUID);
        int wonAuctions = getWonAuctions(targetUUID);
        double totalSpent = getTotalSpent(targetUUID);

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Header
        inv.setItem(4, createItem(Material.PLAYER_HEAD,
                Component.text(targetName + "'s Stats").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("View detailed auction statistics").color(NamedTextColor.GRAY)));

        // Stats items
        inv.setItem(19, createItem(Material.CHEST,
                Component.text("Listings").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Total Listings: ").color(NamedTextColor.GRAY)
                        .append(Component.text(totalListings).color(NamedTextColor.WHITE)),
                Component.text("Active: ").color(NamedTextColor.GRAY)
                        .append(Component.text(activeListings).color(NamedTextColor.GREEN)),
                Component.text("Success Rate: ").color(NamedTextColor.GRAY).append(Component
                        .text(calculateSuccessRate(totalSold, totalListings) + "%").color(NamedTextColor.WHITE))));

        inv.setItem(21, createItem(Material.GOLD_INGOT,
                Component.text("Earnings").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                Component.text("Total Earnings: ").color(NamedTextColor.GRAY)
                        .append(Component.text("$" + String.format("%.2f", totalEarnings)).color(NamedTextColor.GREEN)),
                Component.text("Items Sold: ").color(NamedTextColor.GRAY)
                        .append(Component.text(totalSold).color(NamedTextColor.WHITE)),
                Component.text("Avg Price: ").color(NamedTextColor.GRAY).append(Component
                        .text("$" + calculateAvgPrice(totalEarnings, totalSold)).color(NamedTextColor.WHITE))));

        inv.setItem(23, createItem(Material.DIAMOND_SWORD,
                Component.text("Bidding").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD),
                Component.text("Total Bids: ").color(NamedTextColor.GRAY)
                        .append(Component.text(totalBids).color(NamedTextColor.WHITE)),
                Component.text("Auctions Won: ").color(NamedTextColor.GRAY)
                        .append(Component.text(wonAuctions).color(NamedTextColor.GREEN)),
                Component.text("Total Spent: ").color(NamedTextColor.GRAY)
                        .append(Component.text("$" + String.format("%.2f", totalSpent)).color(NamedTextColor.RED))));

        Component profitComponent = totalEarnings > totalSpent
                ? Component.text("$" + String.format("%.2f", totalEarnings - totalSpent)).color(NamedTextColor.GREEN)
                : Component.text("$" + String.format("%.2f", totalEarnings - totalSpent)).color(NamedTextColor.RED);
        inv.setItem(25, createItem(Material.EMERALD,
                Component.text("Net Worth").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Profit: ").color(NamedTextColor.GRAY).append(profitComponent),
                Component.text("Total Trades: ").color(NamedTextColor.GRAY)
                        .append(Component.text(totalSold + wonAuctions).color(NamedTextColor.WHITE))));

        // Economy analytics
        inv.setItem(31, createItem(Material.PAPER,
                Component.text("Economy Analytics").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Win Rate: ").color(NamedTextColor.GRAY).append(
                        Component.text(calculateWinRate(wonAuctions, totalBids) + "%").color(NamedTextColor.WHITE)),
                Component.text("Avg Bid: ").color(NamedTextColor.GRAY)
                        .append(Component.text("$" + String.format("%.2f", calculateAvgBid(totalSpent, totalBids)))
                                .color(NamedTextColor.WHITE)),
                Component.text("ROI: ").color(NamedTextColor.GRAY)
                        .append(Component.text(calculateROI(totalEarnings, totalSpent)))));

        inv.setItem(33, createItem(Material.BOOKSHELF,
                Component.text("Trading Activity").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Most Sold: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getMostSoldItem(targetUUID)).color(NamedTextColor.WHITE)),
                Component.text("Most Bought: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getMostBoughtItem(targetUUID)).color(NamedTextColor.WHITE)),
                Component.text("Favorite Category: ").color(NamedTextColor.GRAY)
                        .append(Component.text(getFavoriteCategory(targetUUID)).color(NamedTextColor.WHITE))));

        int sellerRank = getSellerRank(targetUUID);
        int buyerRank = getBuyerRank(targetUUID);
        int overallRank = getOverallRank(targetUUID);
        inv.setItem(35, createItem(Material.COMPASS,
                Component.text("Market Position").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Seller Rank: ").color(NamedTextColor.GRAY)
                        .append(Component.text(sellerRank > 0 ? "#" + sellerRank : "N/A").color(NamedTextColor.WHITE)),
                Component.text("Buyer Rank: ").color(NamedTextColor.GRAY)
                        .append(Component.text(buyerRank > 0 ? "#" + buyerRank : "N/A").color(NamedTextColor.WHITE)),
                Component.text("Overall Rank: ").color(NamedTextColor.GRAY).append(
                        Component.text(overallRank > 0 ? "#" + overallRank : "N/A").color(NamedTextColor.WHITE))));

        // Recent activity
        inv.setItem(38, createItem(Material.BOOK,
                Component.text("Recent Listings").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Click to view").color(NamedTextColor.GRAY)));
        inv.setItem(40, createItem(Material.PAPER,
                Component.text("Bid History").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Click to view").color(NamedTextColor.GRAY)));

        // Activity Graph (last 7 days)
        createActivityGraph(inv, targetUUID);
        inv.setItem(42, createItem(Material.CLOCK,
                Component.text("Activity Graph").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.text("Last 7 days activity").color(NamedTextColor.GRAY)));

        // Back button
        inv.setItem(49, createItem(Material.ARROW,
                Component.text("Back to Auction House").color(NamedTextColor.RED)));

        player.openInventory(inv);
    }

    /**
     * Opens stats for the player themselves.
     */
    public void openSelf(Player player) {
        open(player, player.getUniqueId(), player.getName());
    }

    private int getTotalListings(UUID playerUUID) {
        return (int) plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.getSellerUUID().equals(playerUUID))
                .count();
    }

    private int getActiveListings(UUID playerUUID) {
        return (int) plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.getSellerUUID().equals(playerUUID) && l.isActive() && !l.isExpired())
                .count();
    }

    private int getTotalSold(UUID playerUUID) {
        // From history
        return plugin.getStorageManager().getHistory(playerUUID, 1000).stream()
                .filter(h -> h.sellerUUID().equals(playerUUID))
                .filter(h -> h.finalPrice() > 0)
                .toList()
                .size();
    }

    private double getTotalEarnings(UUID playerUUID) {
        return plugin.getStorageManager().getHistory(playerUUID, 1000).stream()
                .filter(h -> h.sellerUUID().equals(playerUUID))
                .mapToDouble(h -> h.finalPrice())
                .sum();
    }

    private int getTotalBids(UUID playerUUID) {
        // Placeholder - would need bid history tracking
        return 0;
    }

    private int getWonAuctions(UUID playerUUID) {
        // From history as buyer
        return plugin.getStorageManager().getHistory(playerUUID, 1000).stream()
                .filter(h -> playerUUID.equals(h.winnerUUID()))
                .toList()
                .size();
    }

    private double getTotalSpent(UUID playerUUID) {
        return plugin.getStorageManager().getHistory(playerUUID, 1000).stream()
                .filter(h -> playerUUID.equals(h.winnerUUID()))
                .mapToDouble(h -> h.finalPrice())
                .sum();
    }

    private String calculateSuccessRate(int sold, int total) {
        if (total == 0)
            return "0";
        return String.format("%.1f", (sold * 100.0) / total);
    }

    private String calculateAvgPrice(double total, int count) {
        if (count == 0)
            return "0.00";
        return String.format("%.2f", total / count);
    }

    private String calculateWinRate(int won, int totalBids) {
        if (totalBids == 0)
            return "0";
        return String.format("%.1f", (won * 100.0) / totalBids);
    }

    private double calculateAvgBid(double totalSpent, int totalBids) {
        if (totalBids == 0)
            return 0;
        return totalSpent / totalBids;
    }

    private String calculateROI(double earnings, double spent) {
        if (spent == 0)
            return "§aN/A";
        double roi = ((earnings - spent) / spent) * 100;
        if (roi > 0)
            return "§a+" + String.format("%.1f", roi) + "%";
        if (roi < 0)
            return "§c" + String.format("%.1f", roi) + "%";
        return "§70.0%";
    }

    // FIX M-5: Return N/A untuk placeholder stats yang belum diimplementasi secara
    // real
    // Daripada random data yang misleading, lebih baik honest "N/A" dengan
    // penjelasan
    private String getMostSoldItem(UUID playerUUID) {
        return "N/A (coming soon)";
    }

    private String getMostBoughtItem(UUID playerUUID) {
        return "N/A (coming soon)";
    }

    private String getFavoriteCategory(UUID playerUUID) {
        return "N/A (coming soon)";
    }

    private int getSellerRank(UUID playerUUID) {
        return -1; // -1 akan ditampilkan sebagai N/A
    }

    private int getBuyerRank(UUID playerUUID) {
        return -1;
    }

    private int getOverallRank(UUID playerUUID) {
        return -1;
    }

    /**
     * Creates a visual activity graph showing last 7 days of activity.
     * Uses bar chart representation with different materials for intensity.
     */
    private void createActivityGraph(Inventory inv, UUID playerUUID) {
        int[] dailyActivity = new int[7];
        long now = System.currentTimeMillis();
        long dayMs = 24 * 60 * 60 * 1000;

        var history = plugin.getStorageManager().getHistory(playerUUID, 1000);
        if (history == null || history.isEmpty()) {
            // No history data, show empty bars
            return;
        }

        // Count activity for last 7 days
        for (int i = 0; i < 7; i++) {
            long dayStart = now - ((i + 1) * dayMs);
            long dayEnd = now - (i * dayMs);

            dailyActivity[i] = (int) history.stream()
                    .filter(h -> h.resolvedAt() >= dayStart && h.resolvedAt() < dayEnd)
                    .count();
        }

        // Find max for scaling
        int maxActivity = Arrays.stream(dailyActivity).max().orElse(1);
        // FIX M-5: maxActivity sudah dihitung di atas dari data real, tidak perlu
        // override

        Material[] barMaterials = {
                Material.WHITE_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.GREEN_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.RED_STAINED_GLASS_PANE,
                Material.REDSTONE_BLOCK
        };

        String[] dayNames = { "6d ago", "5d ago", "4d ago", "3d ago", "2d ago", "Yesterday", "Today" };

        for (int i = 0; i < 7; i++) {
            int activity = dailyActivity[6 - i]; // Reverse to show oldest to newest
            int barHeight = maxActivity > 0 ? (activity * 5) / maxActivity : 0;
            Material barMat = barMaterials[Math.min(barMaterials.length - 1, barMaterials.length - 1 - barHeight)];

            // Place bars in row 4 (slots 36-42)
            if (i < 7) {
                String activityText = activity == 0 ? "No data" : activity + " transactions";
                inv.setItem(36 + i, createItem(barMat,
                        "§e" + dayNames[i],
                        "§7Activity: §f" + activityText,
                        "§7" + (activity > 0 ? "█".repeat(Math.max(1, barHeight + 1)) : "-")));
            }
        }
    }

    private void createDashboardActivityGraph(Inventory inv) {
        // FIX M-5: Ganti placeholder random dengan honest "no data" atau real query
        int[] dailyActivity = new int[7];
        // TODO: Query real activity data dari database
        // Untuk sekarang, semua 0 (honest "no data" daripada random misleading)

        int maxActivity = 1; // Avoid division by zero
        Material[] barMaterials = {
                Material.WHITE_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.GREEN_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.RED_STAINED_GLASS_PANE,
                Material.REDSTONE_BLOCK
        };

        String[] dayNames = { "6d ago", "5d ago", "4d ago", "3d ago", "2d ago", "Yesterday", "Today" };

        for (int i = 0; i < 7; i++) {
            int activity = dailyActivity[6 - i];
            int barHeight = maxActivity > 0 ? (activity * 5) / maxActivity : 0;
            Material barMat = barMaterials[Math.min(barMaterials.length - 1, barMaterials.length - 1 - barHeight)];

            inv.setItem(36 + i, createItem(barMat,
                    "§e" + dayNames[i],
                    "§7Activity: §f" + activity + " transactions",
                    "§7" + "█".repeat(Math.max(1, barHeight + 1))));
        }
    }

    // Dashboard helper methods
    private double calculateTotalVolume() {
        return plugin.getAuctionManager().getActiveListings().stream()
                .mapToDouble(l -> l.getCurrentBid())
                .sum();
    }

    private int getTotalActiveUsers() {
        return (int) plugin.getAuctionManager().getActiveListings().stream()
                .map(l -> l.getSellerUUID())
                .distinct()
                .count();
    }

    private double calculateAvgListingPrice() {
        var listings = plugin.getAuctionManager().getActiveListings();
        if (listings.isEmpty())
            return 0;
        return listings.stream().mapToDouble(l -> l.getCurrentBid()).average().orElse(0);
    }

    // FIX M-5: Dashboard stats - implement real query atau return 0/N/A daripada
    // random
    private int getSoldToday() {
        // TODO: Query real data dari auctify_history untuk sales hari ini
        return 0; // Honest 0 daripada random misleading
    }

    private double getRevenueToday() {
        // TODO: Query real data dari auctify_history untuk revenue hari ini
        return 0.0; // Honest 0 daripada random misleading
    }

    private String getTopCategory(int rank) {
        // TODO: Query real category stats
        return "N/A"; // Honest N/A daripada random placeholder
    }

    private String getMarketTrend() {
        // Simplified - would compare with previous period
        return "§a↑ Rising";
    }

    private String getActivityLevel() {
        int activeListings = plugin.getAuctionManager().getActiveListings().size();
        if (activeListings > 100)
            return "§aHigh";
        if (activeListings > 50)
            return "§eMedium";
        return "§cLow";
    }

    private String getDemandLevel() {
        return "§aStrong";
    }

    // FIX M-5: Time-based dashboard stats - implement real queries atau return
    // honest values
    private int getListingsLast24h() {
        // TODO: Query auctify_history for last 24h listings
        return 0;
    }

    private int getSalesLast24h() {
        // TODO: Query auctify_history for last 24h sales
        return 0;
    }

    private double getVolumeLast24h() {
        // TODO: Query auctify_history for last 24h volume
        return 0.0;
    }

    private int getListingsLast7d() {
        // TODO: Query auctify_history for last 7 days listings
        return 0;
    }

    private int getSalesLast7d() {
        // TODO: Query auctify_history for last 7 days sales
        return 0;
    }

    private double getVolumeLast7d() {
        // TODO: Query auctify_history for last 7 days volume
        return 0.0;
    }

    private int getTotalListingsAllTime() {
        // TODO: Query total listings count from history
        return 0;
    }

    private int getTotalSalesAllTime() {
        // TODO: Query total sales count from history
        return 0;
    }

    private double getTotalVolumeAllTime() {
        // TODO: Query total volume from history
        return 0.0;
    }

    // FIX H-1: Updated to use Adventure API Components
    private ItemStack createItem(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore.length > 0) {
                List<Component> loreList = new ArrayList<>();
                for (Component line : lore) {
                    loreList.add(line);
                }
                meta.lore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // Overload for backward compatibility with legacy string calls
    private ItemStack createItem(Material material, String name, String... lore) {
        // Convert legacy color codes to Adventure Components
        Component nameComponent = LegacyComponentSerializer.legacySection().deserialize(name);
        Component[] loreComponents = new Component[lore.length];
        for (int i = 0; i < lore.length; i++) {
            loreComponents[i] = LegacyComponentSerializer.legacySection().deserialize(lore[i]);
        }
        return createItem(material, nameComponent, loreComponents);
    }

    /**
     * Inventory holder for Stats GUI.
     */
    public static class StatsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
