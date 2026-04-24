package dev.auctify.commands;

import dev.auctify.Auctify;
import dev.auctify.commands.subcommands.*;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Root command dispatcher for the /ac command.
 * Running /ac with no args opens the auction GUI directly (for players).
 * Routes subcommands to their respective handlers.
 */
public class AuctifyCommand implements CommandExecutor {

    private final Auctify plugin;

    /** Map of subcommand names to their handler instances. */
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    /**
     * Registers all subcommands.
     *
     * @param plugin the main plugin instance
     */
    public AuctifyCommand(Auctify plugin) {
        this.plugin = plugin;

        // Register all subcommands
        subCommands.put("sell", new SellSubCommand(plugin));
        subCommands.put("bid", new BidSubCommand(plugin));
        subCommands.put("open", new OpenSubCommand(plugin));
        subCommands.put("cancel", new CancelSubCommand(plugin));
        subCommands.put("search", new SearchSubCommand(plugin));
        subCommands.put("history", new HistorySubCommand(plugin));
        subCommands.put("reload", new ReloadSubCommand(plugin));
        subCommands.put("about", new AboutSubCommand(plugin));
        subCommands.put("claim", new ClaimSubCommand(plugin));
        subCommands.put("admin", new AdminSubCommand(plugin));
        // New v1.0.1 features
        subCommands.put("bidhistory", new BidHistorySubCommand(plugin));
        subCommands.put("extend", new ExtendSubCommand(plugin));
        subCommands.put("bulkcancel", new BulkCancelSubCommand(plugin));
        // Setup wizard is handled directly in onCommand
    }

    /**
     * Dispatches the /ac command to the appropriate subcommand handler.
     * Running /ac with no args opens the GUI directly for players.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No args → open GUI directly for players, show help for console
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getAuctionGUI().open(player);
            } else {
                sendHelpMenu(sender);
            }
            return true;
        }

        // "help" subcommand → show help menu
        if (args[0].equalsIgnoreCase("help")) {
            sendHelpMenu(sender);
            return true;
        }

        // "setup" subcommand → launch setup wizard
        if (args[0].equalsIgnoreCase("setup")) {
            if (!(sender instanceof Player player)) {
                MessageUtil.sendRaw(sender, "§cSetup wizard can only be used in-game.");
                return true;
            }
            if (args.length > 1 && args[1].equalsIgnoreCase("skip")) {
                plugin.getSetupWizard().skipSetup(player);
            } else if (args.length > 2) {
                // Handle setup step responses (e.g., /ac setup step1 en)
                try {
                    int step = Integer.parseInt(args[1].replace("step", ""));
                    String value = args[2];
                    plugin.getSetupWizard().handleStep(player, step, value);
                } catch (NumberFormatException e) {
                    plugin.getSetupWizard().startSetup(player);
                }
            } else {
                plugin.getSetupWizard().startSetup(player);
            }
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);

        if (sub == null) {
            MessageUtil.sendRaw(sender, "§cUnknown subcommand. Use §f/ac help §cfor a list.");
            return true;
        }

        // Check if command requires player
        if (sub.isPlayerOnly() && !(sender instanceof Player)) {
            MessageUtil.send(sender, "player-only", null);
            return true;
        }

        // Check permission for admin subcommands
        if (sub instanceof AdminSubCommand || sub instanceof ReloadSubCommand) {
            if (!sender.hasPermission("auctify.admin")) {
                MessageUtil.send(sender, "no-permission", null);
                return true;
            }
        }

        // Execute the subcommand
        sub.execute(sender, args);
        return true;
    }

    /**
     * Sends the help menu with all available subcommands.
     */
    private void sendHelpMenu(CommandSender sender) {
        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "§6§l✦ AUCTIFY §8— §7Command Reference");
        MessageUtil.sendRaw(sender, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendRaw(sender, " §e/ac §8» §7Open the auction house");
        MessageUtil.sendRaw(sender, " §e/ac sell §6<price> §7[buyout] [duration] §8» §7List held item");
        MessageUtil.sendRaw(sender, " §e/ac bid §6<id> <amount> §8» §7Place a bid");
        MessageUtil.sendRaw(sender, " §e/ac cancel §6<id> §8» §7Cancel your listing");
        MessageUtil.sendRaw(sender, " §e/ac search §6<query> §8» §7Search listings");
        MessageUtil.sendRaw(sender, " §e/ac history §8» §7View your auction history");
        MessageUtil.sendRaw(sender, " §e/ac bidhistory §6<id> §8» §7View bid history §b(NEW)");
        MessageUtil.sendRaw(sender, " §e/ac extend §6<id> <minutes> §8» §7Extend auction §b(NEW)");
        MessageUtil.sendRaw(sender, " §e/ac bulkcancel §8» §7Cancel all your auctions §b(NEW)");
        MessageUtil.sendRaw(sender, " §e/ac claim §8» §7Collect pending items");
        MessageUtil.sendRaw(sender, " §e/ac about §8» §7Plugin information");
        MessageUtil.sendRaw(sender, " §e/ac admin §8» §7Admin moderation panel §c(Admin)");
        MessageUtil.sendRaw(sender, " §e/ac setup §8» §7Run setup wizard §c(Admin)");
        MessageUtil.sendRaw(sender, " §e/ac reload §8» §7Reload configuration §c(Admin)");
        MessageUtil.sendRaw(sender, " §e/ac help §8» §7Show this menu");
        MessageUtil.sendRaw(sender, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendRaw(sender, "");
    }

    /**
     * Returns the subcommand map for tab completion.
     *
     * @return the map of subcommand names to handlers
     */
    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
}
