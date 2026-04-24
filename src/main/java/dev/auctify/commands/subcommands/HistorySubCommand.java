package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionHistory;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the /ac history command. Shows the player's recent auction history
 * (as seller or winner).
 */
public class HistorySubCommand implements SubCommand {

    private final Auctify plugin;

    /** Date formatter for displaying resolved timestamps. */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");

    /** Constructor. */
    public HistorySubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (!player.hasPermission("auctify.history")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        UUID targetUUID;
        String targetName;

        // Admin can view other players' history
        if (args.length >= 2 && player.hasPermission("auctify.admin.history")) {
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                // Try to get offline player
                targetUUID = plugin.getServer().getOfflinePlayer(args[1]).getUniqueId();
                targetName = args[1];
            } else {
                targetUUID = target.getUniqueId();
                targetName = target.getName();
            }
        } else {
            targetUUID = player.getUniqueId();
            targetName = player.getName();
        }

        List<AuctionHistory> history = plugin.getAuctionManager().getHistory(targetUUID, 10);

        if (history.isEmpty()) {
            if (targetUUID.equals(player.getUniqueId())) {
                MessageUtil.send(player, "history-empty", null);
            } else {
                MessageUtil.send(player, "history-empty-other", Map.of("player", targetName));
            }
            return;
        }

        if (targetUUID.equals(player.getUniqueId())) {
            MessageUtil.send(player, "history-header", Map.of("count", "10"));
        } else {
            MessageUtil.send(player, "history-header-other", Map.of("count", "10", "player", targetName));
        }

        for (AuctionHistory h : history) {
            String date = DATE_FORMAT.format(new Date(h.resolvedAt()));

            if ("SOLD".equals(h.reason())) {
                if (h.taxAmount() > 0) {
                    MessageUtil.send(player, "history-line-sold", Map.of(
                            "date", date,
                            "id", h.id(),
                            "price", plugin.getEconomyManager().format(h.finalPrice()),
                            "tax", plugin.getEconomyManager().format(h.taxAmount())));
                } else {
                    MessageUtil.send(player, "history-line-sold-no-tax", Map.of(
                            "date", date,
                            "id", h.id(),
                            "price", plugin.getEconomyManager().format(h.finalPrice())));
                }
            } else if ("EXPIRED".equals(h.reason())) {
                MessageUtil.send(player, "history-line-expired", Map.of("date", date, "id", h.id()));
            } else if ("CANCELLED".equals(h.reason())) {
                MessageUtil.send(player, "history-line-cancelled", Map.of("date", date, "id", h.id()));
            }
        }
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
