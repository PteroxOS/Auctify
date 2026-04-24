package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Player Statistics GUI showing auction stats and activity.
 */
public class StatsGUI {

    private final Auctify plugin;

    public StatsGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the player statistics GUI.
     *
     * @param player     the player to show stats for
     * @param targetUUID the target player UUID (for viewing others' stats)
     * @param targetName the target player name
     */
    public void open(Player player, UUID targetUUID, String targetName) {
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
        inv.setItem(4, createItem(Material.PLAYER_HEAD, "§6§l" + targetName + "'s Stats",
                "§7View detailed auction statistics"));

        // Stats items
        inv.setItem(19, createItem(Material.CHEST, "§e§lListings",
                "§7Total Listings: §f" + totalListings,
                "§7Active: §a" + activeListings,
                "§7Success Rate: §f" + calculateSuccessRate(totalSold, totalListings) + "%"));

        inv.setItem(21, createItem(Material.GOLD_INGOT, "§a§lEarnings",
                "§7Total Earnings: §a$" + String.format("%.2f", totalEarnings),
                "§7Items Sold: §f" + totalSold,
                "§7Avg Price: §f$" + calculateAvgPrice(totalEarnings, totalSold)));

        inv.setItem(23, createItem(Material.DIAMOND_SWORD, "§b§lBidding",
                "§7Total Bids: §f" + totalBids,
                "§7Auctions Won: §a" + wonAuctions,
                "§7Total Spent: §c$" + String.format("%.2f", totalSpent)));

        inv.setItem(25, createItem(Material.EMERALD, "§6§lNet Worth",
                "§7Profit: §" + (totalEarnings > totalSpent ? "a" : "c") + "$"
                        + String.format("%.2f", totalEarnings - totalSpent),
                "§7Total Trades: §f" + (totalSold + wonAuctions)));

        // Recent activity
        inv.setItem(38, createItem(Material.BOOK, "§e§lRecent Listings", "§7Click to view"));
        inv.setItem(40, createItem(Material.PAPER, "§e§lBid History", "§7Click to view"));

        // Activity Graph (last 7 days)
        createActivityGraph(inv, targetUUID);
        inv.setItem(42, createItem(Material.CLOCK, "§e§lActivity Graph", "§7Last 7 days activity"));

        // Back button
        inv.setItem(49, createItem(Material.ARROW, "§cBack to Auction House"));

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

        // Create visual bars in slots 45-47 (row 5, last 3 slots)
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
                inv.setItem(36 + i, createItem(barMat,
                        "§e" + dayNames[i],
                        "§7Activity: §f" + activity + " transactions",
                        "§7" + "█".repeat(Math.max(1, barHeight + 1))));
            }
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Use legacy color codes support for display name
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
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
