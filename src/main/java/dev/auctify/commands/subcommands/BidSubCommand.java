package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /ac bid command for direct CLI bidding.
 * Usage: /ac bid &lt;listing_id&gt; &lt;amount&gt;
 */
public class BidSubCommand implements SubCommand {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public BidSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.bid")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (!plugin.getEconomyManager().isAvailable()) {
            MessageUtil.send(player, "economy-not-found", null);
            return;
        }

        if (args.length < 3) {
            MessageUtil.send(player, "bid-usage", null);
            return;
        }

        String listingId = args[1];

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        plugin.getAuctionManager().placeBid(player, listingId, amount);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
