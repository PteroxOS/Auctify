package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.gui.PriceHistoryGUI;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /ac pricehistory command for viewing price trends.
 */
public class PriceHistorySubCommand implements SubCommand {

    private final Auctify plugin;

    public PriceHistorySubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.pricehistory")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length == 1) {
            // Show all recent price history
            PriceHistoryGUI.openAllHistory(plugin, player);
        } else {
            // Show price history for specific item type
            String itemType = args[1];
            // Validate item type to prevent potential issues
            if (itemType == null || itemType.isEmpty() || itemType.length() > 64) {
                MessageUtil.send(player, "pricehistory-usage", null);
                return;
            }
            PriceHistoryGUI.openPriceHistory(plugin, player, itemType);
        }
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
