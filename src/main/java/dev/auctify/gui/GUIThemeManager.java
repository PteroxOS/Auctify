package dev.auctify.gui;

import dev.auctify.Auctify;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages GUI themes and provides theme materials for GUI rendering.
 */
public class GUIThemeManager {

    private final Auctify plugin;
    private final Map<String, GUITheme> themes = new HashMap<>();
    private String currentTheme;

    public GUIThemeManager(Auctify plugin) {
        this.plugin = plugin;
        loadThemes();
    }

    /**
     * Loads all themes from the configuration.
     */
    private void loadThemes() {
        var config = plugin.getConfig();
        currentTheme = config.getString("gui.theme", "default");

        ConfigurationSection themesSection = config.getConfigurationSection("gui.themes");
        if (themesSection == null) {
            plugin.getLogger().warning("No themes found in config. Using default theme.");
            createDefaultTheme();
            return;
        }

        for (String themeName : themesSection.getKeys(false)) {
            ConfigurationSection themeSection = themesSection.getConfigurationSection(themeName);
            if (themeSection == null) continue;

            String title = themeSection.getString("title", "§8✦ §6Auctify §8— §7Auction House §8✦");
            Material fillerMaterial = parseMaterial(themeSection.getString("filler-material", "GRAY_STAINED_GLASS_PANE"));
            Material borderMaterial = parseMaterial(themeSection.getString("border-material", "GRAY_STAINED_GLASS_PANE"));
            Material highlightMaterial = parseMaterial(themeSection.getString("highlight-material", "LIME_STAINED_GLASS_PANE"));
            Material dangerMaterial = parseMaterial(themeSection.getString("danger-material", "RED_STAINED_GLASS_PANE"));
            Material infoMaterial = parseMaterial(themeSection.getString("info-material", "BLUE_STAINED_GLASS_PANE"));

            GUITheme theme = new GUITheme(themeName, title, fillerMaterial, borderMaterial,
                    highlightMaterial, dangerMaterial, infoMaterial);
            themes.put(themeName.toLowerCase(), theme);
        }

        // Ensure default theme exists
        if (!themes.containsKey("default")) {
            createDefaultTheme();
        }

        plugin.getLogger().info("Loaded " + themes.size() + " GUI themes.");
    }

    /**
     * Creates a default theme if none exists in config.
     */
    private void createDefaultTheme() {
        GUITheme defaultTheme = new GUITheme("default",
                "§8✦ §6Auctify §8— §7Auction House §8✦",
                Material.GRAY_STAINED_GLASS_PANE,
                Material.GRAY_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.RED_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE);
        themes.put("default", defaultTheme);
    }

    /**
     * Parses a material string to Material enum.
     */
    private Material parseMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialName + ". Using GRAY_STAINED_GLASS_PANE.");
            return Material.GRAY_STAINED_GLASS_PANE;
        }
    }

    /**
     * Gets the current active theme.
     */
    public GUITheme getCurrentTheme() {
        return themes.get(currentTheme.toLowerCase());
    }

    /**
     * Gets a specific theme by name.
     */
    public GUITheme getTheme(String name) {
        return themes.get(name.toLowerCase());
    }

    /**
     * Sets the current theme.
     */
    public void setCurrentTheme(String themeName) {
        if (themes.containsKey(themeName.toLowerCase())) {
            this.currentTheme = themeName.toLowerCase();
        } else {
            plugin.getLogger().warning("Theme not found: " + themeName + ". Keeping current theme.");
        }
    }

    /**
     * Reloads themes from config.
     */
    public void reload() {
        themes.clear();
        loadThemes();
    }
}
