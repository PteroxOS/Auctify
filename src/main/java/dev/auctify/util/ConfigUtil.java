package dev.auctify.util;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Logger;

/**
 * Type-safe wrapper around Bukkit's {@link FileConfiguration} for reading config values.
 * Logs warnings when a config value is missing or has an unexpected type, and always
 * returns a sensible default. All reads are performed at call-time (never cached) to
 * support hot-reload via {@code /ac reload}.
 */
public final class ConfigUtil {

    /** Reference to the main plugin instance for config and logger access. */
    private static JavaPlugin plugin;

    /** Private constructor to prevent instantiation of this utility class. */
    private ConfigUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Initializes the ConfigUtil with the plugin instance.
     * Must be called once during plugin startup.
     *
     * @param pluginInstance the main Auctify plugin instance
     */
    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * Gets a string value from config with a default fallback.
     *
     * @param path         the config path (e.g., "general.prefix")
     * @param defaultValue the default value if the path is missing
     * @return the config string value or the default
     */
    public static String getString(String path, String defaultValue) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path)) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        return config.getString(path, defaultValue);
    }

    /**
     * Gets an integer value from config with a default fallback.
     *
     * @param path         the config path
     * @param defaultValue the default value if the path is missing or wrong type
     * @return the config int value or the default
     */
    public static int getInt(String path, int defaultValue) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path)) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        // Bukkit's getInt handles type coercion from strings/doubles
        return config.getInt(path, defaultValue);
    }

    /**
     * Gets a double value from config with a default fallback.
     *
     * @param path         the config path
     * @param defaultValue the default value if the path is missing or wrong type
     * @return the config double value or the default
     */
    public static double getDouble(String path, double defaultValue) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path)) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        return config.getDouble(path, defaultValue);
    }

    /**
     * Gets a boolean value from config with a default fallback.
     *
     * @param path         the config path
     * @param defaultValue the default value if the path is missing or wrong type
     * @return the config boolean value or the default
     */
    public static boolean getBoolean(String path, boolean defaultValue) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path)) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        return config.getBoolean(path, defaultValue);
    }

    /**
     * Gets a long value from config with a default fallback.
     *
     * @param path         the config path
     * @param defaultValue the default value if the path is missing or wrong type
     * @return the config long value or the default
     */
    public static long getLong(String path, long defaultValue) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path)) {
            logMissing(path, defaultValue);
            return defaultValue;
        }
        return config.getLong(path, defaultValue);
    }

    /**
     * Gets a string list from config with an empty list as fallback.
     *
     * @param path the config path
     * @return the config string list, or an empty list if missing
     */
    public static List<String> getStringList(String path) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path)) {
            logMissing(path, "[]");
            return List.of();
        }
        return config.getStringList(path);
    }

    /**
     * Gets a {@link Material} from a config string value.
     * Logs a warning if the material name is invalid and returns the default.
     *
     * @param path         the config path containing the material name
     * @param defaultValue the default Material if the path is missing or invalid
     * @return the parsed Material or the default
     */
    public static Material getMaterial(String path, Material defaultValue) {
        String name = getString(path, defaultValue.name());
        Material material = Material.matchMaterial(name);
        if (material == null) {
            getLogger().warning("Invalid material '" + name + "' at config path '" + path
                    + "'. Using default: " + defaultValue.name());
            return defaultValue;
        }
        return material;
    }

    /**
     * Logs a warning about a missing config value.
     *
     * @param path         the missing config path
     * @param defaultValue the default value being used instead
     */
    private static void logMissing(String path, Object defaultValue) {
        getLogger().warning("Config value missing at '" + path + "'. Using default: " + defaultValue);
    }

    /**
     * Gets the plugin's logger. Falls back to the root "Auctify" logger if plugin is null.
     *
     * @return the logger instance
     */
    private static Logger getLogger() {
        return plugin != null ? plugin.getLogger() : Logger.getLogger("Auctify");
    }
}
