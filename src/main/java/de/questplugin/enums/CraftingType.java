package de.questplugin.enums;

/**
 * Alle verfügbaren Crafting-Typen
 */
public enum CraftingType {
    ANVIL("Amboss"),
    SMITHING_TABLE("Schmiedetisch");

    private final String displayName;

    CraftingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}