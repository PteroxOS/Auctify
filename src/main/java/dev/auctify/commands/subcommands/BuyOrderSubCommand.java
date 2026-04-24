package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * /ac buyorder — Manage buy orders (WTB system). Usage: /ac buyorder create
 * <price per unit> — Creates buy order for held item, /ac buyorder list — List
 * your active buy orders, /ac buyorder cancel <id> — Cancel a buy order, /ac
 * buyorder browse — Browse active buy orders from others.
 */
public class BuyOrderSubCommand implements SubCommand {

    private final Auctify plugin;

    public BuyOrderSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (!player.hasPermission("auctify.buyorder")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length < 2) {
            showUsage(player);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create", "c" -> handleCreate(player, args);
            case "list", "l" -> handleList(player);
            case "cancel", "remove" -> handleCancel(player, args);
            case "browse", "b", "view" -> handleBrowse(player);
            case "sell", "s", "fill", "f" -> handleSell(player, args);
            default -> showUsage(player);
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "buyorder-usage-create", null);
            return;
        }

        // Get item in hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            MessageUtil.send(player, "buyorder-no-item", null);
            return;
        }

        // Parse price per unit/stack
        double pricePerUnit;
        try {
            pricePerUnit = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        // Parse amount (default to full stack in hand, or specified amount)
        int amount;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0 || amount > 64) {
                    MessageUtil.send(player, "buyorder-invalid-amount", Map.of("max", "64"));
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "buyorder-invalid-amount", Map.of("max", "64"));
                return;
            }
        } else {
            // Default: use full amount of items in hand
            amount = Math.min(hand.getAmount(), 64);
        }

        String orderId = plugin.getBuyOrderManager().createBuyOrder(
                player, hand.getType(), amount, pricePerUnit);

        if (orderId != null) {
            plugin.getSoundManager().playSuccess(player);
        }
    }

    private void handleList(Player player) {
        var orders = plugin.getBuyOrderManager().getOrdersByBuyer(player.getUniqueId());

        if (orders.isEmpty()) {
            MessageUtil.send(player, "buyorder-list-empty", null);
            return;
        }

        MessageUtil.send(player, "buyorder-list-header", Map.of("count", String.valueOf(orders.size())));

        for (var order : orders) {
            MessageUtil.send(player, "buyorder-list-entry", Map.of(
                    "id", order.getId(),
                    "amount", String.valueOf(order.getAmount()),
                    "item", order.getItemType().name(),
                    "price", String.format("%.2f", order.getPricePerUnit())));
        }
    }

    private void handleCancel(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "buyorder-usage-cancel", null);
            return;
        }

        String orderId = args[2];
        if (plugin.getBuyOrderManager().cancelBuyOrder(player, orderId)) {
            plugin.getSoundManager().playSuccess(player);
        }
    }

    private void handleBrowse(Player player) {
        var orders = plugin.getBuyOrderManager().getActiveOrders();

        if (orders.isEmpty()) {
            MessageUtil.send(player, "buyorder-browse-empty", null);
            return;
        }

        MessageUtil.send(player, "buyorder-browse-header", Map.of("count", String.valueOf(orders.size())));

        // Show first 10 orders
        int count = 0;
        for (var order : orders) {
            if (count++ >= 10)
                break;
            if (!order.isActive())
                continue;

            MessageUtil.send(player, "buyorder-browse-entry", Map.of(
                    "id", order.getId(),
                    "buyer", order.getBuyerName(),
                    "amount", String.valueOf(order.getAmount()),
                    "item", order.getItemType().name(),
                    "price", String.format("%.2f", order.getPricePerUnit()),
                    "total", String.format("%.2f", order.getPricePerUnit() * order.getAmount())));
        }
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, "buyorder-sell-usage", null);
            return;
        }

        String orderId = args[1];
        int amount = 1; // default to 1 item

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    MessageUtil.send(player, "buyorder-invalid-amount", Map.of("max", "64"));
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "buyorder-invalid-amount", Map.of("max", "64"));
                return;
            }
        }

        // Get item in hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            MessageUtil.send(player, "buyorder-sell-no-item", null);
            return;
        }

        // Try to fill the buy order
        boolean success = plugin.getBuyOrderManager().fillBuyOrder(player, orderId);

        if (success) {
            plugin.getSoundManager().playSuccess(player);
        }
    }

    private void showUsage(Player player) {
        MessageUtil.send(player, "buyorder-usage-title", null);
        MessageUtil.send(player, "buyorder-usage-create", null);
        MessageUtil.send(player, "buyorder-usage-list", null);
        MessageUtil.send(player, "buyorder-usage-cancel", null);
        MessageUtil.send(player, "buyorder-usage-browse", null);
        MessageUtil.send(player, "buyorder-usage-sell", null);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
