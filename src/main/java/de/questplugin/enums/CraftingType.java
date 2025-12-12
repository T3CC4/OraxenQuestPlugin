package de.questplugin.enums;

/**
 * ENTFERNT - Nicht mehr benötigt
 * Anvil ist jetzt direkt in CraftingManager integriert
 */
@Deprecated
public enum CraftingType {
    ANVIL("Amboss");

    private final String displayName;

    CraftingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}