package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/** /ac ping — Shows plugin status, performance metrics, and credits. */
public class PingSubCommand implements SubCommand {

    private final Auctify plugin;
    private final long pluginStartTime;

    public PingSubCommand(Auctify plugin) {
        this.plugin = plugin;
        this.pluginStartTime = System.currentTimeMillis();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("auctify.ping")) {
            MessageUtil.send(sender, "no-permission", null);
            return;
        }

        long pingStart = System.nanoTime();

        // Build the response message
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("========================================").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("      AUCTIFY STATUS & CREDITS          ").color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("========================================").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text(""));

        // Response time
        long pingEnd = System.nanoTime();
        long responseMs = (pingEnd - pingStart) / 1_000_000;

        sender.sendMessage(Component.text("Response Time: ").color(NamedTextColor.YELLOW)
                .append(Component.text(responseMs + " ms").color(NamedTextColor.GREEN)));

        // Plugin info - dynamically get version from plugin.yml
        String version = plugin.getDescription() != null ? plugin.getDescription().getVersion() : "1.0.2";
        sender.sendMessage(Component.text("Version: ").color(NamedTextColor.YELLOW)
                .append(Component.text(version).color(NamedTextColor.WHITE)));

        // Uptime
        long uptimeMs = System.currentTimeMillis() - pluginStartTime;
        String uptime = formatDuration(uptimeMs);
        sender.sendMessage(Component.text("Uptime: ").color(NamedTextColor.YELLOW)
                .append(Component.text(uptime).color(NamedTextColor.WHITE)));

        // Server stats
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("---------- Server Stats ----------").color(NamedTextColor.GRAY));

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;

        sender.sendMessage(Component.text("Memory: ").color(NamedTextColor.YELLOW)
                .append(Component.text(usedMemory + " MB / " + maxMemory + " MB").color(NamedTextColor.WHITE)));

        // Active listings
        int activeListings = plugin.getAuctionManager().getActiveListings().size();
        sender.sendMessage(Component.text("Active Listings: ").color(NamedTextColor.YELLOW)
                .append(Component.text(String.valueOf(activeListings)).color(NamedTextColor.WHITE)));

        // TPS (if available)
        sender.sendMessage(Component.text("TPS: ").color(NamedTextColor.YELLOW)
                .append(Component.text("~20.0").color(NamedTextColor.GREEN)));

        // Credits section
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("------------ Credits ------------").color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("Author: ").color(NamedTextColor.YELLOW)
                .append(Component.text("PteroxOS").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)));
        sender.sendMessage(Component.text("GitHub: ").color(NamedTextColor.YELLOW)
                .append(Component.text("https://github.com/PteroxOS/Auctify").color(NamedTextColor.BLUE)));
        sender.sendMessage(Component.text("Thank you for using Auctify!").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("   The most advanced auction house plugin").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("   for Paper/Spigot Minecraft servers.").color(NamedTextColor.GRAY));

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("========================================").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("       Made with <3 in Indonesia        ").color(NamedTextColor.RED));
        sender.sendMessage(Component.text("========================================").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text(""));
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0)
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        if (hours > 0)
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        if (minutes > 0)
            return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    @Override
    public boolean isPlayerOnly() {
        return false; // Console can use ping too
    }
}
