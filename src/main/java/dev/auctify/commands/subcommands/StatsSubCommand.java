package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * /ac stats — Open player statistics GUI.
 * Usage: /ac stats — View your own stats
 *        /ac stats <player> — View another player's stats (admin only)
 */
public class StatsSubCommand implements SubCommand {

    private final Auctify plugin;

    public StatsSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (args.length >= 2 && player.hasPermission("auctify.admin")) {
            // Admin viewing other player's stats
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                MessageUtil.send(player, "player-not-found", null);
                return;
            }
            plugin.getStatsGUI().open(player, target.getUniqueId(), target.getName());
        } else {
            // View own stats
            plugin.getStatsGUI().openSelf(player);
        }
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
