package dev.auctify.commands.subcommands;

import dev.auctify.Auctify;
import dev.auctify.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Rating subcommand - allows players to view ratings and reputation.
 */
public class RatingSubCommand implements SubCommand {

    private final Auctify plugin;

    public RatingSubCommand(Auctify plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player player) {
                showRating(sender, player.getUniqueId(), player.getName());
            } else {
                MessageUtil.send(sender, "rating-usage", null);
            }
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        if (!target.hasPlayedBefore() && target.getUniqueId() == null) {
            MessageUtil.send(sender, "player-not-found", null);
            return;
        }

        showRating(sender, target.getUniqueId(), target.getName());
    }

    private void showRating(CommandSender sender, UUID targetUUID, String targetName) {
        double avgRating = plugin.getStorageManager().getAverageRating(targetUUID);
        int ratingCount = plugin.getStorageManager().getRatingCount(targetUUID);
        
        String avgDisplay = avgRating >= 0 ? String.format("%.1f", avgRating) : "No ratings";
        String stars = avgRating >= 0 ? getStarDisplay(avgRating) : "☆☆☆☆☆";
        String reputation = getReputationTitle(avgRating);
        
        MessageUtil.send(sender, "rating-header", Map.of("player", targetName));
        MessageUtil.send(sender, "rating-average", Map.of("stars", stars, "avg", avgDisplay));
        MessageUtil.send(sender, "rating-count", Map.of("count", String.valueOf(ratingCount)));
        MessageUtil.send(sender, "rating-reputation", Map.of("title", reputation));
    }

    private String getStarDisplay(double rating) {
        int fullStars = (int) Math.floor(rating);
        boolean hasHalf = rating - fullStars >= 0.5;
        int emptyStars = 5 - fullStars - (hasHalf ? 1 : 0);
        
        StringBuilder sb = new StringBuilder();
        sb.append("§e").append("★".repeat(fullStars));
        if (hasHalf) {
            sb.append("§7★");
        }
        sb.append("§8").append("☆".repeat(emptyStars));
        return sb.toString();
    }

    private String getReputationTitle(double rating) {
        if (rating < 0) return "§7New Seller";
        if (rating >= 4.8) return "§6⭐ Legendary Seller";
        if (rating >= 4.5) return "§a✓ Trusted Seller";
        if (rating >= 4.0) return "§e★ Good Seller";
        if (rating >= 3.5) return "§7+ Average Seller";
        if (rating >= 3.0) return "§c- Below Average";
        if (rating >= 2.0) return "§4✗ Poor Seller";
        return "§4✗✗ Avoid";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }
}
