package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the /ac sell command.
 * Usage: /ac sell &lt;price&gt; [buyout] [duration]
 */
public class SellSubCommand implements SubCommand {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public SellSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // Check economy availability
        if (!plugin.getEconomyManager().isAvailable()) {
            MessageUtil.send(player, "economy-not-found", null);
            return;
        }

        // Require at least a price argument
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "§cUsage: §f/ac sell <price> [buyout] [duration]");
            return;
        }

        // Parse start price
        double startPrice;
        try {
            startPrice = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        // Parse optional buyout price (default: 0 = no buyout)
        double buyoutPrice = 0;
        if (args.length >= 3) {
            try {
                buyoutPrice = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                MessageUtil.sendRaw(player, "§cInvalid buyout price.");
                return;
            }
        }

        // Parse optional duration (default from config)
        int duration = plugin.getConfig().getInt("general.default-duration", 300);
        if (args.length >= 4) {
            try {
                duration = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                MessageUtil.sendRaw(player, "§cInvalid duration. Use seconds (e.g., 300 for 5 minutes).");
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

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
