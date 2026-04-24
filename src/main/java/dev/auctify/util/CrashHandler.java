package dev.auctify.util;

import dev.auctify.Auctify;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles uncaught exceptions, logs them to crash.txt, and sends to Discord
 * webhook.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Auctify plugin;
    private final File crashFile;
    private Thread.UncaughtExceptionHandler previousHandler;

    public CrashHandler(Auctify plugin) {
        this.plugin = plugin;
        this.crashFile = new File(plugin.getDataFolder(), "crash.txt");
    }

    /** Registers this handler as the default uncaught exception handler. */
    public void register() {
        this.previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        plugin.getLogger().info("Crash handler registered.");
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String crashReport = generateCrashReport(timestamp, t, e);

        // Write to crash.txt
        writeCrashFile(crashReport);

        // Send to Discord webhook if configured
        sendToDiscord(timestamp, crashReport, e);

        // Log to console
        plugin.getLogger().severe("═══════════════════════════════════════════");
        plugin.getLogger().severe("CRASH DETECTED! Report saved to crash.txt");
        plugin.getLogger().severe("═══════════════════════════════════════════");
        e.printStackTrace();

        // Call previous handler if exists
        if (previousHandler != null && previousHandler != this) {
            previousHandler.uncaughtException(t, e);
        }
    }

    /** Generates a formatted crash report. */
    private String generateCrashReport(String timestamp, Thread thread, Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════════════════════╗\n");
        sb.append("║                    AUCTIFY CRASH REPORT                       ║\n");
        sb.append("╚═══════════════════════════════════════════════════════════════╝\n\n");
        sb.append("Timestamp: ").append(timestamp).append("\n");
        sb.append("Plugin Version: ").append(plugin.getPluginMeta().getVersion()).append("\n");
        sb.append("Server: ").append(Bukkit.getName()).append(" ").append(Bukkit.getVersion()).append("\n");
        sb.append("Java: ").append(System.getProperty("java.version")).append("\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version"))
                .append("\n");
        sb.append("Thread: ").append(thread.getName()).append(" (").append(thread.getId()).append(")\n\n");
        sb.append("Exception: ").append(error.getClass().getName()).append("\n");
        sb.append("Message: ").append(error.getMessage()).append("\n\n");
        sb.append("Stack Trace:\n");

        for (StackTraceElement element : error.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }

        Throwable cause = error.getCause();
        if (cause != null) {
            sb.append("\nCaused by: ").append(cause.getClass().getName()).append("\n");
            sb.append("Message: ").append(cause.getMessage()).append("\n\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("    at ").append(element.toString()).append("\n");
            }
        }

        sb.append("\n═══════════════════════════════════════════════════════════════\n");
        sb.append("Please report this at: https://github.com/PteroxOS/Auctify/issues\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        return sb.toString();
    }

    /** Writes the crash report to crash.txt file. */
    private void writeCrashFile(String report) {
        try (FileWriter writer = new FileWriter(crashFile, true)) {
            writer.write(report);
            writer.write("\n\n");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to write crash.txt: " + e.getMessage());
        }
    }

    /** Sends crash report to Discord webhook if configured. */
    private void sendToDiscord(String timestamp, String report, Throwable error) {
        if (!plugin.getConfig().getBoolean("discord.crash-webhook.enabled", false)) {
            return;
        }

        String webhookUrl = plugin.getConfig().getString("discord.crash-webhook.url", "");
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || webhookUrl.contains("...")) {
            return;
        }

        DiscordWebhookUtil webhookUtil = new DiscordWebhookUtil(plugin);
        webhookUtil.sendCrashNotification(timestamp, error.getClass().getSimpleName(), error.getMessage());
    }
}
