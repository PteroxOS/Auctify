package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.notification.NotificationManager;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Handles the /ac notifications command for managing notification preferences.
 */
public class NotificationsSubCommand implements SubCommand {

    private final Auctify plugin;

    public NotificationsSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.notifications")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length == 1) {
            showStatus(player);
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("toggle")) {
            if (args.length < 3) {
                MessageUtil.send(player, "notifications-usage", null);
                return;
            }

            String type = args[2].toLowerCase();
            NotificationManager.NotificationType notificationType = parseNotificationType(type);

            if (notificationType == null) {
                MessageUtil.send(player, "notifications-invalid-type", Map.of("type", type));
                return;
            }

            toggleNotification(player, notificationType);
        } else if (action.equals("all")) {
            if (args.length < 3) {
                MessageUtil.send(player, "notifications-usage", null);
                return;
            }

            String value = args[2].toLowerCase();
            boolean enable = value.equals("on") || value.equals("true") || value.equals("1");
            setAllNotifications(player, enable);
        } else {
            MessageUtil.send(player, "notifications-usage", null);
        }
    }

    private void showStatus(Player player) {
        NotificationManager.NotificationPreferences prefs = plugin.getNotificationManager()
                .getPreferences(player.getUniqueId());

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "§6§l✦ Notification Preferences");
        MessageUtil.sendRaw(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendRaw(player, " §7Outbid: §f"
                + (prefs.isEnabled(NotificationManager.NotificationType.OUTBID) ? "§aEnabled" : "§cDisabled"));
        MessageUtil.sendRaw(player, " §7Buyout: §f"
                + (prefs.isEnabled(NotificationManager.NotificationType.BUYOUT) ? "§aEnabled" : "§cDisabled"));
        MessageUtil.sendRaw(player, " §7Auction Won: §f"
                + (prefs.isEnabled(NotificationManager.NotificationType.AUCTION_WON) ? "§aEnabled" : "§cDisabled"));
        MessageUtil.sendRaw(player, " §7Item Sold: §f"
                + (prefs.isEnabled(NotificationManager.NotificationType.ITEM_SOLD) ? "§aEnabled" : "§cDisabled"));
        MessageUtil.sendRaw(player, " §7Expiration: §f"
                + (prefs.isEnabled(NotificationManager.NotificationType.EXPIRATION) ? "§aEnabled" : "§cDisabled"));
        MessageUtil.sendRaw(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendRaw(player, " §e/ac notifications toggle <type> §7- Toggle specific notification");
        MessageUtil.sendRaw(player, " §e/ac notifications all <on|off> §7- Enable/disable all");
        MessageUtil.sendRaw(player, "");
    }

    private void toggleNotification(Player player, NotificationManager.NotificationType type) {
        NotificationManager.NotificationPreferences prefs = plugin.getNotificationManager()
                .getPreferences(player.getUniqueId());
        boolean newState = !prefs.isEnabled(type);
        prefs.setEnabled(type, newState);
        plugin.getNotificationManager().setPreferences(player.getUniqueId(), prefs);

        String typeName = type.name().toLowerCase().replace("_", " ");
        MessageUtil.send(player, newState ? "notifications-enabled" : "notifications-disabled",
                Map.of("type", typeName));
    }

    private void setAllNotifications(Player player, boolean enable) {
        NotificationManager.NotificationPreferences prefs = plugin.getNotificationManager()
                .getPreferences(player.getUniqueId());
        for (NotificationManager.NotificationType type : NotificationManager.NotificationType.values()) {
            prefs.setEnabled(type, enable);
        }
        plugin.getNotificationManager().setPreferences(player.getUniqueId(), prefs);

        MessageUtil.send(player, enable ? "notifications-all-enabled" : "notifications-all-disabled", null);
    }

    private NotificationManager.NotificationType parseNotificationType(String type) {
        return switch (type) {
            case "outbid" -> NotificationManager.NotificationType.OUTBID;
            case "buyout" -> NotificationManager.NotificationType.BUYOUT;
            case "auction-won", "won" -> NotificationManager.NotificationType.AUCTION_WON;
            case "item-sold", "sold" -> NotificationManager.NotificationType.ITEM_SOLD;
            case "expiration", "expiry" -> NotificationManager.NotificationType.EXPIRATION;
            default -> null;
        };
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
