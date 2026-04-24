package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Preview GUI that shows the contents of a Shulker Box listing. Read-only — all
 * clicks are cancelled.
 */
public class ShulkerPreviewGUI {

    private final Auctify plugin;

    public ShulkerPreviewGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Opens a read-only preview of a shulker box's contents. */
    public void open(Player player, ItemStack shulkerItem, String listingId) {
        AuctifyHolder holder = new AuctifyHolder("SHULKER");
        holder.setListingId(listingId);
        Inventory inv = Bukkit.createInventory(holder, 36,
                ColorUtil.toComponent(MessageUtil.get("gui-shulker-title")));

        // Extract shulker contents
        if (shulkerItem.getItemMeta() instanceof BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof ShulkerBox shulker) {
                ItemStack[] contents = shulker.getInventory().getContents();
                for (int i = 0; i < Math.min(contents.length, 27); i++) {
                    if (contents[i] != null && !contents[i].getType().isAir()) {
                        inv.setItem(i, contents[i].clone());
                    }
                }
            }
        }

        // Fill empty slots in shulker area
        ItemStack filler = buildItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        // Bottom row filler
        ItemStack navFiller = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, navFiller);
        }

        // Back button
        inv.setItem(31, buildItem(Material.ARROW, MessageUtil.get("gui-shulker-back")));

        plugin.getGUIManager().markOpen(player, "SHULKER");
        plugin.getGUIManager().setViewingListing(player, listingId);
        player.openInventory(inv);
    }

    /**
     * Checks if an ItemStack is a shulker box.
     */
    public static boolean isShulkerBox(ItemStack item) {
        if (item == null)
            return false;
        return item.getType().name().endsWith("SHULKER_BOX");
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
