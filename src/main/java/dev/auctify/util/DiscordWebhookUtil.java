package dev.auctify.util;

import dev.auctify.Auctify;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility for sending Discord webhook embeds. All HTTP calls are performed
 * asynchronously to avoid blocking the main thread.
 */
public class DiscordWebhookUtil {

    private final Auctify plugin;

    public DiscordWebhookUtil(Auctify plugin) {
        this.plugin = plugin;
    }

    /** Sends an embed for a newly created listing. */
    public void sendNewListingEmbed(String seller, String item, String startPrice, String buyoutPrice) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("discord.enabled", false))
            return;
        if (!config.getBoolean("discord.on-new-listing.enabled", true))
            return;

        String webhookUrl = getWebhookUrl(config);
        if (webhookUrl == null)
            return;

        String title = config.getString("discord.on-new-listing.title", "\uD83C\uDFF7\uFE0F New Auction Listing!");
        int color = config.getInt("discord.on-new-listing.color", 3447003);

        String jsonPayload = buildJson(title, color, seller,
                field("Seller", seller, true),
                field("Item", item, true),
                field("Start Price", startPrice, true),
                field("Buyout", buyoutPrice, true));

        sendAsync(webhookUrl, jsonPayload);
    }

    /** Sends an embed for a successfully sold listing. */
    public void sendSoldEmbed(String seller, String winner, String item, String finalPrice) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("discord.enabled", false))
            return;
        if (!config.getBoolean("discord.on-sale.enabled", true))
            return;

        String webhookUrl = getWebhookUrl(config);
        if (webhookUrl == null)
            return;

        String title = config.getString("discord.on-sale.title", "\uD83D\uDCB0 Item Sold!");
        int color = config.getInt("discord.on-sale.color", 3066993);

        String jsonPayload = buildJson(title, color, seller,
                field("Seller", seller, true),
                field("Winner", winner, true),
                field("Item", item, true),
                field("Sold For", finalPrice, false));

        sendAsync(webhookUrl, jsonPayload);
    }

    /** Sends an embed for an expired listing (ended without bids). */
    public void sendExpiredEmbed(String seller, String item, String startPrice) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("discord.enabled", false))
            return;
        if (!config.getBoolean("discord.on-expire.enabled", true))
            return;

        String webhookUrl = getWebhookUrl(config);
        if (webhookUrl == null)
            return;

        String title = config.getString("discord.on-expire.title", "⏰ Auction Expired");
        int color = config.getInt("discord.on-expire.color", 10070709); // Gray

        String jsonPayload = buildJson(title, color, seller,
                field("Seller", seller, true),
                field("Item", item, true),
                field("Start Price", startPrice, true),
                field("Status", "No bids received", false));

        sendAsync(webhookUrl, jsonPayload);
    }

    /** Validates and returns the webhook URL, or null if invalid/placeholder. */
    private String getWebhookUrl(FileConfiguration config) {
        String url = config.getString("discord.webhook-url", "");
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        url = url.trim();
        // Block the default placeholder URL
        if (url.equals("https://discord.com/api/webhooks/...")) {
            return null;
        }
        // Basic validation
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            plugin.getLogger().warning("[Discord] Invalid webhook URL format. Must start with http or https.");
            return null;
        }
        return url;
    }

    /** Builds a JSON embed payload with player head as icon. */
    private String buildJson(String title, int color, String playerName, String... fields) {
        StringBuilder fieldsJson = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0)
                fieldsJson.append(",");
            fieldsJson.append(fields[i]);
        }

        String serverName = Bukkit.getServer().getName();
        String version = plugin.getPluginMeta().getVersion();
        String footerText = "🏛️ " + serverName + " • Auctify v" + version;

        // Use player head as icon (mc-heads.net - front facing avatar)
        String playerHeadUrl = "https://mc-heads.net/avatar/" + escapeJson(playerName) + "/128.png";

        return "{\"embeds\":[{"
                + "\"title\":\"" + escapeJson(title) + "\","
                + "\"color\":" + color + ","
                + "\"fields\":[" + fieldsJson + "],"
                + "\"footer\":{\"text\":\"" + escapeJson(footerText)
                + "\",\"icon_url\":\"" + playerHeadUrl + "\"},"
                + "\"timestamp\":\"" + java.time.Instant.now().toString() + "\","
                + "\"thumbnail\":{\"url\":\"" + playerHeadUrl + "\"}"
                + "}]}";
    }

    /** Creates a JSON field string. */
    private String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escapeJson(name) + "\",\"value\":\"" + escapeJson(value)
                + "\",\"inline\":" + inline + "}";
    }

    private void sendAsync(String webhookUrl, String jsonPayload) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("User-Agent", "Auctify-Plugin");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    plugin.getLogger().info("[Discord] Webhook sent successfully.");
                } else {
                    // Read error response for debugging
                    String errorBody = "";
                    try (InputStream errStream = conn.getErrorStream()) {
                        if (errStream != null) {
                            errorBody = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } catch (Exception e) {
                        // FIX Mi-1: Log error saat baca error stream gagal (bukan silent ignore)
                        plugin.getLogger()
                                .warning("[Auctify] Discord webhook: failed to read error stream: " + e.getMessage());
                    }
                    plugin.getLogger().warning("[Discord] Webhook failed! HTTP " + responseCode + ": " + errorBody);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[Discord] Error sending webhook: " + e.getMessage());
            }
        });
    }

    /** Sends a crash notification to the crash webhook. */
    public void sendCrashNotification(String timestamp, String exceptionType, String message) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("discord.crash-webhook.enabled", false))
            return;

        String webhookUrl = config.getString("discord.crash-webhook.url", "");
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || webhookUrl.contains("..."))
            return;

        String title = "🔥 Auctify Crash Detected";
        int color = 0xFF0000; // Red

        String jsonPayload = "{\"embeds\":[{\"title\":\"" + escapeJson(title) + "\","
                + "\"color\":" + color + ","
                + "\"fields\":["
                + "{\"name\":\"Timestamp\",\"value\":\"" + escapeJson(timestamp) + "\",\"inline\":true},"
                + "{\"name\":\"Exception\",\"value\":\"" + escapeJson(exceptionType) + "\",\"inline\":true},"
                + "{\"name\":\"Message\",\"value\":\"" + escapeJson(message) + "\",\"inline\":false}"
                + "],"
                + "\"footer\":{\"text\":\"Auctify Crash Handler\"}"
                + "}]}";

        sendAsync(webhookUrl, jsonPayload);
    }

    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t")
                .replace("§", ""); // Strip MC color codes
    }
}
