package de.questplugin.enums;

import org.bukkit.inventory.EquipmentSlot;

/**
 * Alle verfügbaren Equipment-Slots für Mob-Equipment
 */
public enum MobEquipmentSlot {
    MAIN_HAND(EquipmentSlot.HAND, "Haupthand"),
    OFF_HAND(EquipmentSlot.OFF_HAND, "Nebenhand"),
    HELMET(EquipmentSlot.HEAD, "Helm"),
    CHESTPLATE(EquipmentSlot.CHEST, "Brustpanzer"),
    LEGGINGS(EquipmentSlot.LEGS, "Hose"),
    BOOTS(EquipmentSlot.FEET, "Stiefel");

    private final EquipmentSlot bukkitSlot;
    private final String displayName;

    MobEquipmentSlot(EquipmentSlot bukkitSlot, String displayName) {
        this.bukkitSlot = bukkitSlot;
        this.displayName = displayName;
    }

    public EquipmentSlot getBukkitSlot() {
        return bukkitSlot;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Konvertiert String zu Enum (case-insensitive)
     */
    public static MobEquipmentSlot fromString(String name) {
        if (name == null) return null;

        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}