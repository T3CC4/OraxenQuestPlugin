package de.questplugin.utils;

import de.questplugin.enums.MobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper-Klasse für Mob-Operationen
 *
 * PAPER OPTIMIZATION:
 * - Adventure Components statt ChatColor
 */
public class MobHelper {

    /**
     * Gibt alle verfügbaren Mob-Namen zurück (für Command-Tab-Complete)
     */
    public static List<String> getAllMobNames() {
        List<String> names = new ArrayList<>();
        for (MobType type : MobType.values()) {
            names.add(type.name().toLowerCase());
        }
        return names;
    }

    /**
     * Findet MobType anhand des Namens (case-insensitive)
     */
    public static MobType fromString(String name) {
        return MobType.fromString(name);
    }

    /**
     * Findet MobType anhand des EntityTypes
     */
    public static MobType fromEntityType(EntityType entityType) {
        return MobType.fromEntityType(entityType);
    }

    /**
     * Gibt formatierte Liste aller Mobs aus (für Hilfe-Commands)
     *
     * PAPER: Nutzt Adventure Components statt ChatColor
     */
    public static Component getFormattedMobList() {
        return Component.text()
                // HOSTILE
                .append(Component.text("=== HOSTILE ===", NamedTextColor.RED, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.HOSTILE, NamedTextColor.RED))
                .appendNewline()

                // ILLAGER
                .append(Component.text("=== ILLAGER ===", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.ILLAGER, NamedTextColor.DARK_RED))
                .appendNewline()

                // NETHER
                .append(Component.text("=== NETHER ===", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.NETHER, NamedTextColor.DARK_PURPLE))
                .appendNewline()

                // END
                .append(Component.text("=== END ===", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.END, NamedTextColor.LIGHT_PURPLE))
                .appendNewline()

                // BOSS
                .append(Component.text("=== BOSS ===", NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.BOSS, NamedTextColor.GOLD))
                .appendNewline()

                // PASSIVE
                .append(Component.text("=== PASSIVE ===", NamedTextColor.GREEN, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.PASSIVE, NamedTextColor.GREEN))
                .appendNewline()

                // AQUATIC
                .append(Component.text("=== AQUATIC ===", NamedTextColor.AQUA, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.AQUATIC, NamedTextColor.AQUA))
                .appendNewline()

                // NEUTRAL
                .append(Component.text("=== NEUTRAL ===", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.NEUTRAL, NamedTextColor.YELLOW))
                .appendNewline()

                // TAMEABLE
                .append(Component.text("=== TAMEABLE ===", NamedTextColor.BLUE, TextDecoration.BOLD))
                .appendNewline()
                .append(buildCategoryComponent(MobType.MobCategory.TAMEABLE, NamedTextColor.BLUE))

                .build();
    }

    /**
     * Baut Component für eine Mob-Kategorie
     */
    private static Component buildCategoryComponent(MobType.MobCategory category, NamedTextColor color) {
        MobType[] mobs = MobType.getByCategory(category);
        if (mobs.length == 0) {
            return Component.empty();
        }

        String mobList = Arrays.stream(mobs)
                .map(m -> m.getEntityType().name().toLowerCase())
                .distinct()
                .collect(Collectors.joining(", "));

        return Component.text()
                .append(Component.text(category.getDisplayName() + ": ", color))
                .append(Component.text(mobList, NamedTextColor.WHITE))
                .build();
    }

    /**
     * Gibt alle Mobs einer Kategorie zurück
     */
    public static MobType[] getMobsByCategory(MobType.MobCategory category) {
        return MobType.getByCategory(category);
    }

    /**
     * Konvertiert MobType zu EntityType
     */
    public static EntityType toEntityType(MobType mobType) {
        return mobType.getEntityType();
    }

    /**
     * Gibt Kurzinfo über einen Mob
     *
     * PAPER: Als Adventure Component
     */
    public static Component getMobInfo(MobType mobType) {
        return Component.text()
                .append(Component.text(mobType.getDisplayName(), NamedTextColor.YELLOW))
                .appendSpace()
                .append(Component.text("(" + mobType.getCategory().getDisplayName() + ")", NamedTextColor.GRAY))
                .build();
    }

    /**
     * Prüft ob Mob feindlich ist
     */
    public static boolean isHostile(MobType mobType) {
        return mobType.isHostile();
    }

    /**
     * Prüft ob Mob friedlich ist
     */
    public static boolean isPassive(MobType mobType) {
        return mobType.isPassive();
    }

    /**
     * Gibt alle feindlichen Mobs zurück (für Tab-Complete)
     */
    public static List<String> getHostileMobNames() {
        return Arrays.stream(MobType.getHostileMobs())
                .map(m -> m.getEntityType().name().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Gibt alle friedlichen Mobs zurück (für Tab-Complete)
     */
    public static List<String> getPassiveMobNames() {
        return Arrays.stream(MobType.getPassiveMobs())
                .map(m -> m.getEntityType().name().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Gibt deutschen Namen eines Mobs zurück
     */
    public static String getGermanName(EntityType entityType) {
        MobType type = MobType.fromEntityType(entityType);
        return type != null ? type.getDisplayName() : entityType.name();
    }

    /**
     * Erstellt Mob-Auswahl für Config (wie in raids.yml)
     */
    public static String getConfigExample() {
        return "# Mob-Auswahl:\n" +
                "# type: ZOMBIE        # Mob-Typ\n" +
                "# amount: 10          # Anzahl\n" +
                "# health: 40.0        # Leben\n" +
                "# damage: 5.0         # Schaden\n" +
                "# custom-name: \"&cStarker Zombie\"\n" +
                "# use-equipment: true # Nutzt mob-equipment\n" +
                "#\n" +
                "# Nutze /quest mobs für komplette Liste";
    }

    /**
     * Validiert Mob-Namen aus Config
     */
    public static boolean isValidMob(String mobName) {
        if (mobName == null || mobName.isEmpty()) {
            return false;
        }

        try {
            EntityType.valueOf(mobName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Konvertiert Config-String zu EntityType (case-insensitive)
     */
    public static EntityType parseConfigMob(String mobName) {
        if (!isValidMob(mobName)) {
            return null;
        }

        try {
            return EntityType.valueOf(mobName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gibt empfohlene Mobs für Raids zurück
     */
    public static List<String> getRecommendedRaidMobs() {
        List<String> mobs = new ArrayList<>();

        // Hostile
        mobs.add("ZOMBIE");
        mobs.add("SKELETON");
        mobs.add("CREEPER");
        mobs.add("SPIDER");

        // Nether
        mobs.add("ZOMBIFIED_PIGLIN");
        mobs.add("BLAZE");
        mobs.add("WITHER_SKELETON");

        // Boss
        mobs.add("WITHER");
        mobs.add("WARDEN");

        return mobs;
    }
}