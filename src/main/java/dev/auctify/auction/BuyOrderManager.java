package dev.auctify.auction;

import dev.auctify.Auctify;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages Buy Orders (WTB - Want to Buy) system. Players can post requests to
 * buy items at specific prices.
 */
public class BuyOrderManager {

    private final Auctify plugin;
    private final Map<String, BuyOrder> activeOrders = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> ordersByBuyer = new ConcurrentHashMap<>();

    public BuyOrderManager(Auctify plugin) {
        this.plugin = plugin;
        loadBuyOrders();
    }

    /** Loads active buy orders from storage. */
    private void loadBuyOrders() {
        List<BuyOrder> orders = plugin.getStorageManager().getAllBuyOrders();
        for (BuyOrder order : orders) {
            if (order.isActive() && order.getExpiryTime() > System.currentTimeMillis()) {
                activeOrders.put(order.getId(), order);
                ordersByBuyer.computeIfAbsent(order.getBuyerUUID(), k -> new ArrayList<>()).add(order.getId());
            }
        }
        plugin.getLogger().info("[BuyOrderManager] Loaded " + activeOrders.size() + " active buy orders.");
    }

    /** Creates a new buy order (WTB). */
    public String createBuyOrder(Player buyer, Material itemType, int amount, double pricePerUnit) {
        if (!isEnabled()) {
            MessageUtil.send(buyer, "buyorder-disabled", null);
            return null;
        }

        // Validation
        if (itemType == null || itemType == Material.AIR) {
            MessageUtil.send(buyer, "invalid-item", null);
            return null;
        }
        if (amount <= 0 || amount > 64) {
            MessageUtil.send(buyer, "invalid-amount", Map.of("max", "64"));
            return null;
        }
        if (pricePerUnit <= 0) {
            MessageUtil.send(buyer, "invalid-price", null);
            return null;
        }

        double totalCost = pricePerUnit * amount;

        // Check funds
        if (!plugin.getAuctionManager().getEconomy().has(buyer.getUniqueId(), totalCost)) {
            MessageUtil.send(buyer, "insufficient-funds", Map.of("amount", String.format("%.2f", totalCost)));
            return null;
        }

        // Check max orders per player
        int maxOrders = plugin.getConfig().getInt("buyorders.max-per-player", 5);
        if (ordersByBuyer.getOrDefault(buyer.getUniqueId(), Collections.emptyList()).size() >= maxOrders) {
            MessageUtil.send(buyer, "buyorder-max-reached", Map.of("max", String.valueOf(maxOrders)));
            return null;
        }

        // Generate ID and create order
        String id = "BO" + System.currentTimeMillis();
        long duration = plugin.getConfig().getLong("buyorders.duration", 604800) * 1000;

        BuyOrder order = new BuyOrder(
                id, buyer.getUniqueId(), buyer.getName(),
                itemType, amount, pricePerUnit,
                System.currentTimeMillis(), System.currentTimeMillis() + duration);

        // Reserve money
        var result = plugin.getAuctionManager().getEconomy().withdraw(buyer.getUniqueId(), totalCost);
        if (!result.success()) {
            MessageUtil.send(buyer, "insufficient-funds", Map.of("amount", String.format("%.2f", totalCost)));
            return null;
        }

        // Store in memory and database
        activeOrders.put(id, order);
        ordersByBuyer.computeIfAbsent(buyer.getUniqueId(), k -> new ArrayList<>()).add(id);
        try {
            plugin.getStorageManager().saveBuyOrder(order);
        } catch (Exception e) {
            plugin.getLogger().severe("[BuyOrderManager] Failed to save buy order to database: " + e.getMessage());
        }

        // Notify
        double total = pricePerUnit * amount;
        MessageUtil.send(buyer, "buyorder-created", Map.of(
                "id", id, "amount", String.valueOf(amount),
                "item", itemType.name(), "price", String.format("%.2f", pricePerUnit),
                "total", String.format("%.2f", total)));

        plugin.getLoggerManager().logAdmin(buyer.getName(), "created buy order",
                amount + "x " + itemType.name());

        return id;
    }

    /** Fills a buy order by selling items to it. */
    public boolean fillBuyOrder(Player seller, String orderId) {
        BuyOrder order = activeOrders.get(orderId);
        if (order == null || !order.isActive()) {
            MessageUtil.send(seller, "buyorder-not-found", null);
            return false;
        }

        ItemStack needed = new ItemStack(order.getItemType(), order.getAmount());
        if (!seller.getInventory().containsAtLeast(needed, order.getAmount())) {
            MessageUtil.send(seller, "insufficient-items", Map.of(
                    "amount", String.valueOf(order.getAmount()), "item", order.getItemType().name()));
            return false;
        }

        // Remove items from seller
        seller.getInventory().removeItem(needed);

        // Calculate payment
        double payment = order.getPricePerUnit() * order.getAmount();

        // Pay seller
        plugin.getAuctionManager().getEconomy().deposit(seller.getUniqueId(), payment);

        // Give items to buyer
        Player buyer = plugin.getServer().getPlayer(order.getBuyerUUID());
        if (buyer != null && buyer.isOnline()) {
            Map<Integer, ItemStack> overflow = buyer.getInventory().addItem(needed);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
                }
            }
            MessageUtil.send(buyer, "buyorder-filled", Map.of(
                    "amount", String.valueOf(order.getAmount()),
                    "item", order.getItemType().name()));
        } else {
            // Queue for offline delivery
            try {
                plugin.getStorageManager().addPendingBuyDelivery(order.getBuyerUUID(), needed, order.getId());
            } catch (Exception e) {
                plugin.getLogger().severe("[BuyOrderManager] Failed to queue offline delivery: " + e.getMessage());
            }
            MessageUtil.send(seller, "buyorder-offline-delivery", Map.of(
                    "buyer", order.getBuyerName(),
                    "amount", String.valueOf(order.getAmount()),
                    "item", order.getItemType().name()));
        }

        // Complete order
        order.setActive(false);
        activeOrders.remove(orderId);
        ordersByBuyer.computeIfAbsent(order.getBuyerUUID(), k -> new ArrayList<>()).remove(orderId);
        try {
            plugin.getStorageManager().deleteBuyOrder(orderId);
        } catch (Exception e) {
            plugin.getLogger().warning("[BuyOrderManager] Failed to delete buy order from database: " + e.getMessage());
        }

        // Notify seller
        MessageUtil.send(seller, "buyorder-sold", Map.of(
                "amount", String.valueOf(order.getAmount()),
                "item", order.getItemType().name(),
                "buyer", order.getBuyerName(),
                "payment", String.format("%.2f", payment)));

        plugin.getSoundManager().playMoneyReceived(seller);

        return true;
    }

    /** Cancels a buy order and refunds the buyer. */
    public boolean cancelBuyOrder(Player buyer, String orderId) {
        BuyOrder order = activeOrders.get(orderId);
        if (order == null || !order.isActive()) {
            MessageUtil.send(buyer, "buyorder-not-found", null);
            return false;
        }

        if (!order.getBuyerUUID().equals(buyer.getUniqueId()) && !buyer.hasPermission("auctify.admin")) {
            MessageUtil.send(buyer, "not-your-order", null);
            return false;
        }

        double refund = order.getPricePerUnit() * order.getAmount();
        plugin.getAuctionManager().getEconomy().deposit(buyer.getUniqueId(), refund);

        order.setActive(false);
        activeOrders.remove(orderId);
        ordersByBuyer.computeIfAbsent(order.getBuyerUUID(), k -> new ArrayList<>()).remove(orderId);
        try {
            plugin.getStorageManager().deleteBuyOrder(orderId);
        } catch (Exception e) {
            plugin.getLogger().warning("[BuyOrderManager] Failed to delete cancelled order: " + e.getMessage());
        }

        MessageUtil.send(buyer, "buyorder-cancelled", Map.of("id", orderId));
        return true;
    }

    /**
     * Gets all active orders for a material type, sorted by price (highest first).
     */
    public List<BuyOrder> getOrdersForMaterial(Material material) {
        return activeOrders.values().stream()
                .filter(o -> o.isActive() && o.getItemType() == material)
                .sorted(Comparator.comparingDouble(BuyOrder::getPricePerUnit).reversed())
                .collect(Collectors.toList());
    }

    /** Gets all active orders. */
    public Collection<BuyOrder> getActiveOrders() {
        return Collections.unmodifiableCollection(activeOrders.values());
    }

    /** Gets orders for a specific buyer. */
    public List<BuyOrder> getOrdersByBuyer(UUID buyerUUID) {
        return ordersByBuyer.getOrDefault(buyerUUID, Collections.emptyList()).stream()
                .map(activeOrders::get)
                .filter(Objects::nonNull)
                .filter(BuyOrder::isActive)
                .collect(Collectors.toList());
    }

    /** Cleans up expired orders. */
    public void cleanupExpiredOrders() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, BuyOrder>> it = activeOrders.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, BuyOrder> entry = it.next();
            BuyOrder order = entry.getValue();
            if (order.isActive() && order.getExpiryTime() < now) {
                double refund = order.getPricePerUnit() * order.getAmount();
                plugin.getAuctionManager().getEconomy().deposit(order.getBuyerUUID(), refund);

                order.setActive(false);
                it.remove();
                ordersByBuyer.computeIfAbsent(order.getBuyerUUID(), k -> new ArrayList<>()).remove(order.getId());
                try {
                    plugin.getStorageManager().deleteBuyOrder(order.getId());
                } catch (Exception e) {
                    plugin.getLogger().warning("[BuyOrderManager] Failed to delete expired order: " + e.getMessage());
                }

                Player buyer = plugin.getServer().getPlayer(order.getBuyerUUID());
                if (buyer != null && buyer.isOnline()) {
                    MessageUtil.send(buyer, "buyorder-expired", Map.of("id", order.getId()));
                }
            }
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("buyorders.enabled", true);
    }
}
