package dev.auctify.commands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
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
            "sell", "bid", "open", "cancel", "search", "history", "about", "reload", "help"
    );

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
                case "bid", "cancel" -> {
                    // Suggest active listing IDs
                    yield plugin.getAuctionManager().getActiveListings().stream()
                            .map(AuctionListing::getId)
                            .filter(id -> id.startsWith(args[1]))
                            .collect(Collectors.toList());
                }
                case "search" -> List.of("<query>");
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
