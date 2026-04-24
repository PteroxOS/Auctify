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
 * Handles the /ac search command.
 * Usage: /ac search &lt;query&gt;
 * Searches by item name or seller name (case-insensitive).
 */
public class SearchSubCommand implements SubCommand {

    private final Auctify plugin;

    /** Per-player cooldowns to prevent search spam / DoS. */
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1500;

    /** @param plugin the main plugin instance */
    public SearchSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    /** {@inheritDoc} */
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
            MessageUtil.sendRaw(player, "§cPlease wait before searching again.");
            return;
        }
        cooldowns.put(uuid, now);

        if (args.length < 2) {
            MessageUtil.sendRaw(player, "§cUsage: §f/ac search <query>");
            return;
        }

        // Join all args after "search" into the query
        String query = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        List<AuctionListing> results = plugin.getAuctionManager().searchListings(query);

        if (results.isEmpty()) {
            MessageUtil.sendRaw(player, "§7No listings found matching '§f" + query + "§7'.");
            return;
        }

        MessageUtil.sendRaw(player, "§6Search results for '§f" + query + "§6' (" + results.size() + " found):");
        for (AuctionListing listing : results) {
            String itemName = ItemUtil.getDisplayName(listing.getItem());
            MessageUtil.sendRaw(player, "§8 • §f" + itemName + " §7by §e" + listing.getSellerName()
                    + " §7— §aID: §f" + listing.getId() + " §7— §a"
                    + plugin.getEconomyManager().format(listing.getCurrentBid()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
