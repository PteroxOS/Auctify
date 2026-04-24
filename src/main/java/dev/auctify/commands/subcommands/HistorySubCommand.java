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

        List<AuctionHistory> history = plugin.getAuctionManager().getHistory(player.getUniqueId(), 10);

        if (history.isEmpty()) {
            MessageUtil.send(player, "history-empty", null);
            return;
        }

        MessageUtil.send(player, "history-header", Map.of("count", "10"));

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

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
