package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Theme subcommand - allows players to switch GUI themes.
 */
public class ThemeSubCommand implements SubCommand {

    private final Auctify plugin;

    public ThemeSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "theme-usage", null);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "list" -> handleList(player);
            case "set" -> handleSet(player, args);
            default -> MessageUtil.send(player, "theme-usage", null);
        }
    }

    private void handleList(Player player) {
        var themeManager = plugin.getGUIThemeManager();
        var currentTheme = themeManager.getCurrentTheme();
        
        MessageUtil.send(player, "theme-list-header", null);
        MessageUtil.send(player, "theme-current", Map.of("theme", currentTheme != null ? currentTheme.getName() : "default"));
        
        // List available themes from config
        var config = plugin.getConfig();
        var themesSection = config.getConfigurationSection("gui.themes");
        if (themesSection != null) {
            for (String themeName : themesSection.getKeys(false)) {
                String indicator = themeName.equalsIgnoreCase(currentTheme != null ? currentTheme.getName() : "default") ? "§a▶ " : "§7- ";
                MessageUtil.send(player, "theme-list-entry", Map.of("indicator", indicator, "theme", themeName));
            }
        }
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "theme-set-usage", null);
            return;
        }

        String themeName = args[2];
        var themeManager = plugin.getGUIThemeManager();
        
        if (themeManager.getTheme(themeName) == null) {
            MessageUtil.send(player, "theme-not-found", Map.of("theme", themeName));
            return;
        }

        // For now, set globally (could be per-player in future)
        themeManager.setCurrentTheme(themeName);
        plugin.getConfig().set("gui.theme", themeName);
        plugin.saveConfig();
        
        MessageUtil.send(player, "theme-set-success", Map.of("theme", themeName));
        
        // Reopen GUI to show new theme
        plugin.getAuctionGUI().open(player);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
