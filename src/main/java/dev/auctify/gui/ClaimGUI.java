package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.util.ColorUtil;
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
 * Mailbox/Claim GUI where players can view and collect pending deliveries.
 * Items that couldn't be delivered (inventory full, offline) are shown here.
 */
public class ClaimGUI {

    private final Auctify plugin;

    public ClaimGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        AuctifyHolder holder = new AuctifyHolder("CLAIM");
        List<ItemStack> pending = plugin.getStorageManager().getPendingDeliveries(player.getUniqueId());

        int size = Math.min(54, Math.max(9, ((pending.size() / 9) + 1) * 9 + 9));
        Inventory inv = Bukkit.createInventory(holder, size,
                ColorUtil.toComponent(MessageUtil.get("gui-claim-title")));

        // Place pending items
        int slot = 0;
        for (ItemStack item : pending) {
            if (slot >= size - 9)
                break; // Leave bottom row for navigation
            inv.setItem(slot++, item);
        }

        // Fill empty item slots with filler
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = slot; i < size - 9; i++) {
            inv.setItem(i, filler);
        }

        // Bottom row
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }

        // Claim All button (center bottom)
        if (!pending.isEmpty()) {
            inv.setItem(size - 5, buildItem(Material.LIME_WOOL,
                    MessageUtil.get("gui-claim-all"),
                    MessageUtil.get("gui-claim-all-lore", Map.of("count", String.valueOf(pending.size())))));
        } else {
            inv.setItem(size - 5, buildItem(Material.BARRIER,
                    MessageUtil.get("gui-claim-empty"),
                    MessageUtil.get("gui-claim-empty-lore")));
        }

        // Back button
        inv.setItem(size - 9, buildItem(Material.ARROW, MessageUtil.get("gui-claim-back")));

        plugin.getGUIManager().markOpen(player, "CLAIM");
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
