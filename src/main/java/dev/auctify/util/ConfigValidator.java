package dev.auctify.util;

import dev.auctify.Auctify;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the plugin configuration on startup.
 * Checks for required settings, valid values, and provides helpful error messages.
 */
public class ConfigValidator {

    private final Auctify plugin;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public ConfigValidator(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Validates the entire configuration.
     * 
     * @return true if configuration is valid (no errors), false otherwise
     */
    public boolean validate() {
        warnings.clear();
        errors.clear();

        FileConfiguration config = plugin.getConfig();

        plugin.getLogger().info("§e[Config] Validating configuration...");

        // Validate storage settings
        validateStorage(config);

        // Validate bidding settings
        validateBidding(config);

        // Validate listing settings
        validateListing(config);

        // Validate tax settings
        validateTax(config);

        // Validate notification settings
        validateNotifications(config);

        // Validate sound settings
        validateSounds(config);

        // Print results
        printResults();

        return errors.isEmpty();
    }

    private void validateStorage(FileConfiguration config) {
        String storageType = config.getString("storage.type", "sqlite").toLowerCase();
        if (!storageType.matches("sqlite|mysql|h2|memory")) {
            errors.add("Invalid storage type: " + storageType + ". Must be: sqlite, mysql, h2, or memory");
        }

        if (storageType.equals("mysql")) {
            String host = config.getString("storage.mysql.host");
            String database = config.getString("storage.mysql.database");
            String username = config.getString("storage.mysql.username");

            if (host == null || host.isEmpty()) {
                errors.add("MySQL host is required when using MySQL storage");
            }
            if (database == null || database.isEmpty()) {
                errors.add("MySQL database name is required when using MySQL storage");
            }
            if (username == null || username.isEmpty()) {
                errors.add("MySQL username is required when using MySQL storage");
            }
        }

        int autoSaveInterval = config.getInt("storage.auto-save-interval", 5);
        if (autoSaveInterval < 0) {
            errors.add("Auto-save interval must be >= 0 (0 = disabled)");
        }
    }

    private void validateBidding(FileConfiguration config) {
        double minIncrement = config.getDouble("bidding.min-increment", 0);
        if (minIncrement < 0) {
            errors.add("Minimum bid increment must be >= 0");
        }

        double minStartPrice = config.getDouble("bidding.min-start-price", 1.0);
        if (minStartPrice < 0) {
            errors.add("Minimum start price must be >= 0");
        }

        double maxStartPrice = config.getDouble("bidding.max-start-price", 1000000.0);
        if (maxStartPrice < 0) {
            errors.add("Maximum start price must be >= 0");
        }
        if (maxStartPrice > 0 && minStartPrice > maxStartPrice) {
            errors.add("Minimum start price cannot be greater than maximum start price");
        }

        int defaultDuration = config.getInt("bidding.default-duration", 60);
        if (defaultDuration < 1) {
            errors.add("Default auction duration must be >= 1 minute");
        }

        int maxDuration = config.getInt("bidding.max-duration", 720);
        if (maxDuration < 1) {
            errors.add("Maximum auction duration must be >= 1 minute");
        }
        if (defaultDuration > maxDuration) {
            errors.add("Default auction duration cannot be greater than maximum duration");
        }
    }

    private void validateListing(FileConfiguration config) {
        double listingFeePercent = config.getDouble("listing-fee.percent", 0);
        if (listingFeePercent < 0 || listingFeePercent > 100) {
            errors.add("Listing fee percent must be between 0 and 100");
        }

        double listingFeeMin = config.getDouble("listing-fee.min", 0);
        if (listingFeeMin < 0) {
            errors.add("Listing fee minimum must be >= 0");
        }

        double listingFeeMax = config.getDouble("listing-fee.max", 0);
        if (listingFeeMax < 0) {
            errors.add("Listing fee maximum must be >= 0");
        }
        if (listingFeeMax > 0 && listingFeeMin > listingFeeMax) {
            errors.add("Listing fee minimum cannot be greater than maximum");
        }

        int maxListings = config.getInt("listing.max-per-player", 10);
        if (maxListings < 1) {
            errors.add("Maximum listings per player must be >= 1");
        }
    }

    private void validateTax(FileConfiguration config) {
        double taxPercent = config.getDouble("tax.percent", 0);
        if (taxPercent < 0 || taxPercent > 100) {
            errors.add("Tax percent must be between 0 and 100");
        }

        // Validate tax brackets if enabled
        if (config.getBoolean("tax-brackets.enabled", false)) {
            ConfigurationSection brackets = config.getConfigurationSection("tax-brackets.brackets");
            if (brackets == null || brackets.getKeys(false).isEmpty()) {
                warnings.add("Tax brackets enabled but no brackets defined");
            } else {
                for (String key : brackets.getKeys(false)) {
                    double percent = brackets.getDouble(key);
                    if (percent < 0 || percent > 100) {
                        errors.add("Tax bracket " + key + " percent must be between 0 and 100");
                    }
                }
            }
        }
    }

    private void validateNotifications(FileConfiguration config) {
        boolean discordEnabled = config.getBoolean("discord.enabled", false);
        if (discordEnabled) {
            String webhookUrl = config.getString("discord.webhook-url", "");
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                errors.add("Discord notifications enabled but webhook URL is not set");
            }
        }
    }

    private void validateSounds(FileConfiguration config) {
        boolean soundsEnabled = config.getBoolean("sounds.enabled", true);
        if (soundsEnabled) {
            double volume = config.getDouble("sounds.volume", 0.5);
            if (volume < 0 || volume > 1) {
                errors.add("Sound volume must be between 0.0 and 1.0");
            }

            double pitch = config.getDouble("sounds.pitch", 1.0);
            if (pitch < 0.5 || pitch > 2.0) {
                errors.add("Sound pitch must be between 0.5 and 2.0");
            }
        }
    }

    private void printResults() {
        if (errors.isEmpty() && warnings.isEmpty()) {
            plugin.getLogger().info("§a[Config] Configuration validation passed.");
            return;
        }

        if (!warnings.isEmpty()) {
            plugin.getLogger().warning("§e[Config] Configuration warnings:");
            for (String warning : warnings) {
                plugin.getLogger().warning("  - " + warning);
            }
        }

        if (!errors.isEmpty()) {
            plugin.getLogger().severe("§c[Config] Configuration errors found:");
            for (String error : errors) {
                plugin.getLogger().severe("  - " + error);
            }
            plugin.getLogger().severe("§c[Config] Please fix these errors in config.yml and restart.");
        }
    }

    /**
     * Gets the list of validation warnings.
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Gets the list of validation errors.
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
