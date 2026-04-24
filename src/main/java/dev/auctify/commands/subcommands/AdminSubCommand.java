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
 * /ac admin — Open admin GUI
 * /ac admin blacklist add <player> [reason]
 * /ac admin blacklist remove <player>
 * /ac admin blacklist list
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

        if (args[1].equalsIgnoreCase("backup")) {
            handleBackup(sender);
            return;
        }

        if (args[1].equalsIgnoreCase("cancel") && args.length >= 3) {
            String listingId = args[2];
            // Admin force cancel
            if (sender instanceof Player player) {
                plugin.getAuctionManager().cancelListing(player, listingId);
            } else {
                MessageUtil.send(sender, "admin-usage-gui", null);
            }
            return;
        }

        if (args[1].equalsIgnoreCase("npc")) {
            handleNPC(sender, args);
            return;
        }

        // Unknown admin subcommand — show help
        MessageUtil.send(sender, "admin-commands-title", null);
        MessageUtil.send(sender, "admin-cmd-admin", null);
        MessageUtil.send(sender, "admin-cmd-blacklist-add", null);
        MessageUtil.send(sender, "admin-cmd-blacklist-remove", null);
        MessageUtil.send(sender, "admin-cmd-blacklist-list", null);
        MessageUtil.send(sender, "admin-cmd-cancel", null);
        MessageUtil.send(sender, "admin-cmd-backup", null);
    }

    private void handleBlacklist(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "usage-blacklist", null);
            return;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    MessageUtil.send(sender, "usage-blacklist-add", null);
                    return;
                }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    MessageUtil.send(sender, "player-not-found", null);
                    return;
                }
                String reason = args.length >= 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length))
                        : "No reason";
                plugin.getStorageManager().addBlacklist(target.getUniqueId(), reason, sender.getName());
                MessageUtil.send(sender, "admin-blacklist-added", Map.of("player", target.getName(), "reason", reason));
            }
            case "remove" -> {
                if (args.length < 4) {
                    MessageUtil.send(sender, "usage-blacklist-remove", null);
                    return;
                }
                Player target = Bukkit.getPlayer(args[3]);
                if (target == null) {
                    MessageUtil.send(sender, "player-not-found", null);
                    return;
                }
                plugin.getStorageManager().removeBlacklist(target.getUniqueId());
                MessageUtil.send(sender, "admin-blacklist-removed", Map.of("player", target.getName()));
            }
            case "list" -> {
                var list = plugin.getStorageManager().getBlacklist();
                if (list.isEmpty()) {
                    MessageUtil.send(sender, "blacklist-no-players", null);
                    return;
                }
                MessageUtil.send(sender, "blacklist-header", null);
                for (String[] entry : list) {
                    String name = entry[0]; // UUID
                    String r = entry[1] != null ? entry[1] : "No reason";
                    String by = entry[2];
                    MessageUtil.send(sender, "blacklist-entry", Map.of("uuid", name, "by", by, "reason", r));
                }
            }
            default -> MessageUtil.send(sender, "usage-blacklist", null);
        }
    }

    private void handleBackup(CommandSender sender) {
        MessageUtil.send(sender, "backup-starting", null);
        // Run backup asynchronously to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getStorageManager().backup();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    MessageUtil.send(sender, "backup-success", null);
                } else {
                    MessageUtil.send(sender, "backup-failed", null);
                }
            });
        });
    }

    private void handleNPC(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }
        if (!player.hasPermission("auctify.admin.npc")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }
        if (args.length < 3) {
            MessageUtil.send(player, "admin-npc-usage", null);
            return;
        }

        String action = args[2].toLowerCase();
        if (action.equals("spawn")) {
            String name = args.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length))
                    : null;
            var npc = plugin.getAuctionNPC().spawn(player.getLocation(), name);
            MessageUtil.send(player, "admin-npc-spawned", java.util.Map.of("id", String.valueOf(npc.getEntityId())));
        } else if (action.equals("remove")) {
            org.bukkit.entity.Entity nearest = null;
            double nearestDist = 5.0;
            for (org.bukkit.entity.Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5)) {
                if (dev.auctify.npc.AuctionNPC.isAuctionNPC(entity)) {
                    double dist = entity.getLocation().distance(player.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = entity;
                    }
                }
            }
            if (nearest != null) {
                nearest.remove();
                MessageUtil.send(player, "admin-npc-removed", null);
            } else {
                MessageUtil.send(player, "admin-npc-not-found", null);
            }
        } else {
            MessageUtil.send(player, "admin-npc-usage", null);
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
