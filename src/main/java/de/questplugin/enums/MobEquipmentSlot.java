package de.questplugin.enums;

/**
 * Equipment-Slots für Mobs
 */
public enum MobEquipmentSlot {

    MAIN_HAND("Haupthand"),
    OFF_HAND("Nebenhand"),
    HELMET("Helm"),
    CHESTPLATE("Brustpanzer"),
    LEGGINGS("Hose"),
    BOOTS("Stiefel");

    private final String displayName;

    MobEquipmentSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse aus String (case-insensitive)
     */
    public static MobEquipmentSlot fromString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String normalized = input.toUpperCase().trim();

        // Direkte Matches
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Nicht gefunden, versuche Aliases
        }

        // Aliases
        switch (normalized) {
            case "HAND":
            case "MAINHAND":
            case "MAIN":
                return MAIN_HAND;

            case "OFFHAND":
            case "OFF":
            case "SECOND_HAND":
                return OFF_HAND;

            case "HEAD":
            case "HAT":
                return HELMET;

            case "CHEST":
            case "BODY":
            case "CHESTPLATE":
                return CHESTPLATE;

            case "LEGS":
            case "PANTS":
            case "LEGGING":
                return LEGGINGS;

            case "FEET":
            case "BOOT":
            case "SHOES":
                return BOOTS;

            default:
                return null;
        }
    }

    /**
     * Prüft ob der Slot eine Rüstung ist
     */
    public boolean isArmor() {
        return this == HELMET || this == CHESTPLATE ||
                this == LEGGINGS || this == BOOTS;
    }

    /**
     * Prüft ob der Slot eine Waffe/Werkzeug sein kann
     */
    public boolean isWeapon() {
        return this == MAIN_HAND || this == OFF_HAND;
    }
}