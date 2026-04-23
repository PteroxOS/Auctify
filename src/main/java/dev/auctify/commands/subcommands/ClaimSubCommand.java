package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ac claim — Opens the mailbox GUI to collect pending deliveries.
 */
public class ClaimSubCommand implements SubCommand {

    private final Auctify plugin;

    public ClaimSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        plugin.getClaimGUI().open(player);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @Override
    public String getPermission() {
        return "auctify.use";
    }

    @Override
    public String getUsage() {
        return "/ac claim";
    }

    @Override
    public String getDescription() {
        return "Collect pending items from your mailbox.";
    }
}
