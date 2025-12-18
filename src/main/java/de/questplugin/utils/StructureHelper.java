package de.questplugin.utils;

import de.questplugin.enums.StructureType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.loot.LootTables;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper-Klasse für Struktur-LootTable Operationen
 *
 * PAPER OPTIMIZATION:
 * - Adventure Components statt ChatColor
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
     *
     * PAPER: Nutzt Adventure Components statt ChatColor
     */
    public static Component getFormattedStructureList() {
        return Component.text()
                // OVERWORLD
                .append(Component.text("=== OVERWORLD ===", NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()

                .append(Component.text("Unterwasser: ", NamedTextColor.YELLOW))
                .append(Component.text("BURIED_TREASURE, SHIPWRECK_*, OCEAN_RUIN_*", NamedTextColor.WHITE))
                .appendNewline()

                .append(Component.text("Wüste: ", NamedTextColor.YELLOW))
                .append(Component.text("DESERT_PYRAMID", NamedTextColor.WHITE))
                .appendNewline()

                .append(Component.text("Dschungel: ", NamedTextColor.YELLOW))
                .append(Component.text("JUNGLE_TEMPLE", NamedTextColor.WHITE))
                .appendNewline()

                .append(Component.text("Dörfer: ", NamedTextColor.YELLOW))
                .append(Component.text("VILLAGE_* (WEAPONSMITH, TOOLSMITH, etc.)", NamedTextColor.WHITE))
                .appendNewline()

                .append(Component.text("Andere: ", NamedTextColor.YELLOW))
                .append(Component.text("MINESHAFT, STRONGHOLD_*, WOODLAND_MANSION, ANCIENT_CITY", NamedTextColor.WHITE))
                .appendNewline()
                .appendNewline()

                // NETHER
                .append(Component.text("=== NETHER ===", NamedTextColor.RED, TextDecoration.BOLD))
                .appendNewline()

                .append(Component.text("Strukturen: ", NamedTextColor.YELLOW))
                .append(Component.text("NETHER_BRIDGE, BASTION_*", NamedTextColor.WHITE))
                .appendNewline()
                .appendNewline()

                // END
                .append(Component.text("=== END ===", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .appendNewline()

                .append(Component.text("Strukturen: ", NamedTextColor.YELLOW))
                .append(Component.text("END_CITY_TREASURE", NamedTextColor.WHITE))

                .build();
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