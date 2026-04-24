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
        String discordWebhook = plugin.getConfig().getString("discord.webhook-url", "");
        boolean discordEnabled = !discordWebhook.isEmpty() && plugin.getConfig().getBoolean("discord.enabled", false);

        // Simplified about card with aligned borders
        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "§8╔══════════════════════════════════════════════════╗");
        MessageUtil.sendRaw(sender, "§8║                                                  ║");
        MessageUtil.sendRaw(sender, "§8║         §6✦ §e§lA U C T I F Y §6✦ §fv" + version + "        §8║");
        MessageUtil.sendRaw(sender, "§8║                                                  ║");
        MessageUtil.sendRaw(sender, "§8╠══════════════════════════════════════════════════╣");
        MessageUtil.sendRaw(sender, "§8║ §6§lServer Status                               §8║");
        MessageUtil.sendRaw(sender, "§8║                                                  ║");
        MessageUtil.sendRaw(sender,
                "§8║  §7Economy:§8      " + formatStatus(economyStatus, "Connected", "Offline") + "            §8║");
        MessageUtil.sendRaw(sender, "§8║  §7Storage:§8       §f" + padRight(storageType, 28) + "§8║");
        MessageUtil.sendRaw(sender,
                "§8║  §7Discord:§8      " + formatStatus(discordEnabled, "Connected", "Disabled") + "            §8║");
        MessageUtil.sendRaw(sender, "§8║  §7Listings:§8      §a" + padRight(activeListings + " active", 28) + "§8║");
        MessageUtil.sendRaw(sender,
                "§8║  §7Players:§8       §f" + padRight(Bukkit.getOnlinePlayers().size() + " online", 28) + "§8║");
        MessageUtil.sendRaw(sender, "§8║                                                  ║");
        MessageUtil.sendRaw(sender, "§8╠══════════════════════════════════════════════════╣");
        MessageUtil.sendRaw(sender, "§8║ §6§lLinks                                        §8║");
        MessageUtil.sendRaw(sender, "§8║                                                  ║");
        MessageUtil.sendRaw(sender, "§8║  §7GitHub: §fgithub.com/PteroxOS/Auctify          §8║");
        MessageUtil.sendRaw(sender, "§8║                                                  ║");
        MessageUtil.sendRaw(sender, "§8╚══════════════════════════════════════════════════╝");
        MessageUtil.sendRaw(sender, "");
    }

    private String formatStatus(boolean status, String onText, String offText) {
        return status ? "§a✔ " + onText : "§c✗ " + offText;
    }

    /**
     * Pads a string to a fixed width for alignment in the about card.
     *
     * @param text  the text to pad
     * @param width the desired width
     * @return the padded string
     */
    private String padRight(String text, int width) {
        if (text.length() >= width)
            return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPlayerOnly() {
        return false; // Console can view about too
    }
}
