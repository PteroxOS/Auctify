package dev.auctify.trade;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages direct trade requests between players.
 */
public class TradeManager {

    private final Auctify plugin;
    private final Map<String, TradeRequest> activeRequests = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerRequests = new ConcurrentHashMap<>();
    private static final long REQUEST_TIMEOUT = 60000; // 1 minute

    public TradeManager(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends a trade request from sender to target.
     */
    public String sendTradeRequest(Player sender, Player target, ItemStack senderItem, ItemStack targetItem,
            double senderMoney, double targetMoney) {
        if (!plugin.getConfig().getBoolean("trade.enabled", true)) {
            MessageUtil.send(sender, "trade-disabled", null);
            return null;
        }

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            MessageUtil.send(sender, "trade-self", null);
            return null;
        }

        // Check if sender already has a pending request
        if (playerRequests.containsKey(sender.getUniqueId())) {
            MessageUtil.send(sender, "trade-pending", null);
            return null;
        }

        // Check if target already has a pending request
        if (playerRequests.containsKey(target.getUniqueId())) {
            MessageUtil.send(sender, "target-busy", null);
            return null;
        }

        String requestId = "TR" + System.currentTimeMillis();
        TradeRequest request = new TradeRequest(requestId, sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), target.getName(), senderItem, targetItem, senderMoney, targetMoney);

        activeRequests.put(requestId, request);
        playerRequests.put(sender.getUniqueId(), requestId);
        playerRequests.put(target.getUniqueId(), requestId);

        // Notify both players
        MessageUtil.send(sender, "trade-sent", Map.of("target", target.getName()));
        MessageUtil.send(target, "trade-received", Map.of("sender", sender.getName()));

        // Schedule timeout
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (activeRequests.containsKey(requestId) && !request.isAccepted() && !request.isCancelled()) {
                cancelTradeRequest(requestId, "timeout");
            }
        }, 1200L); // 60 seconds (20 ticks per second)

        return requestId;
    }

    /**
     * Accepts a trade request.
     */
    public boolean acceptTradeRequest(Player player, String requestId) {
        TradeRequest request = activeRequests.get(requestId);
        if (request == null) {
            MessageUtil.send(player, "trade-not-found", null);
            return false;
        }

        if (!request.getTargetUUID().equals(player.getUniqueId())) {
            MessageUtil.send(player, "trade-not-recipient", null);
            return false;
        }

        if (request.isExpired(REQUEST_TIMEOUT)) {
            cancelTradeRequest(requestId, "expired");
            return false;
        }

        // Verify items and money still exist
        Player sender = plugin.getServer().getPlayer(request.getSenderUUID());
        if (sender == null || !sender.isOnline()) {
            cancelTradeRequest(requestId, "sender-offline");
            return false;
        }

        // Check sender's item
        if (request.getSenderItem() != null && !sender.getInventory().containsAtLeast(request.getSenderItem(), request.getSenderItem().getAmount())) {
            cancelTradeRequest(requestId, "sender-no-item");
            return false;
        }

        // Check sender's money
        if (request.getSenderMoney() > 0 && !plugin.getEconomyManager().has(sender.getUniqueId(), request.getSenderMoney())) {
            cancelTradeRequest(requestId, "sender-no-money");
            return false;
        }

        // Check target's item
        if (request.getTargetItem() != null && !player.getInventory().containsAtLeast(request.getTargetItem(), request.getTargetItem().getAmount())) {
            cancelTradeRequest(requestId, "target-no-item");
            return false;
        }

        // Check target's money
        if (request.getTargetMoney() > 0 && !plugin.getEconomyManager().has(player.getUniqueId(), request.getTargetMoney())) {
            cancelTradeRequest(requestId, "target-no-money");
            return false;
        }

        // Execute trade
        executeTrade(request, sender, player);
        return true;
    }

    /**
     * Cancels a trade request.
     */
    public boolean cancelTradeRequest(String requestId, String reason) {
        TradeRequest request = activeRequests.get(requestId);
        if (request == null) return false;

        request.setCancelled(true);
        activeRequests.remove(requestId);
        playerRequests.remove(request.getSenderUUID());
        playerRequests.remove(request.getTargetUUID());

        // Notify players
        Player sender = plugin.getServer().getPlayer(request.getSenderUUID());
        Player target = plugin.getServer().getPlayer(request.getTargetUUID());

        if (sender != null && sender.isOnline()) {
            MessageUtil.send(sender, "trade-cancelled-" + reason, Map.of("target", request.getTargetName()));
        }
        if (target != null && target.isOnline()) {
            MessageUtil.send(target, "trade-cancelled-" + reason, Map.of("sender", request.getSenderName()));
        }

        return true;
    }

    /**
     * Executes the trade by transferring items and money.
     */
    private void executeTrade(TradeRequest request, Player sender, Player target) {
        request.setAccepted(true);
        activeRequests.remove(request.getId());
        playerRequests.remove(sender.getUniqueId());
        playerRequests.remove(target.getUniqueId());

        // Transfer items
        if (request.getSenderItem() != null) {
            sender.getInventory().removeItem(request.getSenderItem());
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(request.getSenderItem());
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), drop);
                }
            }
        }

        if (request.getTargetItem() != null) {
            target.getInventory().removeItem(request.getTargetItem());
            Map<Integer, ItemStack> overflow = sender.getInventory().addItem(request.getTargetItem());
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    sender.getWorld().dropItemNaturally(sender.getLocation(), drop);
                }
            }
        }

        // Transfer money
        if (request.getSenderMoney() > 0) {
            plugin.getEconomyManager().withdraw(sender.getUniqueId(), request.getSenderMoney());
            plugin.getEconomyManager().deposit(target.getUniqueId(), request.getSenderMoney());
        }

        if (request.getTargetMoney() > 0) {
            plugin.getEconomyManager().withdraw(target.getUniqueId(), request.getTargetMoney());
            plugin.getEconomyManager().deposit(sender.getUniqueId(), request.getTargetMoney());
        }

        // Notify both players
        MessageUtil.send(sender, "trade-completed", Map.of("target", target.getName()));
        MessageUtil.send(target, "trade-completed", Map.of("sender", sender.getName()));

        plugin.getSoundManager().playMoneyReceived(sender);
        plugin.getSoundManager().playMoneyReceived(target);
    }

    /**
     * Gets a trade request by ID.
     */
    public TradeRequest getRequest(String requestId) {
        return activeRequests.get(requestId);
    }

    /**
     * Gets the active request for a player.
     */
    public TradeRequest getPlayerRequest(UUID playerUUID) {
        String requestId = playerRequests.get(playerUUID);
        return requestId != null ? activeRequests.get(requestId) : null;
    }

    /**
     * Cleans up expired requests.
     */
    public void cleanupExpired() {
        activeRequests.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired(REQUEST_TIMEOUT)) {
                cancelTradeRequest(entry.getKey(), "expired");
                return true;
            }
            return false;
        });
    }
}
