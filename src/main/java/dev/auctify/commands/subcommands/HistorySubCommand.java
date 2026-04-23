package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionHistory;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Handles the /ac history command.
 * Shows the player's recent auction history (as seller or winner).
 */
public class HistorySubCommand implements SubCommand {

    private final Auctify plugin;

    /** Date formatter for displaying resolved timestamps. */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");

    /** @param plugin the main plugin instance */
    public HistorySubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("auctify.history")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        // TODO: Add /ac history <player> for admin lookup

        List<AuctionHistory> history = plugin.getAuctionManager().getHistory(player.getUniqueId(), 10);

        if (history.isEmpty()) {
            MessageUtil.sendRaw(player, "§7You have no auction history yet.");
            return;
        }

        MessageUtil.sendRaw(player, "§6§lYour Auction History §8(last 10)");
        MessageUtil.sendRaw(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (AuctionHistory h : history) {
            String date = DATE_FORMAT.format(new Date(h.resolvedAt()));
            String status = switch (h.reason()) {
                case "SOLD" -> "§a✔ SOLD";
                case "EXPIRED" -> "§c✘ EXPIRED";
                case "CANCELLED" -> "§e⚠ CANCELLED";
                default -> "§7" + h.reason();
            };

            StringBuilder line = new StringBuilder();
            line.append("§8[§7").append(date).append("§8] ");
            line.append(status).append(" §8| ");
            line.append("§7ID: §f").append(h.id()).append(" ");

            if ("SOLD".equals(h.reason())) {
                line.append("§8» §a").append(plugin.getEconomyManager().format(h.finalPrice()));
                if (h.taxAmount() > 0) {
                    line.append(" §8(§ctax: ").append(plugin.getEconomyManager().format(h.taxAmount())).append("§8)");
                }
            }

            MessageUtil.sendRaw(player, line.toString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
