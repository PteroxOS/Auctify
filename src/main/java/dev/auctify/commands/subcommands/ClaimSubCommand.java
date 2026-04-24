package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ac claim — Opens the mailbox GUI to collect pending deliveries. */
public class ClaimSubCommand implements SubCommand {

    private final Auctify plugin;

    public ClaimSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.claim")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        plugin.getClaimGUI().open(player);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    public String getPermission() {
        return "auctify.use";
    }

    public String getUsage() {
        return "/ac claim";
    }

    public String getDescription() {
        return "Collect pending items from your mailbox.";
    }
}
