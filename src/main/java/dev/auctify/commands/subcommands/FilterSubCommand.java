package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.gui.AuctifyHolder;
import dev.auctify.gui.GUIManager;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

/**
 * Handles the /ac filter command for managing advanced search filters.
 * Usage: /ac filter <type> <value> or /ac filter clear
 */
public class FilterSubCommand implements SubCommand {

    private final Auctify plugin;

    public FilterSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.filter")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "filter-usage", null);
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("clear")) {
            clearFilters(player);
            return;
        }

        if (args.length < 3) {
            MessageUtil.send(player, "filter-usage", null);
            return;
        }

        String value = args[2];

        switch (action) {
            case "minprice" -> setMinPrice(player, value);
            case "maxprice" -> setMaxPrice(player, value);
            case "seller" -> setSellerName(player, value);
            case "endtime" -> setMaxEndTime(player, value);
            case "sort" -> setSortMode(player, value);
            default -> MessageUtil.send(player, "filter-invalid-type", Map.of("type", action));
        }
    }

    private void setMinPrice(Player player, String value) {
        try {
            double minPrice = Double.parseDouble(value);
            if (minPrice < 0) {
                MessageUtil.send(player, "filter-invalid-price", null);
                return;
            }
            updateFilter(player, holder -> holder.setMinPrice(minPrice));
            MessageUtil.send(player, "filter-minprice-set", Map.of("price", String.format("%.2f", minPrice)));
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "filter-invalid-price", null);
        }
    }

    private void setMaxPrice(Player player, String value) {
        try {
            double maxPrice = Double.parseDouble(value);
            if (maxPrice < 0) {
                MessageUtil.send(player, "filter-invalid-price", null);
                return;
            }
            updateFilter(player, holder -> holder.setMaxPrice(maxPrice));
            MessageUtil.send(player, "filter-maxprice-set", Map.of("price", String.format("%.2f", maxPrice)));
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "filter-invalid-price", null);
        }
    }

    private void setSellerName(Player player, String value) {
        updateFilter(player, holder -> holder.setSellerName(value));
        MessageUtil.send(player, "filter-seller-set", Map.of("seller", value));
    }

    private void setMaxEndTime(Player player, String value) {
        try {
            long maxEndTime = System.currentTimeMillis() + (Long.parseLong(value) * 1000L);
            updateFilter(player, holder -> holder.setMaxEndTime(maxEndTime));
            MessageUtil.send(player, "filter-endtime-set", Map.of("seconds", value));
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "filter-invalid-time", null);
        }
    }

    private void setSortMode(Player player, String value) {
        String sortMode = value.toUpperCase();
        if (!isValidSortMode(sortMode)) {
            MessageUtil.send(player, "filter-invalid-sort", null);
            return;
        }
        updateFilter(player, holder -> holder.setSortMode(sortMode));
        MessageUtil.send(player, "filter-sort-set", Map.of("mode", sortMode));
    }

    private void clearFilters(Player player) {
        updateFilter(player, holder -> {
            holder.setMinPrice(null);
            holder.setMaxPrice(null);
            holder.setSellerName(null);
            holder.setMaxEndTime(null);
            holder.setSortMode("TIME_ASC");
        });
        MessageUtil.send(player, "filter-cleared", null);
    }

    private void updateFilter(Player player, FilterUpdater updater) {
        GUIManager guiManager = plugin.getGUIManager();
        if (!guiManager.getOpenGUI(player).map("MAIN"::equals).orElse(false)) {
            MessageUtil.send(player, "filter-not-in-auction", null);
            return;
        }

        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv == null || !(inv.getHolder() instanceof AuctifyHolder holder)) {
            return;
        }

        updater.update(holder);
        plugin.getAuctionGUI().open(player, holder.getPage(), holder.getCategory(),
                holder.getSortMode(), holder.getQuery(), holder.getMinPrice(),
                holder.getMaxPrice(), holder.getSellerName(), holder.getMaxEndTime());
    }

    private boolean isValidSortMode(String sortMode) {
        return switch (sortMode) {
            case "TIME_ASC", "TIME_DESC", "PRICE_ASC", "PRICE_DESC", "BIDS", "NEWEST", "ENDING_SOON" -> true;
            default -> false;
        };
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @FunctionalInterface
    private interface FilterUpdater {
        void update(AuctifyHolder holder);
    }
}
