package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Handles the /ac bulkcancel command. Usage: /ac bulkcancel. Cancels all active
 * listings owned by the player (only those without bids).
 */
public class BulkCancelSubCommand implements SubCommand {

    private final Auctify plugin;

    public BulkCancelSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("auctify.sell")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        int count = plugin.getAuctionManager().bulkCancelAuctions(player);

        if (count > 0) {
            MessageUtil.send(player, "bulk-cancel-success", Map.of("count", String.valueOf(count)));
        } else {
            MessageUtil.send(player, "bulk-cancel-none", null);
        }
    }
}
