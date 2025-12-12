package de.questplugin.utils;

import de.questplugin.enums.MobType;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper-Klasse für Mob-Operationen
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
     */
    public static String getFormattedMobList() {
        StringBuilder sb = new StringBuilder();

        sb.append(ChatColor.RED + "=== HOSTILE ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.HOSTILE, ChatColor.RED);

        sb.append("\n");
        sb.append(ChatColor.DARK_RED + "=== ILLAGER ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.ILLAGER, ChatColor.DARK_RED);

        sb.append("\n");
        sb.append(ChatColor.DARK_PURPLE + "=== NETHER ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.NETHER, ChatColor.DARK_PURPLE);

        sb.append("\n");
        sb.append(ChatColor.LIGHT_PURPLE + "=== END ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.END, ChatColor.LIGHT_PURPLE);

        sb.append("\n");
        sb.append(ChatColor.GOLD + "=== BOSS ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.BOSS, ChatColor.GOLD);

        sb.append("\n");
        sb.append(ChatColor.GREEN + "=== PASSIVE ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.PASSIVE, ChatColor.GREEN);

        sb.append("\n");
        sb.append(ChatColor.AQUA + "=== AQUATIC ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.AQUATIC, ChatColor.AQUA);

        sb.append("\n");
        sb.append(ChatColor.YELLOW + "=== NEUTRAL ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.NEUTRAL, ChatColor.YELLOW);

        sb.append("\n");
        sb.append(ChatColor.BLUE + "=== TAMEABLE ===" + ChatColor.RESET + "\n");
        appendCategory(sb, MobType.MobCategory.TAMEABLE, ChatColor.BLUE);

        return sb.toString();
    }

    /**
     * Fügt Kategorie zur formatierten Liste hinzu
     */
    private static void appendCategory(StringBuilder sb, MobType.MobCategory category, ChatColor color) {
        MobType[] mobs = MobType.getByCategory(category);
        if (mobs.length == 0) return;

        sb.append(color).append(category.getDisplayName()).append(": ")
                .append(ChatColor.WHITE);

        String mobList = Arrays.stream(mobs)
                .map(m -> m.getEntityType().name().toLowerCase())
                .distinct()
                .collect(Collectors.joining(", "));

        sb.append(mobList).append("\n");
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
     */
    public static String getMobInfo(MobType mobType) {
        return ChatColor.YELLOW + mobType.getDisplayName() +
                ChatColor.GRAY + " (" + mobType.getCategory().getDisplayName() + ")";
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