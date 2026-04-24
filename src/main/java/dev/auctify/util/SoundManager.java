package dev.auctify.util;

import dev.auctify.Auctify;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Manages sound effects for auction events to enhance UX.
 */
public class SoundManager {

    private final Auctify plugin;
    private final boolean enabled;

    public SoundManager(Auctify plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("sounds.enabled", true);
    }

    /**
     * Plays a sound if enabled in config.
     */
    private void play(Player player, Sound sound, float volume, float pitch) {
        if (!enabled || player == null || !player.isOnline()) return;
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            // Sound might not exist in older versions, ignore
        }
    }

    // GUI Sounds
    public void playGUIClick(Player player) {
        play(player, Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    public void playGUIOpen(Player player) {
        play(player, Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    public void playGUIClose(Player player) {
        play(player, Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
    }

    // Auction Sounds
    public void playBidPlaced(Player player) {
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
    }

    public void playBidSuccess(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 2.0f);
    }

    public void playOutbid(Player player) {
        play(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
    }

    public void playAuctionWon(Player player) {
        play(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
    }

    public void playAuctionSold(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f);
    }

    public void playBuyoutSuccess(Player player) {
        play(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
    }

    public void playListingCreated(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.6f, 1.8f);
    }

    public void playError(Player player) {
        play(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
    }

    public void playSuccess(Player player) {
        play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);
    }

    public void playMoneyReceived(Player player) {
        play(player, Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.2f);
    }

    public void playAlert(Player player) {
        play(player, Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 1.8f);
    }
}
