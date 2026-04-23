package dev.auctify.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages automatic downloading of plugin dependencies.
 * Dependencies are split into two categories:
 * <ul>
 *   <li><b>REQUIRED</b> — Plugin cannot function without these.
 *       If missing and download fails, the plugin will disable itself.</li>
 *   <li><b>OPTIONAL</b> — Extra features that enhance the plugin.
 *       If missing, plugin continues normally without those features.</li>
 * </ul>
 *
 * <p>Downloads missing jars from configured URLs into the server's plugins folder.
 * All downloads happen on the main thread during startup (before the server is
 * fully loaded) to ensure dependencies are available when needed.</p>
 *
 * <p>Controlled by the {@code dependencies.auto-download} config toggle.</p>
 */
public class DependencyManager {

    /** All known dependencies with metadata. */
    private static final Map<String, DependencyInfo> KNOWN_DEPS = new LinkedHashMap<>();

    static {
        // ═══════════════════════════════════════════
        //  REQUIRED DEPENDENCIES
        //  Plugin will DISABLE if these are missing
        // ═══════════════════════════════════════════
        KNOWN_DEPS.put("Vault", new DependencyInfo(
                "Vault",
                "Vault.jar",
                "https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar",
                "Economy abstraction layer (required for all transactions)",
                true // required
        ));

        // ═══════════════════════════════════════════
        //  OPTIONAL DEPENDENCIES
        //  Plugin works without these, but features are limited
        // ═══════════════════════════════════════════
        KNOWN_DEPS.put("PlaceholderAPI", new DependencyInfo(
                "PlaceholderAPI",
                "PlaceholderAPI.jar",
                "https://github.com/PlaceholderAPI/PlaceholderAPI/releases/download/2.11.6/PlaceholderAPI-2.11.6.jar",
                "Provides %auctify_*% placeholders for scoreboards, tab, etc.",
                false // optional
        ));
        KNOWN_DEPS.put("ProtocolLib", new DependencyInfo(
                "ProtocolLib",
                "ProtocolLib.jar",
                "https://github.com/dmulloy2/ProtocolLib/releases/download/5.3.0/ProtocolLib.jar",
                "Packet-level GUI protection (optional, future feature)",
                false // optional
        ));
    }

    private final JavaPlugin plugin;
    private final Logger logger;

    /**
     * Creates a new DependencyManager.
     *
     * @param plugin the main plugin instance
     */
    public DependencyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Checks for missing dependencies and downloads them if auto-download is enabled.
     * Should be called during {@code onEnable()} before any managers are initialized.
     *
     * @return true if all REQUIRED dependencies are present (or were downloaded),
     *         false if any REQUIRED dependency is missing and could not be obtained.
     *         When false is returned, the caller should disable the plugin.
     */
    public boolean checkAndDownload() {
        boolean autoDownload = plugin.getConfig().getBoolean("dependencies.auto-download", true);
        boolean allRequiredPresent = true;
        List<String> missingRequired = new ArrayList<>();
        List<String> missingOptional = new ArrayList<>();

        logger.info("");
        logger.info("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        logger.info("§e  ⚡ §7Dependency Check");
        logger.info("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (Map.Entry<String, DependencyInfo> entry : KNOWN_DEPS.entrySet()) {
            String name = entry.getKey();
            DependencyInfo info = entry.getValue();
            String tag = info.required() ? "§c[REQUIRED]" : "§7[OPTIONAL]";

            // Check if the plugin is already loaded on the server
            if (Bukkit.getPluginManager().getPlugin(name) != null) {
                logger.info("  §a✓ " + name + " §8— §aLoaded " + tag);
                continue;
            }

            // Check if the jar file exists in the plugins folder (maybe not loaded yet)
            File pluginsDir = plugin.getDataFolder().getParentFile();
            File jarFile = findJarFile(pluginsDir, name, info.fileName());

            if (jarFile != null && jarFile.exists()) {
                logger.info("  §e⟳ " + name + " §8— §eJar found, needs server restart " + tag);
                continue;
            }

            // Dependency is missing
            if (!autoDownload) {
                logger.warning("  §c✗ " + name + " §8— §cMissing! " + tag);
                logger.warning("    §7" + info.description());
                if (info.required()) {
                    missingRequired.add(name);
                    allRequiredPresent = false;
                } else {
                    missingOptional.add(name);
                }
                continue;
            }

            // Attempt to download
            logger.info("  §e⬇ " + name + " §8— §eDownloading... " + tag);
            File targetFile = new File(pluginsDir, info.fileName());
            boolean success = downloadFile(info.url(), targetFile);

            if (success) {
                logger.info("  §a✓ " + name + " §8— §aDownloaded! Restart to activate.");
            } else {
                logger.warning("  §c✗ " + name + " §8— §cDownload failed! " + tag);
                logger.warning("    §7Manual download: " + info.url());
                if (info.required()) {
                    missingRequired.add(name);
                    allRequiredPresent = false;
                } else {
                    missingOptional.add(name);
                }
            }
        }

        logger.info("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Print summary
        if (!missingRequired.isEmpty()) {
            logger.severe("");
            logger.severe("§c§l  ✘ MISSING REQUIRED DEPENDENCIES!");
            logger.severe("§c  The following plugins are REQUIRED for Auctify to work:");
            for (String dep : missingRequired) {
                DependencyInfo info = KNOWN_DEPS.get(dep);
                logger.severe("§c    • " + dep + " — " + info.description());
                logger.severe("§c      Download: " + info.url());
            }
            logger.severe("");
            logger.severe("§c  Auctify will now DISABLE itself.");
            logger.severe("§c  Install the missing dependencies and restart the server.");
            logger.severe("");
        }

        if (!missingOptional.isEmpty()) {
            logger.warning("");
            logger.warning("§e  ⚠ Missing optional dependencies (plugin will work without these):");
            for (String dep : missingOptional) {
                DependencyInfo info = KNOWN_DEPS.get(dep);
                logger.warning("§e    • " + dep + " — " + info.description());
            }
            logger.warning("");
        }

        return allRequiredPresent;
    }

    /**
     * Searches for a jar file by name, also checking for versioned filenames.
     * For example, finds "PlaceholderAPI-2.11.6.jar" when looking for "PlaceholderAPI".
     */
    private File findJarFile(File pluginsDir, String pluginName, String defaultFileName) {
        // Check exact filename first
        File exact = new File(pluginsDir, defaultFileName);
        if (exact.exists()) return exact;

        // Check for versioned filenames (e.g., "Vault-1.7.3.jar", "PlaceholderAPI-2.11.6.jar")
        File[] files = pluginsDir.listFiles((dir, name) ->
                name.toLowerCase().startsWith(pluginName.toLowerCase()) && name.endsWith(".jar"));
        if (files != null && files.length > 0) {
            return files[0];
        }

        return null;
    }

    /**
     * Downloads a file from a URL to a local destination.
     * Follows HTTP redirects (301, 302, 307, 308) up to 5 times.
     *
     * @param urlStr      the download URL
     * @param destination the local file to save to
     * @return true if the download was successful
     */
    private boolean downloadFile(String urlStr, File destination) {
        try {
            HttpURLConnection connection = openConnectionWithRedirects(urlStr, 5);

            if (connection == null) {
                logger.warning("Failed to establish connection to: " + urlStr);
                return false;
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warning("HTTP " + responseCode + " when downloading from: " + urlStr);
                connection.disconnect();
                return false;
            }

            long fileSize = connection.getContentLengthLong();
            String sizeStr = fileSize > 0 ? String.format("%.1f KB", fileSize / 1024.0) : "unknown size";
            logger.info("    Downloading " + sizeStr + "...");

            // Stream the download to disk
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(destination)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalRead = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }

                logger.info("    Saved " + String.format("%.1f KB", totalRead / 1024.0)
                        + " to " + destination.getName());
            }

            connection.disconnect();
            return true;

        } catch (IOException e) {
            logger.log(Level.WARNING, "Download error: " + e.getMessage(), e);
            // Clean up partial download
            if (destination.exists()) {
                destination.delete();
            }
            return false;
        }
    }

    /**
     * Opens an HTTP connection, following redirects up to maxRedirects times.
     * GitHub releases use redirects (302 → S3), so this is essential.
     *
     * @param urlStr       the URL to connect to
     * @param maxRedirects maximum number of redirects to follow
     * @return the final HttpURLConnection, or null if too many redirects
     * @throws IOException if a connection error occurs
     */
    private HttpURLConnection openConnectionWithRedirects(String urlStr, int maxRedirects) throws IOException {
        for (int i = 0; i < maxRedirects; i++) {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Auctify-Plugin/" + plugin.getPluginMeta().getVersion());
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false); // Handle redirects manually

            int code = conn.getResponseCode();

            if (code == HttpURLConnection.HTTP_OK) {
                return conn;
            }

            // Follow redirect
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == 307 || code == 308) {
                String redirectUrl = conn.getHeaderField("Location");
                conn.disconnect();

                if (redirectUrl == null || redirectUrl.isEmpty()) {
                    logger.warning("Redirect with no Location header from: " + urlStr);
                    return null;
                }

                urlStr = redirectUrl;
                continue;
            }

            // Non-redirect, non-OK response
            return conn;
        }

        logger.warning("Too many redirects for: " + urlStr);
        return null;
    }

    /**
     * Immutable record holding info about a downloadable dependency.
     *
     * @param pluginName  the Bukkit plugin name to check for
     * @param fileName    the jar filename to save as
     * @param url         the download URL
     * @param description a human-readable description
     * @param required    whether this dependency is required (true) or optional (false)
     */
    private record DependencyInfo(String pluginName, String fileName, String url,
                                   String description, boolean required) {
    }
}
