package dev.auctify.commands;

import dev.auctify.Auctify;
import dev.auctify.auction.AuctionListing;
import dev.auctify.auction.BuyOrder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completion handler for the /ac command. Provides context-aware
 * suggestions for subcommands and their arguments.
 */
public class TabCompleter implements org.bukkit.command.TabCompleter {

    private final Auctify plugin;

    /** All available subcommand names. */
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "sell", "bid", "open", "cancel", "search", "history", "about",
            "claim", "admin", "reload",
            "bidhistory", "extend", "bulkcancel", "watchlist", "ping",
            "buyorder", "stats", "bulksell", "filter", "notifications", "autobid", "pricehistory",
            "theme", "template", "rating", "trade", "bulkbuy");

    /** Constructor. */
    public TabCompleter(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Provides tab completion suggestions based on the current input. */
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
                    // C-3 CRITICAL FIX: Only suggest listing IDs if player has permission and not
                    // blacklisted
                    if (sender instanceof Player player) {
                        // Check permission
                        if (!player.hasPermission("auctify.bid")) {
                            yield List.of();
                        }
                        // Check blacklist
                        if (plugin.getStorageManager().isBlacklisted(player.getUniqueId())) {
                            yield List.of();
                        }
                        yield plugin.getAuctionManager().getActiveListings().stream()
                                .filter(l -> !l.getSellerUUID().equals(player.getUniqueId()) && l.isActive()
                                        && !l.isExpired() && !l.isBinOnly()) // C-3: Jangan suggest BIN-only
                                .map(AuctionListing::getId)
                                .filter(id -> id.startsWith(args[1]))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "cancel", "extend", "bidhistory" -> {
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
                case "history" -> {
                    // Suggest online player names for admin history lookup
                    if (sender instanceof Player player && player.hasPermission("auctify.admin.history")) {
                        yield plugin.getServer().getOnlinePlayers().stream()
                                .map(p -> p.getName())
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "admin" -> List.of("blacklist", "cancel", "backup");
                case "watchlist" -> {
                    // Suggest active listing IDs for watchlist toggle
                    if (sender instanceof Player player) {
                        yield plugin.getAuctionManager().getActiveListings().stream()
                                .filter(l -> l.isActive() && !l.isExpired())
                                .filter(l -> !l.getSellerUUID().equals(player.getUniqueId()))
                                .map(AuctionListing::getId)
                                .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "buyorder" -> List.of("create", "list", "cancel", "browse", "sell");
                case "stats" -> {
                    // Suggest online player names
                    if (sender instanceof Player player && player.hasPermission("auctify.admin.stats")) {
                        yield plugin.getServer().getOnlinePlayers().stream()
                                .map(p -> p.getName())
                                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "bulksell" -> List.of("<start_price>");
                case "filter" -> List.of("minprice", "maxprice", "seller", "endtime", "sort", "clear");
                case "notifications" -> List.of("toggle", "all");
                case "autobid" -> List.of("set", "remove", "clear");
                case "pricehistory" -> List.of("[item_type]");
                case "theme" -> List.of("list", "set", "reset");
                case "template" -> List.of("list", "create", "delete", "use");
                case "rating" -> List.of("[player]");
                case "trade" -> List.of("send", "accept", "decline", "cancel");
                case "bulkbuy" -> List.of("add", "remove", "list", "clear", "buy");
                default -> List.of();
            };
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "sell" -> List.of("[buyout]");
                case "bid" -> List.of("<amount>");
                case "extend" -> List.of("<minutes>");
                case "theme" -> {
                    // Suggest available theme names from config
                    var themesSection = plugin.getConfig().getConfigurationSection("gui.themes");
                    if (themesSection != null) {
                        yield themesSection.getKeys(false).stream()
                                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "template" -> {
                    String action = args[1].toLowerCase();
                    if (action.equals("delete") || action.equals("use")) {
                        // Suggest template names the player owns
                        if (sender instanceof Player player) {
                            var templates = plugin.getTemplateManager().getTemplates(player.getUniqueId());
                            if (templates != null) {
                                yield templates.values().stream()
                                        .map(t -> t.getName())
                                        .filter(name -> name != null
                                                && name.toLowerCase().startsWith(args[2].toLowerCase()))
                                        .collect(Collectors.toList());
                            }
                        }
                    }
                    yield List.of("<template_name>");
                }
                case "trade" -> {
                    String action = args[1].toLowerCase();
                    if (action.equals("send")) {
                        // Suggest online player names
                        yield plugin.getServer().getOnlinePlayers().stream()
                                .filter(p -> !p.getName().equals(sender.getName())) // Don't suggest self
                                .map(p -> p.getName())
                                .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                    yield List.of();
                }
                case "bulkbuy" -> {
                    String action = args[1].toLowerCase();
                    if (action.equals("add") || action.equals("remove")) {
                        // Suggest active listing IDs
                        if (sender instanceof Player player) {
                            yield plugin.getAuctionManager().getActiveListings().stream()
                                    .filter(l -> l.isActive() && !l.isExpired())
                                    .filter(l -> !l.getSellerUUID().equals(player.getUniqueId())) // Don't suggest own
                                                                                                  // listings
                                    .map(AuctionListing::getId)
                                    .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                                    .collect(Collectors.toList());
                        }
                    }
                    yield List.of();
                }
                case "buyorder" -> {
                    yield switch (args[1].toLowerCase()) {
                        case "create", "c" -> List.of("<price>");
                        case "cancel", "remove" -> {
                            // Suggest order IDs the player owns
                            if (sender instanceof Player player && args.length > 2) {
                                var orders = plugin.getBuyOrderManager().getOrdersByBuyer(player.getUniqueId());
                                if (orders != null) {
                                    yield orders.stream()
                                            .map(BuyOrder::getId)
                                            .filter(id -> id != null && id.startsWith(args[2]))
                                            .collect(Collectors.toList());
                                }
                            }
                            yield List.of();
                        }
                        case "sell", "s", "fill", "f" -> {
                            // Suggest all active buy order IDs
                            if (sender instanceof Player && args.length > 2) {
                                var orders = plugin.getBuyOrderManager().getActiveOrders();
                                if (orders != null) {
                                    yield orders.stream()
                                            .map(BuyOrder::getId)
                                            .filter(id -> id != null && id.startsWith(args[2]))
                                            .collect(Collectors.toList());
                                }
                            }
                            yield List.of();
                        }
                        default -> List.of();
                    };
                }
                case "bulksell" -> List.of("<buyout_price>");
                default -> List.of();
            };
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            return switch (sub) {
                case "sell" -> List.of("[duration]");
                case "buyorder" -> {
                    // For buyorder create, suggest amount
                    if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("c")) {
                        yield List.of("[amount]");
                    }
                    yield List.of();
                }
                case "bulksell" -> List.of("[duration_minutes]");
                case "filter" -> {
                    String type = args[1].toLowerCase();
                    if (type.equals("sort")) {
                        yield List.of("TIME_ASC", "TIME_DESC", "PRICE_ASC", "PRICE_DESC", "BIDS", "NEWEST",
                                "ENDING_SOON");
                    }
                    yield List.of("[value]");
                }
                case "notifications" -> {
                    String action = args[1].toLowerCase();
                    if (action.equals("toggle")) {
                        yield List.of("outbid", "buyout", "auction-won", "item-sold", "expiration");
                    } else if (action.equals("all")) {
                        yield List.of("on", "off");
                    }
                    yield List.of();
                }
                case "autobid" -> {
                    String action = args[1].toLowerCase();
                    if (action.equals("set")) {
                        yield List.of("[max_amount]");
                    }
                    yield List.of();
                }
                default -> List.of();
            };
        }

        return List.of();
    }
}
