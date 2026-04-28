package dev.auctify.gui;

import org.bukkit.Material;

/**
 * Represents a GUI theme with custom materials and styling.
 */
public class GUITheme {

    private final String name;
    private final String title;
    private final Material fillerMaterial;
    private final Material borderMaterial;
    private final Material highlightMaterial;
    private final Material dangerMaterial;
    private final Material infoMaterial;

    public GUITheme(String name, String title, Material fillerMaterial, Material borderMaterial,
                    Material highlightMaterial, Material dangerMaterial, Material infoMaterial) {
        this.name = name;
        this.title = title;
        this.fillerMaterial = fillerMaterial;
        this.borderMaterial = borderMaterial;
        this.highlightMaterial = highlightMaterial;
        this.dangerMaterial = dangerMaterial;
        this.infoMaterial = infoMaterial;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public Material getFillerMaterial() {
        return fillerMaterial;
    }

    public Material getBorderMaterial() {
        return borderMaterial;
    }

    public Material getHighlightMaterial() {
        return highlightMaterial;
    }

    public Material getDangerMaterial() {
        return dangerMaterial;
    }

    public Material getInfoMaterial() {
        return infoMaterial;
    }
}
