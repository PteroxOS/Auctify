package dev.auctify.commands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completion handler for the /ac command.
 * Provides context-aware suggestions for subcommands and their arguments.
 */
public class TabCompleter implements org.bukkit.command.TabCompleter {

    private final Auctify plugin;

    /** All available subcommand names. */
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "sell", "bid", "open", "cancel", "search", "history", "about",
            "claim", "admin", "setup", "reload", "help");

    /** @param plugin the main plugin instance */
    public TabCompleter(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Provides tab completion suggestions based on the current input.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // First arg: suggest subcommands that match the partial input
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "sell" -> List.of("<price>");
                case "bid" -> {
                    // Only suggest listing IDs the player can actually bid on (not their own, not
                    // expired)
                    if (sender instanceof Player player) {
                        yield plugin.getAuctionManager().getActiveListings().stream()
                                .filter(l -> !l.getSellerUUID().equals(player.getUniqueId()) && l.isActive()
                                        && !l.isExpired())
                                .map(AuctionListing::getId)
                                .filter(id -> id.startsWith(args[1]))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "cancel" -> {
                    // Only suggest listing IDs the player owns (or all if admin)
                    if (sender instanceof Player player) {
                        boolean isAdmin = player.hasPermission("auctify.admin");
                        yield plugin.getAuctionManager().getActiveListings().stream()
                                .filter(l -> l.isActive()
                                        && (isAdmin || l.getSellerUUID().equals(player.getUniqueId())))
                                .map(AuctionListing::getId)
                                .filter(id -> id.startsWith(args[1]))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "search" -> List.of("<query>");
                case "admin" -> List.of("blacklist", "cancel", "backup");
                default -> List.of();
            };
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "sell" -> List.of("[buyout]");
                case "bid" -> List.of("<amount>");
                default -> List.of();
            };
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("sell")) {
            return List.of("[duration]");
        }

        return List.of();
    }
}
