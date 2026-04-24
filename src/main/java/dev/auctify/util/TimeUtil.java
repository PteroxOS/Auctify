package dev.auctify.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for formatting time durations into human-readable strings. Uses
 * the configurable time format pattern from config.yml so admins can customize
 * how remaining time appears in the auction GUI.
 */
public final class TimeUtil {

    /** Private constructor to prevent instantiation of this utility class. */
    private TimeUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Formats a duration in seconds into a human-readable time string using the
     * pattern defined in display.time-format of config.yml. The format supports
     * placeholders: {h} for hours, {m} for minutes, {s} for seconds.
     */
    public static String formatSeconds(long seconds, FileConfiguration config) {
        // Clamp negative values to zero for safety
        if (seconds < 0) {
            seconds = 0;
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        // Read the format pattern fresh from config each time (supports hot-reload)
        String format = config.getString("display.time-format", "§c{m}m {s}s");

        // Replace placeholders with computed values
        format = format.replace("{h}", String.valueOf(hours));
        format = format.replace("{m}", String.valueOf(minutes));
        format = format.replace("{s}", String.valueOf(secs));

        return format;
    }

    /**
     * Formats a duration in seconds into a simple "Xh Ym Zs" string without relying
     * on config. Used for logging and internal purposes.
     */
    public static String formatSimple(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(secs).append("s");

        return sb.toString().trim();
    }
}
