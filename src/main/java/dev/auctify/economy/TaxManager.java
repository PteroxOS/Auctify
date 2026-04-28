package dev.auctify.economy;

import dev.auctify.Auctify;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.TreeMap;

/**
 * Manages tax brackets for auction listings based on listing value.
 */
public class TaxManager {

    private final Auctify plugin;
    private final TreeMap<Double, Double> taxBrackets;

    public TaxManager(Auctify plugin) {
        this.plugin = plugin;
        this.taxBrackets = new TreeMap<>();
        loadTaxBrackets();
    }

    /**
     * Loads tax brackets from configuration.
     */
    private void loadTaxBrackets() {
        taxBrackets.clear();
        
        if (!plugin.getConfig().getBoolean("tax-brackets.enabled", false)) {
            return;
        }

        var config = plugin.getConfig().getConfigurationSection("tax-brackets.brackets");
        if (config == null) {
            return;
        }

        for (String key : config.getKeys(false)) {
            try {
                double threshold = Double.parseDouble(key);
                double percentage = config.getDouble(key);
                taxBrackets.put(threshold, percentage);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid tax bracket threshold: " + key);
            }
        }

        plugin.getLogger().info("[TaxManager] Loaded " + taxBrackets.size() + " tax brackets.");
    }

    /**
     * Reloads tax brackets from configuration.
     */
    public void reload() {
        loadTaxBrackets();
    }

    /**
     * Calculates the tax amount for a given listing value.
     * 
     * @param value The listing value (final sale price or buyout price)
     * @return The tax amount to be deducted
     */
    public double calculateTax(double value) {
        if (!plugin.getConfig().getBoolean("tax-brackets.enabled", false)) {
            return 0;
        }

        // Find the highest threshold that the value exceeds
        Map.Entry<Double, Double> applicableBracket = taxBrackets.floorEntry(value);
        
        if (applicableBracket == null) {
            return 0; // No tax for values below the lowest threshold
        }

        double percentage = applicableBracket.getValue();
        return value * (percentage / 100.0);
    }

    /**
     * Calculates the tax amount for a listing, considering player exemption.
     * 
     * @param player The player creating the listing
     * @param value The listing value
     * @return The tax amount to be deducted
     */
    public double calculateTax(Player player, double value) {
        String exemptPerm = plugin.getConfig().getString("tax-brackets.exempt-permission", "auctify.tax.exempt");
        
        if (player.hasPermission(exemptPerm)) {
            return 0;
        }

        return calculateTax(value);
    }

    /**
     * Gets the tax percentage for a given value.
     * 
     * @param value The listing value
     * @return The tax percentage, or 0 if no tax applies
     */
    public double getTaxPercentage(double value) {
        if (!plugin.getConfig().getBoolean("tax-brackets.enabled", false)) {
            return 0;
        }

        Map.Entry<Double, Double> applicableBracket = taxBrackets.floorEntry(value);
        
        if (applicableBracket == null) {
            return 0;
        }

        return applicableBracket.getValue();
    }

    /**
     * Checks if tax brackets are enabled.
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("tax-brackets.enabled", false);
    }
}
