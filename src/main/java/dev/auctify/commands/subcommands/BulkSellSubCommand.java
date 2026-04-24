package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * /ac bulksell — Sell multiple stacks of items at once.
 * Usage: /ac bulksell <start_price> <buyout_price> [duration_minutes]
 * Sells all items of the same type as the held item from inventory.
 */
public class BulkSellSubCommand implements SubCommand {

    private final Auctify plugin;

    public BulkSellSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (!player.hasPermission("auctify.bulksell")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length < 3) {
            MessageUtil.send(player, "bulksell-usage", null);
            return;
        }

        // Parse prices
        double startPrice, buyoutPrice;
        try {
            startPrice = Double.parseDouble(args[1]);
            buyoutPrice = Double.parseDouble(args[2]);
            if (startPrice <= 0 || Double.isNaN(startPrice) || Double.isInfinite(startPrice)) {
                MessageUtil.send(player, "invalid-price", null);
                return;
            }
            if (buyoutPrice < 0 || Double.isNaN(buyoutPrice) || Double.isInfinite(buyoutPrice)) {
                MessageUtil.send(player, "invalid-price", null);
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        // Parse duration (default 24 hours = 1440 minutes)
        int durationMinutes = 1440;
        if (args.length >= 4) {
            try {
                durationMinutes = Integer.parseInt(args[3]);
                if (durationMinutes <= 0 || durationMinutes > 10080) { // Max 7 days
                    MessageUtil.send(player, "invalid-duration", null);
                    return;
                }
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "invalid-duration", null);
                return;
            }
        }

        // Get held item
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            MessageUtil.send(player, "sell-no-item", null);
            return;
        }

        // Find all matching items in inventory
        int totalAmount = 0;
        int stacksListed = 0;
        int maxBulkListings = plugin.getConfig().getInt("bulksell.max-stacks", 9);

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir())
                continue;

            // Match by type and similar metadata
            if (item.getType() == hand.getType() &&
                    item.getEnchantments().equals(hand.getEnchantments())) {

                if (stacksListed >= maxBulkListings) {
                    MessageUtil.send(player, "bulksell-max-reached", Map.of("max", String.valueOf(maxBulkListings)));
                    break;
                }

                // Calculate per-stack price
                int amount = item.getAmount();
                double stackStartPrice = (startPrice / hand.getAmount()) * amount;
                double stackBuyoutPrice = buyoutPrice > 0 ? (buyoutPrice / hand.getAmount()) * amount : 0;

                // Create listing
                String listingId = plugin.getAuctionManager().createListing(
                        player, item, stackStartPrice, stackBuyoutPrice, durationMinutes * 60);

                if (listingId != null) {
                    totalAmount += amount;
                    stacksListed++;
                    // Remove item from inventory
                    player.getInventory().setItem(i, null);
                }
            }
        }

        if (stacksListed > 0) {
            MessageUtil.send(player, "bulksell-success", Map.of(
                    "stacks", String.valueOf(stacksListed),
                    "amount", String.valueOf(totalAmount),
                    "item", hand.getType().name()));
            plugin.getSoundManager().playSuccess(player);
        } else {
            MessageUtil.send(player, "bulksell-no-items", null);
        }
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
