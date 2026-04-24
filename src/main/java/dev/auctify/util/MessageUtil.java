package dev.auctify.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

/**
 * Utility class for sending formatted messages to players.
 * Reads message templates from the locales/ folder based on the language
 * setting,
 * replaces placeholders in {key} format, prepends the configured prefix, and
 * sends the result as an Adventure Component.
 */
public final class MessageUtil {

    private static JavaPlugin plugin;
    private static FileConfiguration messagesConfig;

    private MessageUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /** Initializes the MessageUtil and loads the configured language file. */
    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        reload();
    }

    /** Reloads the language file based on the config setting. */
    public static void reload() {
        String lang = plugin.getConfig().getString("general.language", "en");
        File localesFolder = new File(plugin.getDataFolder(), "locales");
        if (!localesFolder.exists()) {
            localesFolder.mkdirs();
        }

        // Always extract bundled default languages so the user can see them
        String[] bundledLocales = { "en", "id" };
        for (String bundled : bundledLocales) {
            File f = new File(localesFolder, bundled + ".yml");
            if (!f.exists()) {
                try {
                    plugin.saveResource("locales/" + bundled + ".yml", false);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        File langFile = new File(localesFolder, lang + ".yml");
        if (!langFile.exists()) {
            try {
                langFile.createNewFile();
            } catch (Exception ignored) {
            }
        }
        messagesConfig = YamlConfiguration.loadConfiguration(langFile);

        // Auto-merge: inject any new keys from the bundled default into the user's file
        // This ensures new GUI keys added in updates appear automatically
        java.io.InputStream defaultStream = plugin.getResource("locales/" + lang + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : defaults.getKeys(false)) {
                if (!messagesConfig.contains(key)) {
                    messagesConfig.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try {
                    messagesConfig.save(langFile);
                    plugin.getLogger().info("Locale file '" + lang + ".yml' updated with new keys.");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save updated locale file: " + e.getMessage());
                }
            }
        }
    }

    /** Sends a message to a player using a key from the locale file. */
    public static void send(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null || plugin == null || messagesConfig == null)
            return;

        String prefix = plugin.getConfig().getString("general.prefix", "§8[§6Auctify§8] §r");
        String message = messagesConfig.getString(key, "");

        if (message.isEmpty()) {
            plugin.getLogger().warning("Missing message key in locales file: " + key);
            return;
        }

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        sender.sendMessage(ColorUtil.toComponent(prefix + message));
    }

    /** Retrieves a raw message string from the locale file. */
    public static String getMessage(String key) {
        if (plugin == null || messagesConfig == null)
            return "";

        return messagesConfig.getString(key, "");
    }

    /** Sends a raw string message to a player with the configured prefix. */
    public static void sendRaw(CommandSender sender, String message) {
        if (sender == null || plugin == null)
            return;
        String prefix = plugin.getConfig().getString("general.prefix", "§8[§6Auctify§8] §r");
        sender.sendMessage(ColorUtil.toComponent(prefix + message));
    }

    /**
     * Sends a message WITHOUT prefix (for UI formatting like borders). Use this for
     * wizard interfaces where prefix would break alignment.
     */
    public static void sendPlain(CommandSender sender, String message) {
        if (sender == null || plugin == null)
            return;
        sender.sendMessage(ColorUtil.toComponent(message));
    }

    /** Broadcasts a message to all online players using a locale key. */
    public static void broadcast(String key, Map<String, String> placeholders) {
        if (plugin == null || messagesConfig == null)
            return;

        String prefix = plugin.getConfig().getString("general.prefix", "§8[§6Auctify§8] §r");
        String message = messagesConfig.getString(key, "");

        if (message.isEmpty())
            return;

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        Component component = ColorUtil.toComponent(prefix + message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }

    /**
     * Returns a translated string from the locale file with placeholders replaced.
     * Used by GUI builders to fetch configurable display text.
     */
    public static String get(String key, Map<String, String> placeholders) {
        if (messagesConfig == null)
            return key;
        String message = messagesConfig.getString(key, key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    /** Returns a translated string with no placeholders. */
    public static String get(String key) {
        return get(key, null);
    }

    /** Broadcasts a raw message string to all online players with the prefix. */
    public static void broadcastRaw(String message) {
        if (plugin == null)
            return;
        String prefix = plugin.getConfig().getString("general.prefix", "§8[§6Auctify§8] §r");
        Component component = ColorUtil.toComponent(prefix + message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }
}
