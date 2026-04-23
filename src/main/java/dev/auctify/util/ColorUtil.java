package dev.auctify.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Utility class for handling Minecraft color codes and Adventure components.
 * Provides methods to translate legacy color codes (§), strip colors, and
 * convert strings to Adventure {@link Component} instances.
 *
 * <p>Uses Adventure API exclusively to avoid deprecated Bukkit ChatColor.</p>
 */
public final class ColorUtil {

    /** The legacy section-sign serializer used for converting color-coded strings to Components. */
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    /** Serializer for stripping all formatting to plain text. */
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER =
            PlainTextComponentSerializer.plainText();

    /** Private constructor to prevent instantiation of this utility class. */
    private ColorUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Translates a legacy color-coded string (§ codes) by converting it to
     * an Adventure Component and back. Also handles &amp; codes.
     *
     * @param text the raw text potentially containing &amp; or § color codes
     * @return the translated string with valid Minecraft color codes, or empty string if null
     */
    public static String translate(String text) {
        if (text == null) {
            return "";
        }
        // Convert & codes to § codes
        text = text.replace('&', '§');
        return text;
    }

    /**
     * Strips all color codes (both § and &amp; based) from the given text
     * using Adventure's plain text serializer to avoid deprecated ChatColor.
     *
     * @param text the colored text to strip
     * @return the plain text without any color codes, or empty string if null
     */
    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        // Parse the legacy text to a Component, then serialize to plain text
        Component component = LEGACY_SERIALIZER.deserialize(translate(text));
        return PLAIN_SERIALIZER.serialize(component);
    }

    /**
     * Converts a legacy color-coded string (§ codes) into an Adventure {@link Component}.
     * This is the preferred way to send messages via Paper's Adventure API.
     *
     * @param text the legacy color-coded text
     * @return an Adventure Component representing the colored text
     */
    public static Component toComponent(String text) {
        if (text == null) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(text);
    }
}
