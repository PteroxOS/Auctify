package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Handles the /ac about command.
 * Shows detailed plugin information, credits, and server stats.
 */
public class AboutSubCommand implements SubCommand {

    private final Auctify plugin;

    /** @param plugin the main plugin instance */
    public AboutSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(CommandSender sender, String[] args) {
        String version = plugin.getPluginMeta().getVersion();
        int activeListings = plugin.getAuctionManager().getActiveListings().size();
        boolean economyStatus = plugin.getEconomyManager().isAvailable();
        String storageType = plugin.getConfig().getString("storage.type", "sqlite").toUpperCase();
        String serverVersion = Bukkit.getVersion();

        // Premium-styled about card
        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "§8┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┃    §6✦ §e§lA U C T I F Y §6✦          §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7Version§8:      §f" + padRight("v" + version, 18) + "§8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7Author§8:       §e" + padRight("Jephyruu", 18) + "§8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7API§8:          §f" + padRight("Paper 1.18+", 18) + "§8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7License§8:      §f" + padRight("MIT", 18) + "§8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫");
        MessageUtil.sendRaw(sender, "§8┃  §6§lServer Status                    §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7Economy§8:      " + (economyStatus ? "§a✔ Connected   " : "§c✗ Unavailable ") + "     §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7Storage§8:      §f" + padRight(storageType, 18) + "§8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7Listings§8:     §a" + padRight(String.valueOf(activeListings) + " active", 18) + "§8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7Players§8:      §f" + padRight(Bukkit.getOnlinePlayers().size() + " online", 18) + "§8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫");
        MessageUtil.sendRaw(sender, "§8┃  §6§lDescription                      §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7A professional live auction house  §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7plugin with real-time bidding,     §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7buyouts, tax system, and a        §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7beautiful chest GUI interface.     §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫");
        MessageUtil.sendRaw(sender, "§8┃  §6§lFeatures                         §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Real-time bidding system       §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Instant buyout option          §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Configurable seller tax        §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Auto-refresh chest GUI         §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7SQLite & MySQL support         §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Offline item delivery          §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Auction history tracking       §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Full Vault integration         §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Hot-reloadable config          §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §e★ §7Public API for developers      §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫");
        MessageUtil.sendRaw(sender, "§8┃  §6§lLinks                            §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7GitHub§8: §fgithub.com/pteroxos    §8┃");
        MessageUtil.sendRaw(sender, "§8┃  §7Support§8: §fdiscord.gg/auctify    §8┃");
        MessageUtil.sendRaw(sender, "§8┃                                      §8┃");
        MessageUtil.sendRaw(sender, "§8┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛");
        MessageUtil.sendRaw(sender, "");
    }

    /**
     * Pads a string to a fixed width for alignment in the about card.
     *
     * @param text  the text to pad
     * @param width the desired width
     * @return the padded string
     */
    private String padRight(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return false; // Console can view about too
    }
}
