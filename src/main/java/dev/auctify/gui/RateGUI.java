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

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Rating GUI shown after a buyer wins an auction. Allows the buyer to rate the
 * seller from 1 to 5 stars.
 */
public class RateGUI {

    private final Auctify plugin;

    public RateGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Opens the rating GUI for a player to rate a seller. */
    public void open(Player player, UUID sellerUUID, String sellerName) {
        // FIX-8a: Cegah player rate diri sendiri
        if (player.getUniqueId().equals(sellerUUID)) {
            MessageUtil.send(player, "rate-self-not-allowed", null);
            return;
        }

        // FIX-8b: Cek apakah player pernah bertransaksi dengan seller (won auction from
        // seller)
        if (!plugin.getStorageManager().hasTransactionWith(player.getUniqueId(), sellerUUID)) {
            MessageUtil.send(player, "rate-no-transaction", null);
            return;
        }

        // FIX-8c: Cek apakah player sudah pernah rate seller ini
        if (plugin.getStorageManager().hasRated(sellerUUID, player.getUniqueId())) {
            MessageUtil.send(player, "rate-already-rated", null);
            return;
        }

        AuctifyHolder holder = new AuctifyHolder("RATE");
        holder.setTargetPlayerUUID(sellerUUID.toString());
        Inventory inv = Bukkit.createInventory(holder, 27,
                ColorUtil.toComponent(MessageUtil.get("gui-rate-title", Map.of("seller", sellerName))));

        // Fill with filler
        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        // Star ratings: slots 11, 12, 13, 14, 15
        Material starMat = Material.NETHER_STAR;
        for (int stars = 1; stars <= 5; stars++) {
            String starDisplay = "§e" + "★".repeat(stars) + "§8" + "☆".repeat(5 - stars);
            inv.setItem(10 + stars, buildItem(starMat,
                    MessageUtil.get("gui-rate-star", Map.of("stars", String.valueOf(stars))),
                    starDisplay,
                    "",
                    MessageUtil.get("gui-rate-click")));
        }

        // Info item (slot 4)
        double avgRating = plugin.getStorageManager().getAverageRating(sellerUUID);
        int ratingCount = plugin.getStorageManager().getRatingCount(sellerUUID);
        String avgDisplay = avgRating >= 0 ? String.format("%.1f", avgRating) : MessageUtil.get("gui-rate-no-ratings");
        inv.setItem(4, buildItem(Material.PLAYER_HEAD,
                MessageUtil.get("gui-rate-seller-info", Map.of("seller", sellerName)),
                MessageUtil.get("gui-rate-avg", Map.of("avg", avgDisplay)),
                MessageUtil.get("gui-rate-count", Map.of("count", String.valueOf(ratingCount)))));

        // Skip button (slot 22)
        inv.setItem(22, buildItem(Material.BARRIER,
                MessageUtil.get("gui-rate-skip"),
                MessageUtil.get("gui-rate-skip-lore")));

        plugin.getGUIManager().markOpen(player, "RATE");
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
