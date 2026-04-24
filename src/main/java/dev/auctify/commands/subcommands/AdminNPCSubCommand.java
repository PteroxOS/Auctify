package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.npc.AuctionNPC;
import dev.auctify.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.Map;

/**
 * Admin command to spawn/remove Auctioneer NPC.
 * Usage: /ac admin npc spawn [name]
 *        /ac admin npc remove (removes nearest NPC within 5 blocks)
 */
public class AdminNPCSubCommand implements SubCommand {

    private final Auctify plugin;

    public AdminNPCSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "player-only", null);
            return;
        }

        if (!player.hasPermission("auctify.admin.npc")) {
            MessageUtil.send(player, "no-permission", null);
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "admin-npc-usage", null);
            return;
        }

        String action = args[1].toLowerCase();
        
        if (action.equals("spawn")) {
            Location loc = player.getLocation();
            String name = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
            
            Villager npc = plugin.getAuctionNPC().spawn(loc, name);
            MessageUtil.send(player, "admin-npc-spawned", Map.of("id", String.valueOf(npc.getEntityId())));
            
        } else if (action.equals("remove")) {
            // Find nearest NPC within 5 blocks
            Location playerLoc = player.getLocation();
            Entity nearest = null;
            double nearestDist = 5.0;
            
            for (Entity entity : player.getWorld().getNearbyEntities(playerLoc, 5, 5, 5)) {
                if (AuctionNPC.isAuctionNPC(entity)) {
                    double dist = entity.getLocation().distance(playerLoc);
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
        return true;
    }
}
