package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Handles the /ac sell command. Usage: /ac sell <price> [buyout] [duration] */
public class SellSubCommand implements SubCommand {

    private final Auctify plugin;

    /** Constructor. */
    public SellSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.sell")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        // Check economy availability
        if (!plugin.getEconomyManager().isAvailable()) {
            MessageUtil.send(player, "economy-not-found", null);
            return;
        }

        // Require at least a price argument
        if (args.length < 2) {
            MessageUtil.send(player, "sell-usage", null);
            return;
        }

        // Parse start price
        double startPrice;
        try {
            startPrice = Double.parseDouble(args[1]);
            if (startPrice <= 0 || Double.isNaN(startPrice) || Double.isInfinite(startPrice)) {
                MessageUtil.send(player, "invalid-price", null);
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        // Parse optional buyout price (default: 0 = no buyout)
        double buyoutPrice = 0;
        if (args.length >= 3) {
            try {
                buyoutPrice = Double.parseDouble(args[2]);
                if (buyoutPrice < 0 || Double.isNaN(buyoutPrice) || Double.isInfinite(buyoutPrice)) {
                    MessageUtil.send(player, "sell-invalid-buyout", null);
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "sell-invalid-buyout", null);
                return;
            }
        }

        // Parse optional duration (default from config)
        int duration = plugin.getConfig().getInt("general.default-duration", 300);
        if (args.length >= 4) {
            try {
                duration = Integer.parseInt(args[3]);
                if (duration <= 0 || duration > 10080) { // Max 7 days
                    MessageUtil.send(player, "sell-invalid-duration", null);
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "sell-invalid-duration", null);
                return;
            }
        }

        // Get item from main hand
        ItemStack item = player.getInventory().getItemInMainHand();

        // Null/air item check
        if (ItemUtil.isEmpty(item)) {
            MessageUtil.send(player, "hold-item-to-sell", null);
            return;
        }

        // Delegate to AuctionManager (handles all validation and listing creation)
        plugin.getAuctionManager().createListing(player, item, startPrice, buyoutPrice, duration);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
