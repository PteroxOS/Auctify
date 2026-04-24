package dev.auctify.util;

import dev.auctify.Auctify;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages logging of all auction transactions and user activities. Logs are
 * saved to logs/ folder with daily rotation.
 */
public class LoggerManager {

    private final Auctify plugin;
    private final File logsFolder;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;

    public LoggerManager(Auctify plugin) {
        this.plugin = plugin;
        this.logsFolder = new File(plugin.getDataFolder(), "logs");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.timeFormat = new SimpleDateFormat("HH:mm:ss");

        // Create logs folder if it doesn't exist
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    /** Logs a new listing creation. */
    public void logListing(String playerName, String itemName, double startPrice, String listingId) {
        String message = String.format("[LISTING] %s listed %s for %.2f (ID: %s)",
                playerName, itemName, startPrice, listingId);
        writeLog(message);
    }

    /** Logs a bid placement. */
    public void logBid(String bidder, String seller, String item, double amount, String listingId) {
        String message = String.format("[BID] %s bid %.2f on %s's %s (ID: %s)",
                bidder, amount, seller, item, listingId);
        writeLog(message);
    }

    /** Logs a successful auction sale. */
    public void logSale(String seller, String buyer, String item, double price, String listingId) {
        String message = String.format("[SALE] %s bought %s from %s for %.2f (ID: %s)",
                buyer, item, seller, price, listingId);
        writeLog(message);
    }

    /** Logs an expired auction. */
    public void logExpired(String seller, String item, String listingId) {
        String message = String.format("[EXPIRED] %s's %s expired without bids (ID: %s)",
                seller, item, listingId);
        writeLog(message);
    }

    /** Logs an auction cancellation. */
    public void logCancel(String playerName, String item, String listingId) {
        String message = String.format("[CANCEL] %s cancelled listing for %s (ID: %s)",
                playerName, item, listingId);
        writeLog(message);
    }

    /** Logs a buyout purchase. */
    public void logBuyout(String buyer, String seller, String item, double price, String listingId) {
        String message = String.format("[BUYOUT] %s bought %s from %s for %.2f (ID: %s)",
                buyer, item, seller, price, listingId);
        writeLog(message);
    }

    /** Logs admin actions. */
    public void logAdmin(String admin, String action, String target) {
        String message = String.format("[ADMIN] %s %s %s", admin, action, target);
        writeLog(message);
    }

    /** Logs player claim actions. */
    public void logClaim(String player, String item, String type) {
        String message = String.format("[CLAIM] %s claimed %s (%s)", player, item, type);
        writeLog(message);
    }

    /** Writes a log message to the daily log file. */
    private void writeLog(String message) {
        String date = dateFormat.format(new Date());
        String time = timeFormat.format(new Date());
        File logFile = new File(logsFolder, "auctify-" + date + ".log");

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write("[" + time + "] " + message + System.lineSeparator());
        } catch (IOException e) {
            plugin.getLogger().warning("[LoggerManager] Failed to write log: " + e.getMessage());
        }
    }

    /** Gets the logs folder path. */
    public File getLogsFolder() {
        return logsFolder;
    }
}
