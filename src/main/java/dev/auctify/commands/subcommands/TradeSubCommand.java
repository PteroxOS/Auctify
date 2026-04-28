package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Trade subcommand - allows direct player-to-player trading.
 */
public class TradeSubCommand implements SubCommand {

    private final Auctify plugin;

    public TradeSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "trade-usage", null);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "send" -> handleSend(player, args);
            case "accept" -> handleAccept(player, args);
            case "cancel" -> handleCancel(player);
            default -> MessageUtil.send(player, "trade-usage", null);
        }
    }

    private void handleSend(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "trade-send-usage", null);
            return;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayerExact(targetName);

        // Fallback for Bedrock players or offline lookup
        if (target == null) {
            var offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (offlineTarget != null && offlineTarget.isOnline()) {
                target = offlineTarget.getPlayer();
            }
        }

        if (target == null || !target.isOnline()) {
            MessageUtil.send(player, "player-not-found", null);
            return;
        }

        ItemStack senderItem = player.getInventory().getItemInMainHand();
        ItemStack targetItem = target.getInventory().getItemInMainHand();

        double senderMoney = 0;
        double targetMoney = 0;

        // Parse money amounts if provided
        if (args.length > 3) {
            try {
                senderMoney = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "invalid-amount", null);
                return;
            }
        }

        if (args.length > 4) {
            try {
                targetMoney = Double.parseDouble(args[4]);
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "invalid-amount", null);
                return;
            }
        }

        String requestId = plugin.getTradeManager().sendTradeRequest(player, target,
                senderItem, targetItem, senderMoney, targetMoney);

        if (requestId != null) {
            MessageUtil.send(player, "trade-details", Map.of(
                    "target", target.getName(),
                    "your-item", senderItem != null ? senderItem.getType().name() : "None",
                    "their-item", targetItem != null ? targetItem.getType().name() : "None",
                    "your-money", String.valueOf(senderMoney),
                    "their-money", String.valueOf(targetMoney)));
        }
    }

    private void handleAccept(Player player, String[] args) {
        var request = plugin.getTradeManager().getPlayerRequest(player.getUniqueId());

        if (request == null) {
            MessageUtil.send(player, "trade-no-request", null);
            return;
        }

        if (plugin.getTradeManager().acceptTradeRequest(player, request.getId())) {
            // Trade completed
        }
    }

    private void handleCancel(Player player) {
        var request = plugin.getTradeManager().getPlayerRequest(player.getUniqueId());

        if (request == null) {
            MessageUtil.send(player, "trade-no-request", null);
            return;
        }

        plugin.getTradeManager().cancelTradeRequest(request.getId(), "manual");
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
