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
 * GUI for viewing economy transaction audit logs.
 */
public class AuditLogGUI {

    private final Auctify plugin;

    public AuditLogGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Opens the audit log GUI. */
    public void open(Player player, int page) {
        AuditHolder holder = new AuditHolder();
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 54,
                Component.text("Economy Audit Log").color(NamedTextColor.GOLD));

        // Fill with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Sample audit log entries (would be loaded from storage in production)
        String[] logEntries = {
                "§a[SALE] PlayerX sold Diamond for §f$1000",
                "§e[BID] PlayerY bid §f$500 on Iron Sword",
                "§c[CANCEL] PlayerZ cancelled listing ABC123",
                "§a[SALE] PlayerA sold Gold Ingot for §f$200",
                "§e[BID] PlayerB bid §f$150 on Stone",
                "§a[SALE] PlayerC sold Emerald for §f$5000",
                "§c[CANCEL] PlayerD cancelled listing DEF456",
                "§a[SALE] PlayerE sold Netherite for §f$10000",
                "§e[BID] PlayerF bid §f$2000 on Diamond Pickaxe",
                "§a[SALE] PlayerG sold Iron for §f$300"
        };

        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) logEntries.length / itemsPerPage));
        if (page >= totalPages)
            page = totalPages - 1;
        if (page < 0)
            page = 0;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, logEntries.length);

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            inv.setItem(slot, createItem(Material.PAPER, "§eLog Entry #" + (i + 1),
                    logEntries[i],
                    "§7Timestamp: §f" + (System.currentTimeMillis() - (i * 60000L))));
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "§ePrevious Page"));
        }

        // Stats
        inv.setItem(49, createItem(Material.BOOK, "§e§lAudit Statistics",
                "§7Total Transactions: §f" + logEntries.length,
                "§7Sales: §a" + (logEntries.length / 2),
                "§7Cancellations: §c" + (logEntries.length / 4),
                "§7Page: §f" + (page + 1) + "§7/§f" + totalPages));

        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, "§eNext Page"));
        }

        // Back button
        inv.setItem(48, createItem(Material.BARRIER, "§cBack to Admin Panel"));

        player.openInventory(inv);
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

    public static class AuditHolder extends AuctifyHolder {
        private int page;

        public AuditHolder() {
            super("AUDIT");
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
