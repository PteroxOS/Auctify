package dev.auctify.gui;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ColorUtil;
import dev.auctify.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The main auction house GUI displayed when players run /ac open.
 * Shows paginated active listings with navigation and auto-refresh.
 * Uses Adventure API for all item display names and lore.
 * All display text is loaded from the locale file via MessageUtil.
 */
public class AuctionGUI {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public AuctionGUI(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the main auction GUI for a player at page 0.
     *
     * @param player the player to show the GUI to
     */
    public void open(Player player) {
        open(player, 0, "ALL");
    }

    public void open(Player player, int page) {
        open(player, page, "ALL", "TIME_ASC");
    }

    /**
     * Opens the main auction GUI for a player at a specific page and category.
     */
    public void open(Player player, int page, String category) {
        open(player, page, category, "TIME_ASC");
    }

    /**
     * Opens the main auction GUI with sort mode.
     */
    public void open(Player player, int page, String category, String sortMode) {
        open(player, page, category, sortMode, null);
    }

    /**
     * Opens the main auction GUI with search query.
     */
    public void open(Player player, int page, String category, String sortMode, String query) {
        open(player, page, category, sortMode, query, null, null, null, null);
    }

    public void open(Player player, int page, String category, String sortMode, String query,
            Double minPrice, Double maxPrice, String sellerName, Long maxEndTime) {
        var config = plugin.getConfig();
        int rows = config.getInt("gui.rows", 6);
        if (rows < 3)
            rows = 3;
        if (rows > 6)
            rows = 6;

        String title = config.getString("gui.title", "§8✦ §6Auctify §8— §7Auction House §8✦");
        AuctifyHolder holder = new AuctifyHolder("MAIN");
        holder.setPage(page);
        holder.setCategory(category);
        holder.setSortMode(sortMode);
        holder.setQuery(query);
        holder.setMinPrice(minPrice);
        holder.setMaxPrice(maxPrice);
        holder.setSellerName(sellerName);
        holder.setMaxEndTime(maxEndTime);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, ColorUtil.toComponent(title));

        int itemsPerPage = config.getInt("gui.items-per-page", -1);
        if (itemsPerPage <= 0) {
            itemsPerPage = (rows - 1) * 9;
        }

        List<AuctionListing> listings = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> !l.isExpired())
                .filter(l -> l.getItem() != null && matchesCategory(l.getItem().getType(), category))
                .filter(l -> query == null || query.isEmpty() || l.getItem() == null ||
                        l.getItem().getType().name().toLowerCase().contains(query.toLowerCase()) ||
                        (l.getItem().hasItemMeta() && l.getItem().getItemMeta().hasDisplayName() &&
                                l.getItem().getItemMeta().getDisplayName().toLowerCase().contains(query.toLowerCase())))
                .filter(l -> minPrice == null || l.getCurrentBid() >= minPrice)
                .filter(l -> maxPrice == null || l.getCurrentBid() <= maxPrice)
                .filter(l -> sellerName == null || sellerName.isEmpty()
                        || l.getSellerName().toLowerCase().contains(sellerName.toLowerCase()))
                .filter(l -> maxEndTime == null || l.getEndTime() <= maxEndTime)
                .sorted(getSortComparator(sortMode))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / itemsPerPage));
        if (page >= totalPages)
            page = totalPages - 1;
        if (page < 0)
            page = 0;

        // Fill listing slots
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, listings.size());
        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, listings.get(i).buildDisplayItem(config));
        }

        // Fill empty slots with filler
        Material filler = Material.matchMaterial(config.getString("gui.filler-material", "GRAY_STAINED_GLASS_PANE"));
        if (filler == null)
            filler = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack fillerItem = createFillerItem(filler);

        for (int i = endIndex - startIndex; i < (rows - 1) * 9; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, fillerItem);
        }

        // Bottom navigation row — fill with filler first
        int bottomRow = (rows - 1) * 9;
        for (int i = bottomRow; i < rows * 9; i++) {
            inv.setItem(i, fillerItem);
        }

        int prevSlot = config.getInt("gui.navigation.previous-page-slot", 45);
        int nextSlot = config.getInt("gui.navigation.next-page-slot", 53);
        int infoSlot = config.getInt("gui.navigation.info-slot", 49);
        int claimSlot = config.getInt("gui.buttons.claim-slot", 46);
        int categorySlot = config.getInt("gui.buttons.category-cycle-slot", 47);
        int refreshSlot = config.getInt("gui.buttons.refresh-slot", 48);
        int watchlistSlot = config.getInt("gui.buttons.watchlist-slot", 50);
        int historySlot = config.getInt("gui.buttons.history-slot", 51);
        int sortSlot = config.getInt("gui.buttons.sort-slot", 52);

        if (page > 0) {
            inv.setItem(prevSlot, createNavItem(Material.ARROW, MessageUtil.get("gui-previous-page")));
        }

        // Claim/Mailbox Button
        int pendingCount = plugin.getStorageManager().getPendingDeliveries(player.getUniqueId()).size();
        if (pendingCount > 0) {
            inv.setItem(claimSlot, createNavItem(Material.CHEST,
                    MessageUtil.get("gui-claim-button"),
                    MessageUtil.get("gui-claim-button-lore", Map.of("count", String.valueOf(pendingCount)))));
        } else {
            inv.setItem(claimSlot, createNavItem(Material.NAME_TAG,
                    MessageUtil.get("gui-search-title"),
                    MessageUtil.get("gui-search-lore-1"),
                    MessageUtil.get("gui-search-lore-2")));
        }

        // Category Button (Slot 47)
        String catDisplayName = getCategoryDisplayName(category);
        Material catMat = switch (category) {
            case "WEAPONS_TOOLS" -> Material.DIAMOND_SWORD;
            case "ARMOR" -> Material.DIAMOND_CHESTPLATE;
            case "BLOCKS" -> Material.GRASS_BLOCK;
            case "MISC" -> Material.APPLE;
            default -> Material.HOPPER;
        };
        inv.setItem(categorySlot, createNavItem(catMat,
                MessageUtil.get("gui-category-title"),
                MessageUtil.get("gui-category-current", Map.of("category", catDisplayName)),
                "",
                MessageUtil.get("gui-category-click"),
                category.equals("ALL") ? "§a▶ " + MessageUtil.get("gui-category-all")
                        : "§7- " + MessageUtil.get("gui-category-all"),
                category.equals("WEAPONS_TOOLS") ? "§a▶ " + MessageUtil.get("gui-category-weapons")
                        : "§7- " + MessageUtil.get("gui-category-weapons"),
                category.equals("ARMOR") ? "§a▶ " + MessageUtil.get("gui-category-armor")
                        : "§7- " + MessageUtil.get("gui-category-armor"),
                category.equals("BLOCKS") ? "§a▶ " + MessageUtil.get("gui-category-blocks")
                        : "§7- " + MessageUtil.get("gui-category-blocks"),
                category.equals("MISC") ? "§a▶ " + MessageUtil.get("gui-category-misc")
                        : "§7- " + MessageUtil.get("gui-category-misc")));

        // Refresh Button
        inv.setItem(refreshSlot, createNavItem(Material.SUNFLOWER,
                MessageUtil.get("gui-refresh-title"),
                MessageUtil.get("gui-refresh-lore-1"),
                MessageUtil.get("gui-refresh-lore-2")));

        // Profile Item (Slot 50)
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        long activeCount = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.getSellerUUID().equals(player.getUniqueId()) && l.isActive())
                .count();
        int maxListings = plugin.getAuctionManager().getMaxListingsForPlayer(player);
        inv.setItem(50, createNavItem(Material.PLAYER_HEAD,
                MessageUtil.get("gui-profile-title", Map.of("player", player.getName())),
                MessageUtil.get("gui-profile-balance", Map.of("balance", plugin.getEconomyManager().format(balance))),
                MessageUtil.get("gui-profile-listings",
                        Map.of("active", String.valueOf(activeCount), "max", String.valueOf(maxListings)))));

        // History Button
        inv.setItem(historySlot, createNavItem(Material.CLOCK,
                MessageUtil.get("gui-history-title"),
                MessageUtil.get("gui-history-lore-1"),
                MessageUtil.get("gui-history-lore-2")));

        // Sort Button
        inv.setItem(sortSlot, createNavItem(Material.COMPARATOR,
                MessageUtil.get("gui-sort-title"),
                MessageUtil.get("gui-sort-lore-1"),
                MessageUtil.get("gui-sort-lore-2")));

        // Page Info
        inv.setItem(infoSlot, createNavItem(Material.BOOK,
                MessageUtil.get("gui-page-info",
                        Map.of("current", String.valueOf(page + 1), "total", String.valueOf(totalPages))),
                MessageUtil.get("gui-page-listings", Map.of("count", String.valueOf(listings.size())))));

        if (page < totalPages - 1) {
            inv.setItem(nextSlot, createNavItem(Material.ARROW, MessageUtil.get("gui-next-page")));
        }

        // Track state
        GUIManager guiManager = plugin.getGUIManager();
        guiManager.markOpen(player, "MAIN");
        guiManager.setPage(player, page);

        player.openInventory(inv);

        // Set up auto-refresh
        if (config.getBoolean("gui.auto-refresh", true)) {
            int intervalTicks = config.getInt("gui.auto-refresh-interval", 5) * 20;
            final int finalPage = page;
            final String finalCategory = category;
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (guiManager.getOpenGUI(player).map("MAIN"::equals).orElse(false)) {
                    refresh(player, finalPage, finalCategory);
                }
            }, intervalTicks, intervalTicks);
            guiManager.registerRefreshTask(player, task);
        }
    }

    /**
     * Refreshes the GUI contents without reopening the inventory.
     */
    public void refresh(Player player, int page, String category) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null)
            return;
        if (!(inv.getHolder() instanceof AuctifyHolder holder))
            return;

        var config = plugin.getConfig();
        int rows = config.getInt("gui.rows", 6);
        if (rows < 3)
            rows = 3;
        if (rows > 6)
            rows = 6;

        int itemsPerPage = config.getInt("gui.items-per-page", -1);
        if (itemsPerPage <= 0)
            itemsPerPage = (rows - 1) * 9;

        // Get filter values from holder
        String query = holder.getQuery();
        Double minPrice = holder.getMinPrice();
        Double maxPrice = holder.getMaxPrice();
        String sellerName = holder.getSellerName();
        Long maxEndTime = holder.getMaxEndTime();
        String sortMode = holder.getSortMode();

        List<AuctionListing> listings = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> !l.isExpired())
                .filter(l -> l.getItem() != null && matchesCategory(l.getItem().getType(), category))
                .filter(l -> query == null || query.isEmpty() || l.getItem() == null ||
                        l.getItem().getType().name().toLowerCase().contains(query.toLowerCase()) ||
                        (l.getItem().hasItemMeta() && l.getItem().getItemMeta().hasDisplayName() &&
                                l.getItem().getItemMeta().getDisplayName().toLowerCase().contains(query.toLowerCase())))
                .filter(l -> minPrice == null || l.getCurrentBid() >= minPrice)
                .filter(l -> maxPrice == null || l.getCurrentBid() <= maxPrice)
                .filter(l -> sellerName == null || sellerName.isEmpty()
                        || l.getSellerName().toLowerCase().contains(sellerName.toLowerCase()))
                .filter(l -> maxEndTime == null || l.getEndTime() <= maxEndTime)
                .sorted(getSortComparator(sortMode))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / itemsPerPage));

        Material filler = Material.matchMaterial(config.getString("gui.filler-material", "GRAY_STAINED_GLASS_PANE"));
        if (filler == null)
            filler = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack fillerItem = createFillerItem(filler);

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, listings.size());

        for (int i = 0; i < (rows - 1) * 9; i++) {
            int listIndex = startIndex + i;
            if (listIndex < endIndex) {
                inv.setItem(i, listings.get(listIndex).buildDisplayItem(config));
            } else {
                inv.setItem(i, fillerItem);
            }
        }

        // Profile
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        long activeCount = plugin.getAuctionManager().getActiveListings().stream()
                .filter(l -> l.getSellerUUID().equals(player.getUniqueId()) && l.isActive())
                .count();
        int maxListings = plugin.getAuctionManager().getMaxListingsForPlayer(player);
        int watchlistSlot = config.getInt("gui.buttons.watchlist-slot", 50);
        int historySlot = config.getInt("gui.buttons.history-slot", 51);
        inv.setItem(watchlistSlot, createNavItem(Material.PLAYER_HEAD,
                MessageUtil.get("gui-profile-title", Map.of("player", player.getName())),
                MessageUtil.get("gui-profile-balance", Map.of("balance", plugin.getEconomyManager().format(balance))),
                MessageUtil.get("gui-profile-listings",
                        Map.of("active", String.valueOf(activeCount), "max", String.valueOf(maxListings)))));

        // History
        inv.setItem(historySlot, createNavItem(Material.CLOCK,
                MessageUtil.get("gui-history-title"),
                MessageUtil.get("gui-history-lore-1"),
                MessageUtil.get("gui-history-lore-2")));

        int infoSlot = config.getInt("gui.navigation.info-slot", 49);
        inv.setItem(infoSlot, createNavItem(Material.BOOK,
                MessageUtil.get("gui-page-info",
                        Map.of("current", String.valueOf(page + 1), "total", String.valueOf(totalPages))),
                MessageUtil.get("gui-page-listings", Map.of("count", String.valueOf(listings.size())))));
    }

    /** Creates a filler item with an empty display name using Adventure API. */
    private ItemStack createFillerItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.toComponent(name));
            if (lore.length > 0) {
                meta.lore(java.util.Arrays.stream(lore)
                        .map(ColorUtil::toComponent)
                        .collect(Collectors.toList()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getCategoryDisplayName(String category) {
        return switch (category) {
            case "WEAPONS_TOOLS" -> MessageUtil.get("gui-category-weapons");
            case "ARMOR" -> MessageUtil.get("gui-category-armor");
            case "BLOCKS" -> MessageUtil.get("gui-category-blocks");
            case "MISC" -> MessageUtil.get("gui-category-misc");
            default -> MessageUtil.get("gui-category-all");
        };
    }

    private boolean matchesCategory(Material mat, String category) {
        if ("ALL".equals(category))
            return true;
        String name = mat.name();
        switch (category) {
            case "WEAPONS_TOOLS":
                return name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                        || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.equals("BOW")
                        || name.equals("CROSSBOW") || name.equals("TRIDENT");
            case "ARMOR":
                return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                        || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("SHIELD");
            case "BLOCKS":
                return mat.isBlock();
            case "MISC":
                return !mat.isBlock() && !name.endsWith("_SWORD") && !name.endsWith("_PICKAXE")
                        && !name.endsWith("_AXE") && !name.endsWith("_SHOVEL") && !name.endsWith("_HOE")
                        && !name.endsWith("_HELMET") && !name.endsWith("_CHESTPLATE")
                        && !name.endsWith("_LEGGINGS") && !name.endsWith("_BOOTS")
                        && !name.equals("BOW") && !name.equals("CROSSBOW") && !name.equals("TRIDENT")
                        && !name.equals("SHIELD");
            default:
                return true;
        }
    }

    private java.util.Comparator<AuctionListing> getSortComparator(String sortMode) {
        return switch (sortMode) {
            case "TIME_DESC" -> (a, b) -> Long.compare(b.getEndTime(), a.getEndTime());
            case "PRICE_ASC" -> (a, b) -> Double.compare(a.getCurrentBid(), b.getCurrentBid());
            case "PRICE_DESC" -> (a, b) -> Double.compare(b.getCurrentBid(), a.getCurrentBid());
            case "BIDS" -> (a, b) -> Integer.compare(b.getBidHistory().size(), a.getBidHistory().size());
            case "NEWEST" -> (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt());
            case "ENDING_SOON" -> (a, b) -> Long.compare(a.getEndTime(), b.getEndTime());
            default -> (a, b) -> Long.compare(a.getEndTime(), b.getEndTime()); // TIME_ASC
        };
    }
}
