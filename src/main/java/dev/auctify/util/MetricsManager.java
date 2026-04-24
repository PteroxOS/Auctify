package dev.auctify.util;

import dev.auctify.Auctify;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;

/**
 * bStats Metrics for Auctify plugin analytics.
 * Anonymous usage data to help improve the plugin.
 */
public class MetricsManager {

    private final Auctify plugin;
    // NOTE: bStats plugin ID - get from https://bstats.org/plugin/bukkit/Auctify
    // After registering your plugin on bStats, replace 0 with your actual plugin ID
    private final int pluginId = 0;

    public MetricsManager(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes bStats metrics.
     */
    public void init() {
        // Skip if pluginId not set (0 means disabled)
        if (pluginId == 0) {
            plugin.getLogger().info("[Metrics] bStats metrics disabled. Register at bstats.org to enable.");
            return;
        }

        try {
            // Check if bStats is enabled in config (default: false to avoid errors)
            if (!plugin.getConfig().getBoolean("metrics.enabled", false)) {
                plugin.getLogger().info("[Metrics] bStats metrics disabled by config.");
                return;
            }

            // Try to load bStats - will fail if not shaded properly
            Metrics metrics = new Metrics(plugin, pluginId);

            // Add custom charts
            addLanguageChart(metrics);
            addStorageTypeChart(metrics);
            addActiveListingsChart(metrics);
            addFeatureUsageChart(metrics);

            plugin.getLogger().info("[Metrics] bStats metrics enabled. Thank you for supporting Auctify!");
        } catch (NoClassDefFoundError e) {
            plugin.getLogger()
                    .warning("[Metrics] bStats library not found. Metrics disabled. Add bStats dependency to enable.");
        } catch (Exception e) {
            plugin.getLogger().warning("[Metrics] Failed to initialize bStats: " + e.getMessage());
        }
    }

    /**
     * Language usage chart.
     */
    private void addLanguageChart(Metrics metrics) {
        metrics.addCustomChart(new SimplePie("language", () -> plugin.getConfig().getString("general.language", "en")));
    }

    /**
     * Storage type usage chart.
     */
    private void addStorageTypeChart(Metrics metrics) {
        metrics.addCustomChart(
                new SimplePie("storage_type", () -> plugin.getConfig().getString("storage.type", "sqlite")));
    }

    /**
     * Average active listings count.
     */
    private void addActiveListingsChart(Metrics metrics) {
        metrics.addCustomChart(new SimplePie("active_listings_range", () -> {
            int count = plugin.getAuctionManager().getActiveListings().size();
            if (count == 0)
                return "0";
            if (count < 10)
                return "1-9";
            if (count < 50)
                return "10-49";
            if (count < 100)
                return "50-99";
            if (count < 500)
                return "100-499";
            return "500+";
        }));
    }

    /**
     * Feature usage pie chart.
     */
    private void addFeatureUsageChart(Metrics metrics) {
        metrics.addCustomChart(new AdvancedPie("features_enabled", () -> {
            java.util.Map<String, Integer> map = new java.util.HashMap<>();
            map.put("discord", plugin.getConfig().getBoolean("discord.enabled", false) ? 1 : 0);
            map.put("tax", plugin.getConfig().getDouble("tax.percent", 0) > 0 ? 1 : 0);
            map.put("listing_fee", plugin.getConfig().getDouble("fees.listing", 0) > 0 ? 1 : 0);
            map.put("auto_relist", plugin.getConfig().getBoolean("auto-relist.enabled", false) ? 1 : 0);
            map.put("sniping_protection", plugin.getConfig().getBoolean("sniping-protection.enabled", true) ? 1 : 0);
            map.put("sounds", plugin.getConfig().getBoolean("sounds.enabled", true) ? 1 : 0);
            map.put("per_world", !plugin.getConfig().getString("worlds.mode", "global").equals("global") ? 1 : 0);
            return map;
        }));
    }
}
