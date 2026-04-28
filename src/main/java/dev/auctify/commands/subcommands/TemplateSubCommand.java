package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.auction.ListingTemplate;
import dev.auctify.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Template subcommand - allows players to save and manage listing templates.
 */
public class TemplateSubCommand implements SubCommand {

    private final Auctify plugin;

    public TemplateSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "template-usage", null);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "create" -> handleCreate(player, args);
            case "list" -> handleList(player);
            case "delete" -> handleDelete(player, args);
            case "use" -> handleUse(player, args);
            default -> MessageUtil.send(player, "template-usage", null);
        }
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 5) {
            MessageUtil.send(player, "template-create-usage", null);
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            MessageUtil.send(player, "hold-item-to-sell", null);
            return;
        }

        String name = args[2];
        double startPrice;
        double buyoutPrice;
        int duration;

        try {
            startPrice = Double.parseDouble(args[3]);
            buyoutPrice = args.length > 4 ? Double.parseDouble(args[4]) : 0;
            duration = args.length > 5 ? Integer.parseInt(args[5]) : 300;
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        if (startPrice <= 0) {
            MessageUtil.send(player, "invalid-price", null);
            return;
        }

        var template = plugin.getTemplateManager().createTemplate(
                player.getUniqueId(), name, startPrice, buyoutPrice, duration, item.getType());
        
        MessageUtil.send(player, "template-created", Map.of(
            "name", name,
            "start", String.valueOf(startPrice),
            "buyout", buyoutPrice > 0 ? String.valueOf(buyoutPrice) : "None",
            "duration", String.valueOf(duration)
        ));
    }

    private void handleList(Player player) {
        var templates = plugin.getTemplateManager().getTemplates(player.getUniqueId());
        if (templates.isEmpty()) {
            MessageUtil.send(player, "template-empty", null);
            return;
        }

        MessageUtil.send(player, "template-list-header", Map.of("count", String.valueOf(templates.size())));
        for (Map.Entry<String, ListingTemplate> entry : templates.entrySet()) {
            ListingTemplate template = entry.getValue();
            MessageUtil.send(player, "template-list-entry", Map.of(
                "name", template.getName(),
                "start", String.valueOf(template.getStartPrice()),
                "buyout", template.getBuyoutPrice() > 0 ? String.valueOf(template.getBuyoutPrice()) : "None",
                "duration", String.valueOf(template.getDuration())
            ));
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "template-delete-usage", null);
            return;
        }

        String name = args[2];
        if (plugin.getTemplateManager().deleteTemplate(player.getUniqueId(), name)) {
            MessageUtil.send(player, "template-deleted", Map.of("name", name));
        } else {
            MessageUtil.send(player, "template-not-found", Map.of("name", name));
        }
    }

    private void handleUse(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(player, "template-use-usage", null);
            return;
        }

        String name = args[2];
        var template = plugin.getTemplateManager().getTemplate(player.getUniqueId(), name);
        if (template == null) {
            MessageUtil.send(player, "template-not-found", Map.of("name", name));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            MessageUtil.send(player, "hold-item-to-sell", null);
            return;
        }

        // Execute sell command with template values
        String command = "ac sell " + template.getStartPrice();
        if (template.getBuyoutPrice() > 0) {
            command += " " + template.getBuyoutPrice();
        }
        if (template.getDuration() > 0) {
            command += " " + template.getDuration();
        }

        player.performCommand(command);
        MessageUtil.send(player, "template-applied", Map.of("name", name));
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }
}
