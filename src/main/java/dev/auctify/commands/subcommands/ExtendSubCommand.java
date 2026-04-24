package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /ac extend command.
 * Usage: /ac extend <id> <minutes>
 * Extends an auction if it has no bids.
 */
public class ExtendSubCommand implements SubCommand {

    private final Auctify plugin;

    public ExtendSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("auctify.sell")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length < 3) {
            MessageUtil.sendRaw(player, "§cUsage: §f/ac extend <listing_id> <minutes>");
            return;
        }

        String listingId = args[1].toUpperCase();
        int minutes;
        try {
            minutes = Integer.parseInt(args[2]);
            if (minutes <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.sendRaw(player, "§cMinutes must be a positive number.");
            return;
        }

        plugin.getAuctionManager().extendAuction(player, listingId, minutes);
    }
}
