package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;

/**
 * Handles the /ac reload command. Reloads the plugin configuration from disk.
 * Requires auctify.admin permission.
 */
public class ReloadSubCommand implements SubCommand {

    private final Auctify plugin;

    /** Constructor. */
    public ReloadSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("auctify.admin")) {
            MessageUtil.send(sender, "no-permission", null);
            return;
        }

        // Reload config from disk — all config reads use getConfig() at call-time,
        // so no cached values need to be invalidated
        plugin.reloadConfig();
        dev.auctify.util.ConfigUtil.init(plugin);
        dev.auctify.util.MessageUtil.reload();

        MessageUtil.send(sender, "config-reloaded", null);
        plugin.getLogger().info("Configuration reloaded by " + sender.getName());
    }

    @Override
    public boolean isPlayerOnly() {
        return false; // Console can reload too
    }
}
