package dev.auctify.npc;

import dev.auctify.Auctify;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Simple NPC Auctioneer using vanilla Villager.
 * Opens auction GUI on right-click.
 * 
 * For advanced NPC features (skins, floating text), Citizens2 integration
 * can be added as soft dependency.
 */
public class AuctionNPC implements Listener {

    private final Auctify plugin;
    private static final String NPC_METADATA = "auctify.npc";

    public AuctionNPC(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns an Auctioneer NPC at the given location.
     * @param location where to spawn
     * @param name display name (truncated to 16 chars for vanilla)
     * @return the spawned villager entity
     */
    public Villager spawn(Location location, String name) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        
        // Configure as NPC (non-moving, non-trading villager)
        villager.setCustomName(name != null ? name : "§6§lAuctioneer");
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerLevel(5); // Master level for visual
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setRemoveWhenFarAway(false);
        
        // Mark as Auctify NPC
        villager.setMetadata(NPC_METADATA, new FixedMetadataValue(plugin, true));
        
        return villager;
    }

    /**
     * Checks if an entity is an Auctify NPC.
     */
    public static boolean isAuctionNPC(org.bukkit.entity.Entity entity) {
        return entity.hasMetadata(NPC_METADATA);
    }

    /**
     * Handles right-click on NPC to open auction GUI.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().hasMetadata(NPC_METADATA)) {
            event.setCancelled(true);
            plugin.getAuctionGUI().open(event.getPlayer());
        }
    }
}
