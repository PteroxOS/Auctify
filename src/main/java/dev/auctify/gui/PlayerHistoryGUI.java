package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionHistory;
import dev.auctify.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * GUI for viewing a specific player's auction history.
 */
public class PlayerHistoryGUI {

    private final Auctify plugin;

    public PlayerHistoryGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Opens the player history search GUI. */
    public void openSearch(Player player) {
        Inventory inv = Bukkit.createInventory(new HistoryHolder(), 45,
                Component.text("Player History Search").color(NamedTextColor.GOLD));

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        // Info item
        inv.setItem(13, createItem(Material.BOOK, "§e§lPlayer History Search",
                "§7Enter a player name to view their",
                "§7auction history and transactions.",
                "",
                "§7Use §f/ac admin history <player> §7to search"));

        // Back button
        inv.setItem(40, createItem(Material.ARROW, "§cBack to Admin Panel"));

        player.openInventory(inv);
    }

    /** Opens the player history for a specific player. */
    public void open(Player player, UUID playerUUID, String playerName, int page) {
        HistoryHolder holder = new HistoryHolder();
        holder.setPlayerUUID(playerUUID);
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("History: " + playerName).color(NamedTextColor.GOLD));

        // Get player history
        List<AuctionHistory> history = plugin.getStorageManager().getHistory(playerUUID, 1000);

        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) history.size() / itemsPerPage));
        if (page >= totalPages)
            page = totalPages - 1;
        if (page < 0)
            page = 0;

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Fill history items
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, history.size());

        for (int i = startIndex; i < endIndex; i++) {
            AuctionHistory h = history.get(i);
            int slot = i - startIndex;

            Material material = h.finalPrice() > 0 ? Material.EMERALD : Material.BARRIER;
            String status = h.finalPrice() > 0 ? "§aSold" : "§cExpired";

            inv.setItem(slot, createItem(material,
                    "§e" + h.itemData(),
                    "§7Status: " + status,
                    "§7Price: §f" + plugin.getEconomyManager().format(h.finalPrice()),
                    "§7Date: §f" + formatDate(h.resolvedAt()),
                    "§7Winner: §f" + (h.winnerUUID() != null ? h.winnerName() : "None")));
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "§ePrevious Page"));
        }

        // Stats
        int totalSold = (int) history.stream().filter(h -> h.finalPrice() > 0).count();
        double totalRevenue = history.stream().mapToDouble(AuctionHistory::finalPrice).sum();

        inv.setItem(49, createItem(Material.BOOK, "§e§lStatistics",
                "§7Player: §f" + playerName,
                "§7Total Transactions: §f" + history.size(),
                "§7Items Sold: §f" + totalSold,
                "§7Total Revenue: §a" + plugin.getEconomyManager().format(totalRevenue),
                "§7Page: §f" + (page + 1) + "§7/§f" + totalPages));

        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, "§eNext Page"));
        }

        // Back button
        inv.setItem(48, createItem(Material.BARRIER, "§cBack to Search"));

        player.openInventory(inv);
    }

    private String formatDate(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0)
            return days + "d ago";
        if (hours > 0)
            return hours + "h ago";
        if (minutes > 0)
            return minutes + "m ago";
        return "Just now";
    }

    private ItemStack createItem(Material material, String name, String... lore) {
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

    public static class HistoryHolder extends AuctifyHolder {
        private UUID playerUUID;
        private int page;

        public HistoryHolder() {
            super("HISTORY");
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

        public void setPlayerUUID(UUID playerUUID) {
            this.playerUUID = playerUUID;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
