package dev.auctify.auction;

import dev.auctify.Auctify;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages listing templates for players.
 */
public class TemplateManager {

    private final Auctify plugin;
    private final Map<UUID, Map<String, ListingTemplate>> playerTemplates = new HashMap<>();

    public TemplateManager(Auctify plugin) {
        this.plugin = plugin;
        loadTemplates();
    }

    /**
     * Loads templates from config (for now, templates are stored in memory per
     * player).
     * In a future version, this could load from a database.
     */
    private void loadTemplates() {
        // Templates are stored per-player in memory for now
        // Could be extended to load from config or database
    }

    /**
     * Creates a new template for a player.
     */
    public ListingTemplate createTemplate(UUID playerUUID, String name, double startPrice,
            double buyoutPrice, int duration, Material itemType) {
        String templateId = playerUUID.toString() + "_" + name.toLowerCase().replace(" ", "_");
        ListingTemplate template = new ListingTemplate(templateId, name, startPrice, buyoutPrice, duration, itemType);

        playerTemplates.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(name.toLowerCase(), template);
        return template;
    }

    /**
     * Gets a template by name for a player.
     */
    public ListingTemplate getTemplate(UUID playerUUID, String name) {
        Map<String, ListingTemplate> templates = playerTemplates.get(playerUUID);
        if (templates == null)
            return null;
        return templates.get(name.toLowerCase());
    }

    /**
     * Gets all templates for a player.
     */
    public Map<String, ListingTemplate> getTemplates(UUID playerUUID) {
        return playerTemplates.getOrDefault(playerUUID, new HashMap<>());
    }

    /**
     * Deletes a template for a player.
     */
    public boolean deleteTemplate(UUID playerUUID, String name) {
        Map<String, ListingTemplate> templates = playerTemplates.get(playerUUID);
        if (templates == null)
            return false;
        return templates.remove(name.toLowerCase()) != null;
    }

    /**
     * Clears all templates for a player.
     */
    public void clearTemplates(UUID playerUUID) {
        playerTemplates.remove(playerUUID);
    }
}
