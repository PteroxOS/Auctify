package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * /ac admin — Opens the admin moderation panel.
 * Subcommands:
 *   /ac admin                — Open admin GUI
 *   /ac admin blacklist add <player> [reason]
 *   /ac admin blacklist remove <player>
 *   /ac admin blacklist list
 */
public class AdminSubCommand implements SubCommand {

    private final Auctify plugin;

    public AdminSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // /ac admin — open GUI
            if (sender instanceof Player player) {
                plugin.getAdminGUI().open(player, 0);
            } else {
                MessageUtil.send(sender, "player-only", null);
            }
            return;
        }

        if (args[1].equalsIgnoreCase("blacklist") || args[1].equalsIgnoreCase("bl")) {
            handleBlacklist(sender, args);
            return;
        }

        if (args[1].equalsIgnoreCase("cancel") && args.length >= 3) {
            String listingId = args[2];
            // Admin force cancel
            if (sender instanceof Player player) {
                plugin.getAuctionManager().cancelListing(player, listingId);
            } else {
                MessageUtil.sendRaw(sender, "§cUse in-game admin GUI for force cancel.");
            }
            return;
        }

        MessageUtil.sendRaw(sender, "§6Admin Commands:");
        MessageUtil.sendRaw(sender, "§e/ac admin §8— §7Open admin GUI");
        MessageUtil.sendRaw(sender, "§e/ac admin blacklist add <player> [reason] §8— §7Blacklist player");
        MessageUtil.sendRaw(sender, "§e/ac admin blacklist remove <player> §8— §7Unblacklist player");
        MessageUtil.sendRaw(sender, "§e/ac admin blacklist list §8— §7Show blacklisted players");
        MessageUtil.sendRaw(sender, "§e/ac admin cancel <id> §8— §7Force cancel a listing");
    }

    private void handleBlacklist(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendRaw(sender, "§cUsage: /ac admin blacklist <add|remove|list>");
            return;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    MessageUtil.sendRaw(sender, "§cUsage: /ac admin blacklist add <player> [reason]");
                    return;
                }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    MessageUtil.sendRaw(sender, "§cPlayer not found or offline.");
                    return;
                }
                String reason = args.length >= 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "No reason";
                plugin.getStorageManager().addBlacklist(target.getUniqueId(), reason, sender.getName());
                MessageUtil.send(sender, "admin-blacklist-added", Map.of("player", target.getName(), "reason", reason));
            }
            case "remove" -> {
                if (args.length < 4) {
                    MessageUtil.sendRaw(sender, "§cUsage: /ac admin blacklist remove <player>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    MessageUtil.sendRaw(sender, "§cPlayer not found or offline.");
                    return;
                }
                plugin.getStorageManager().removeBlacklist(target.getUniqueId());
                MessageUtil.send(sender, "admin-blacklist-removed", Map.of("player", target.getName()));
            }
            case "list" -> {
                var list = plugin.getStorageManager().getBlacklist();
                if (list.isEmpty()) {
                    MessageUtil.sendRaw(sender, "§7No players are blacklisted.");
                    return;
                }
                MessageUtil.sendRaw(sender, "§6§lBlacklisted Players:");
                for (String[] entry : list) {
                    String name = entry[0]; // UUID
                    String r = entry[1] != null ? entry[1] : "No reason";
                    String by = entry[2];
                    MessageUtil.sendRaw(sender, "§7- §f" + name + " §8(by " + by + ") §7Reason: §f" + r);
                }
            }
            default -> MessageUtil.sendRaw(sender, "§cUsage: /ac admin blacklist <add|remove|list>");
        }
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

    public String getPermission() {
        return "auctify.admin";
    }

    public String getUsage() {
        return "/ac admin [blacklist|cancel]";
    }

    public String getDescription() {
        return "Open the admin moderation panel.";
    }
}
