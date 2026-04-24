package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.util.ItemUtil;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the /ac search command. Usage: /ac search <query>. Searches by item
 * name or seller name (case-insensitive).
 */
public class SearchSubCommand implements SubCommand {

    private final Auctify plugin;

    /** Per-player cooldowns to prevent search spam / DoS. */
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1500;

    /** Constructor. */
    public SearchSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!player.hasPermission("auctify.search")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        // LOW-1: Rate limit search to prevent DoS on servers with thousands of listings
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && now - last < COOLDOWN_MS) {
            MessageUtil.send(player, "search-cooldown", null);
            return;
        }
        cooldowns.put(uuid, now);

        if (args.length < 2) {
            MessageUtil.send(player, "search-usage", null);
            return;
        }

        // Join all args after "search" into the query
        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        List<AuctionListing> results = plugin.getAuctionManager().searchListings(query);

        if (results.isEmpty()) {
            MessageUtil.send(player, "search-no-results", Map.of("query", query));
            return;
        }

        MessageUtil.send(player, "search-header", Map.of("query", query, "count", String.valueOf(results.size())));
        for (AuctionListing listing : results) {
            String itemName = ItemUtil.getDisplayName(listing.getItem());
            MessageUtil.send(player, "search-result-item", Map.of(
                    "item", itemName,
                    "seller", listing.getSellerName(),
                    "id", listing.getId(),
                    "bid", plugin.getEconomyManager().format(listing.getCurrentBid())));
        }
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
