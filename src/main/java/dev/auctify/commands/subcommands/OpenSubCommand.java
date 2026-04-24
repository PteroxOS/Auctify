package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Handles the /ac open command. Opens the main auction house GUI. */
public class OpenSubCommand implements SubCommand {

    private final Auctify plugin;

    /** Constructor. */
    public OpenSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("auctify.open")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        plugin.getAuctionGUI().open(player);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
