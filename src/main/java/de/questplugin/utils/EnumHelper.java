package de.questplugin.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generischer Helper für Enum-Operationen
 * Reduziert Code-Duplikation zwischen BiomeHelper und MobHelper
 */
public class EnumHelper<T extends Enum<T>> {

    private final Class<T> enumClass;

    // Private Constructor
    private EnumHelper(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    /**
     * Factory-Methode zum Erstellen eines EnumHelpers
     */
    public static <T extends Enum<T>> EnumHelper<T> of(Class<T> enumClass) {
        return new EnumHelper<>(enumClass);
    }

    /**
     * Konvertiert String zu Enum (case-insensitive)
     */
    public T fromString(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Prüft ob String ein gültiger Enum-Wert ist
     */
    public boolean isValid(String name) {
        return fromString(name) != null;
    }

    /**
     * Gibt alle Enum-Namen zurück (lowercase)
     */
    public List<String> getAllNames() {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(e -> e.name().toLowerCase())
                .collect(Collectors.toList());
    }

    /**
     * Gibt Anzahl der Enum-Werte zurück
     */
    public int getCount() {
        return enumClass.getEnumConstants().length;
    }

    /**
     * Gibt alle Enum-Konstanten zurück
     */
    public T[] getAll() {
        return enumClass.getEnumConstants();
    }
}