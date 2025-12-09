package de.questplugin.utils;

import de.questplugin.enums.StructureType;
import org.bukkit.loot.LootTables;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper-Klasse für Struktur-LootTable Operationen
 */
public class StructureHelper {

    /**
     * Gibt alle verfügbaren Struktur-Namen zurück (für Command-Tab-Complete)
     */
    public static List<String> getAllStructureNames() {
        List<String> names = new ArrayList<>();
        for (StructureType type : StructureType.values()) {
            names.add(type.name().toLowerCase());
        }
        return names;
    }

    /**
     * Findet StructureType anhand des Namens (case-insensitive)
     */
    public static StructureType fromString(String name) {
        try {
            return StructureType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Prüft ob ein LootTable-Key zu einer Struktur passt
     */
    public static boolean matchesStructure(String lootTableKey, StructureType structure) {
        String structureName = structure.name().toLowerCase();
        String key = lootTableKey.toLowerCase();

        // Exakter Match
        if (key.equals(structureName)) {
            return true;
        }

        // Teilstring-Match (z.B. "village" matched "village_weaponsmith")
        return structureName.contains(key) || key.contains(structureName);
    }

    /**
     * Gibt formatierte Liste aller Strukturen aus (für Hilfe-Commands)
     */
    public static String getFormattedStructureList() {
        StringBuilder sb = new StringBuilder();

        sb.append("§6=== OVERWORLD ===§r\n");
        sb.append("§eUnterwasser:§r BURIED_TREASURE, SHIPWRECK_*, OCEAN_RUIN_*\n");
        sb.append("§eWüste:§r DESERT_PYRAMID\n");
        sb.append("§eDschungel:§r JUNGLE_TEMPLE\n");
        sb.append("§eDörfer:§r VILLAGE_* (WEAPONSMITH, TOOLSMITH, etc.)\n");
        sb.append("§eAndere:§r MINESHAFT, STRONGHOLD_*, WOODLAND_MANSION, ANCIENT_CITY\n\n");

        sb.append("§c=== NETHER ===§r\n");
        sb.append("§eStructuren:§r NETHER_BRIDGE, BASTION_*\n\n");

        sb.append("§d=== END ===§r\n");
        sb.append("§eStructuren:§r END_CITY_TREASURE\n");

        return sb.toString();
    }

    /**
     * Gibt alle Strukturen einer Dimension zurück
     */
    public static StructureType[] getStructuresByDimension(String dimension) {
        switch (dimension.toLowerCase()) {
            case "overworld":
                return StructureType.getOverworldStructures();
            case "nether":
                return StructureType.getNetherStructures();
            case "end":
                return StructureType.getEndStructures();
            default:
                return StructureType.values();
        }
    }

    /**
     * Konvertiert StructureType zu LootTable Key String
     */
    public static String toLootTableKey(StructureType structure) {
        return structure.getLootTable().getKey().getKey();
    }
}