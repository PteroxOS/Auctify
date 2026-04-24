package dev.auctify.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for ItemStack serialization/deserialization and display name
 * extraction.
 * Uses Paper's ItemStack#serializeAsBytes() and ItemStack#deserializeBytes()
 * for reliable binary serialization stored as Base64 strings in the database.
 * Uses Adventure API for display names to avoid deprecated Bukkit methods.
 */
public final class ItemUtil {

    /** Logger for serialization errors. */
    private static final Logger LOGGER = Logger.getLogger("Auctify");

    /** Legacy serializer for extracting plain text from display names. */
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    /** Private constructor to prevent instantiation of this utility class. */
    private ItemUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Serializes an ItemStack into a Base64-encoded string using Paper's binary
     * serialization.
     */
    public static String serializeToBase64(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("Cannot serialize a null or AIR ItemStack.");
        }
        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** Deserializes an ItemStack from a Base64-encoded string. */
    public static ItemStack deserializeFromBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            LOGGER.warning("Attempted to deserialize a null or empty Base64 string.");
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize ItemStack from Base64.", e);
            return null;
        }
    }

    /**
     * Gets a human-readable display name for an ItemStack. Uses Adventure API's
     * displayName() to avoid deprecated methods.
     */
    public static String getDisplayName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            String msg = MessageUtil.getMessage("unknown-item");
            return msg.isEmpty() ? "Unknown Item" : msg;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            // Use Adventure API to get the display name as plain text
            Component displayComponent = meta.displayName();
            if (displayComponent != null) {
                return LEGACY_SERIALIZER.serialize(displayComponent);
            }
        }

        // Format the material name: DIAMOND_SWORD → Diamond Sword
        return formatMaterialName(item.getType());
    }

    /** Formats a Material enum name into a human-readable string. */
    public static String formatMaterialName(Material material) {
        String name = material.name().toLowerCase();
        String[] words = name.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }

    /** Checks whether an ItemStack is null or represents an empty/air slot. */
    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
