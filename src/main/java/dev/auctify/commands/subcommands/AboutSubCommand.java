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
        if (!sender.hasPermission("auctify.about")) {
            MessageUtil.send(sender, "no-permission", null);
            return;
        }

        String version = plugin.getPluginMeta().getVersion();
        int activeListings = plugin.getAuctionManager().getActiveListings().size();
        boolean economyStatus = plugin.getEconomyManager().isAvailable();
        String storageType = plugin.getConfig().getString("storage.type", "sqlite").toUpperCase();
        String discordWebhook = plugin.getConfig().getString("discord.webhook-url", "");
        boolean discordEnabled = !discordWebhook.isEmpty() && plugin.getConfig().getBoolean("discord.enabled", false);

        // Clean format matching /ac ping style
        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "§8========================================");
        MessageUtil.sendRaw(sender, "§6      A U C T I F Y §fv" + version);
        MessageUtil.sendRaw(sender, "§8========================================");
        MessageUtil.sendRaw(sender, "");

        MessageUtil.sendRaw(sender, "§6§lServer Status");
        MessageUtil.sendRaw(sender, "  §7Economy:    " + formatStatus(economyStatus, "Connected", "Offline"));
        MessageUtil.sendRaw(sender, "  §7Storage:    §f" + storageType);
        MessageUtil.sendRaw(sender, "  §7Discord:    " + formatStatus(discordEnabled, "Connected", "Disabled"));
        MessageUtil.sendRaw(sender, "  §7Listings:   §a" + activeListings + " active");
        MessageUtil.sendRaw(sender, "  §7Players:    §f" + Bukkit.getOnlinePlayers().size() + " online");

        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "§6§lLinks");
        MessageUtil.sendRaw(sender, "  §7GitHub: §fhttps://github.com/PteroxOS/Auctify");

        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "§8========================================");
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
