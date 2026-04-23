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
 * Utility for sending Discord webhook embeds.
 * All HTTP calls are performed asynchronously to avoid blocking the main thread.
 */
public class DiscordWebhookUtil {

    private final Auctify plugin;

    public DiscordWebhookUtil(Auctify plugin) {
        this.plugin = plugin;
    }

    /**
     * Sends an embed for a newly created listing.
     */
    public void sendNewListingEmbed(String seller, String item, String startPrice, String buyoutPrice) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("discord.enabled", false)) return;
        if (!config.getBoolean("discord.on-new-listing.enabled", true)) return;

        String webhookUrl = getWebhookUrl(config);
        if (webhookUrl == null) return;

        String title = config.getString("discord.on-new-listing.title", "\uD83C\uDFF7\uFE0F New Auction Listing!");
        int color = config.getInt("discord.on-new-listing.color", 3447003);

        String jsonPayload = buildJson(title, color,
                field("Seller", seller, true),
                field("Item", item, true),
                field("Start Price", startPrice, true),
                field("Buyout", buyoutPrice, true));

        sendAsync(webhookUrl, jsonPayload);
    }

    /**
     * Sends an embed for a successfully sold listing.
     */
    public void sendSoldEmbed(String seller, String winner, String item, String finalPrice) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("discord.enabled", false)) return;
        if (!config.getBoolean("discord.on-sale.enabled", true)) return;

        String webhookUrl = getWebhookUrl(config);
        if (webhookUrl == null) return;

        String title = config.getString("discord.on-sale.title", "\uD83D\uDCB0 Item Sold!");
        int color = config.getInt("discord.on-sale.color", 3066993);

        String jsonPayload = buildJson(title, color,
                field("Seller", seller, true),
                field("Winner", winner, true),
                field("Item", item, true),
                field("Sold For", finalPrice, false));

        sendAsync(webhookUrl, jsonPayload);
    }

    /**
     * Validates and returns the webhook URL, or null if invalid/placeholder.
     */
    private String getWebhookUrl(FileConfiguration config) {
        String url = config.getString("discord.webhook-url", "");
        if (url == null || url.isEmpty()) {
            return null;
        }
        // Block the default placeholder URL
        if (url.equals("https://discord.com/api/webhooks/...")) {
            return null;
        }
        // Basic validation
        if (!url.startsWith("https://discord.com/api/webhooks/") &&
            !url.startsWith("https://discordapp.com/api/webhooks/")) {
            plugin.getLogger().warning("[Discord] Invalid webhook URL format. Must start with https://discord.com/api/webhooks/");
            return null;
        }
        return url;
    }

    /**
     * Builds a JSON embed payload.
     */
    private String buildJson(String title, int color, String... fields) {
        StringBuilder fieldsJson = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) fieldsJson.append(",");
            fieldsJson.append(fields[i]);
        }
        return "{\"embeds\":[{\"title\":\"" + escapeJson(title) + "\",\"color\":" + color
                + ",\"fields\":[" + fieldsJson + "],"
                + "\"footer\":{\"text\":\"Auctify Auction House\"},"
                + "\"timestamp\":\"" + java.time.Instant.now().toString() + "\""
                + "}]}";
    }

    /**
     * Creates a JSON field string.
     */
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
                    } catch (Exception ignored) {}
                    plugin.getLogger().warning("[Discord] Webhook failed! HTTP " + responseCode + ": " + errorBody);
                }
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("[Discord] Error sending webhook: " + e.getMessage());
            }
        });
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "")
                     .replace("\t", "\\t")
                     .replace("§", ""); // Strip MC color codes
    }
}
