package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionHistory;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
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
    // FIX-5: SimpleDateFormat is not thread-safe — use ThreadLocal
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("MM/dd HH:mm"));

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

        // FIX H-3: Jalankan query history di async thread untuk mencegah blocking main
        // thread
        final UUID finalTargetUUID = targetUUID;
        final String finalTargetName = targetName;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<AuctionHistory> history = plugin.getAuctionManager().getHistory(finalTargetUUID, 10);

            // Kembali ke main thread untuk kirim message ke player
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (history.isEmpty()) {
                    if (finalTargetUUID.equals(player.getUniqueId())) {
                        MessageUtil.send(player, "history-empty", null);
                    } else {
                        MessageUtil.send(player, "history-empty-other", Map.of("player", finalTargetName));
                    }
                    return;
                }

                if (finalTargetUUID.equals(player.getUniqueId())) {
                    MessageUtil.send(player, "history-header", Map.of("count", "10"));
                } else {
                    MessageUtil.send(player, "history-header-other", Map.of("count", "10", "player", finalTargetName));
                }

                for (AuctionHistory h : history) {
                    // FIX-5: Use ThreadLocal.get() to access SimpleDateFormat
                    String date = DATE_FORMAT.get().format(new Date(h.resolvedAt()));

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
            });
        });
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
