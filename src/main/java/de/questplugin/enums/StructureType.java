package de.questplugin.enums;

import org.bukkit.loot.LootTables;

/**
 * Alle Minecraft-Strukturen mit ihren zugehörigen LootTables.
 * Basierend auf Minecraft 1.20+
 */
public enum StructureType {

    // ========== OVERWORLD ==========

    // Unterwasser
    BURIED_TREASURE(LootTables.BURIED_TREASURE, "Vergrabener Schatz"),
    SHIPWRECK_MAP(LootTables.SHIPWRECK_MAP, "Schiffswrack (Karte)"),
    SHIPWRECK_SUPPLY(LootTables.SHIPWRECK_SUPPLY, "Schiffswrack (Vorräte)"),
    SHIPWRECK_TREASURE(LootTables.SHIPWRECK_TREASURE, "Schiffswrack (Schatz)"),
    OCEAN_RUIN_COLD(LootTables.UNDERWATER_RUIN_SMALL, "Ozeanruine (Kalt)"),
    OCEAN_RUIN_WARM(LootTables.UNDERWATER_RUIN_BIG, "Ozeanruine (Warm)"),

    // Wüste
    DESERT_PYRAMID(LootTables.DESERT_PYRAMID, "Wüstentempel"),

    // Dschungel
    JUNGLE_TEMPLE(LootTables.JUNGLE_TEMPLE, "Dschungeltempel"),
    JUNGLE_TEMPLE_DISPENSER(LootTables.JUNGLE_TEMPLE_DISPENSER, "Dschungeltempel (Dispenser)"),

    // Dorf
    VILLAGE_WEAPONSMITH(LootTables.VILLAGE_WEAPONSMITH, "Dorf (Waffenschmied)"),
    VILLAGE_TOOLSMITH(LootTables.VILLAGE_TOOLSMITH, "Dorf (Werkzeugschmied)"),
    VILLAGE_ARMORER(LootTables.VILLAGE_ARMORER, "Dorf (Rüstungsschmied)"),
    VILLAGE_CARTOGRAPHER(LootTables.VILLAGE_CARTOGRAPHER, "Dorf (Kartograph)"),
    VILLAGE_MASON(LootTables.VILLAGE_MASON, "Dorf (Steinmetz)"),
    VILLAGE_SHEPHERD(LootTables.VILLAGE_SHEPHERD, "Dorf (Schäfer)"),
    VILLAGE_BUTCHER(LootTables.VILLAGE_BUTCHER, "Dorf (Metzger)"),
    VILLAGE_FLETCHER(LootTables.VILLAGE_FLETCHER, "Dorf (Pfeilmacher)"),
    VILLAGE_FISHER(LootTables.VILLAGE_FISHER, "Dorf (Fischer)"),
    VILLAGE_TANNERY(LootTables.VILLAGE_TANNERY, "Dorf (Gerber)"),
    VILLAGE_TEMPLE(LootTables.VILLAGE_TEMPLE, "Dorf (Tempel)"),
    VILLAGE_DESERT_HOUSE(LootTables.VILLAGE_DESERT_HOUSE, "Dorf (Wüstenhaus)"),
    VILLAGE_PLAINS_HOUSE(LootTables.VILLAGE_PLAINS_HOUSE, "Dorf (Ebenenhaus)"),
    VILLAGE_TAIGA_HOUSE(LootTables.VILLAGE_TAIGA_HOUSE, "Dorf (Taigahaus)"),
    VILLAGE_SNOWY_HOUSE(LootTables.VILLAGE_SNOWY_HOUSE, "Dorf (Schneehaus)"),
    VILLAGE_SAVANNA_HOUSE(LootTables.VILLAGE_SAVANNA_HOUSE, "Dorf (Savannenhaus)"),

    // Andere Strukturen
    MINESHAFT(LootTables.ABANDONED_MINESHAFT, "Minenschacht"),
    STRONGHOLD_CORRIDOR(LootTables.STRONGHOLD_CORRIDOR, "Festung (Korridor)"),
    STRONGHOLD_CROSSING(LootTables.STRONGHOLD_CROSSING, "Festung (Kreuzung)"),
    STRONGHOLD_LIBRARY(LootTables.STRONGHOLD_LIBRARY, "Festung (Bibliothek)"),
    WOODLAND_MANSION(LootTables.WOODLAND_MANSION, "Waldanwesen"),
    PILLAGER_OUTPOST(LootTables.PILLAGER_OUTPOST, "Plünderer-Außenposten"),
    IGLOO(LootTables.IGLOO_CHEST, "Iglu"),
    RUINED_PORTAL(LootTables.RUINED_PORTAL, "Ruiniertes Portal"),
    ANCIENT_CITY(LootTables.ANCIENT_CITY, "Antike Stadt"),
    ANCIENT_CITY_ICE_BOX(LootTables.ANCIENT_CITY_ICE_BOX, "Antike Stadt (Eisbox)"),
    TRAIL_RUINS_COMMON(LootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, "Trail Ruins (Häufig)"),
    TRAIL_RUINS_RARE(LootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE, "Trail Ruins (Selten)"),

    // ========== NETHER ==========

    NETHER_BRIDGE(LootTables.NETHER_BRIDGE, "Netherfestung"),
    BASTION_TREASURE(LootTables.BASTION_TREASURE, "Bastion (Schatz)"),
    BASTION_OTHER(LootTables.BASTION_OTHER, "Bastion (Andere)"),
    BASTION_BRIDGE(LootTables.BASTION_BRIDGE, "Bastion (Brücke)"),
    BASTION_HOGLIN_STABLE(LootTables.BASTION_HOGLIN_STABLE, "Bastion (Hoglin-Stall)"),

    // ========== END ==========

    END_CITY_TREASURE(LootTables.END_CITY_TREASURE, "Endstadt (Schatz)"),
    ;

    private final LootTables lootTable;
    private final String displayName;

    StructureType(LootTables lootTable, String displayName) {
        this.lootTable = lootTable;
        this.displayName = displayName;
    }

    public LootTables getLootTable() {
        return lootTable;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gibt alle Overworld-Strukturen zurück
     */
    public static StructureType[] getOverworldStructures() {
        return new StructureType[] {
                BURIED_TREASURE, SHIPWRECK_MAP, SHIPWRECK_SUPPLY, SHIPWRECK_TREASURE,
                OCEAN_RUIN_COLD, OCEAN_RUIN_WARM, DESERT_PYRAMID, JUNGLE_TEMPLE,
                JUNGLE_TEMPLE_DISPENSER, VILLAGE_WEAPONSMITH, VILLAGE_TOOLSMITH,
                VILLAGE_ARMORER, VILLAGE_CARTOGRAPHER, VILLAGE_MASON, VILLAGE_SHEPHERD,
                VILLAGE_BUTCHER, VILLAGE_FLETCHER, VILLAGE_FISHER, VILLAGE_TANNERY,
                VILLAGE_TEMPLE, VILLAGE_DESERT_HOUSE, VILLAGE_PLAINS_HOUSE,
                VILLAGE_TAIGA_HOUSE, VILLAGE_SNOWY_HOUSE, VILLAGE_SAVANNA_HOUSE,
                MINESHAFT, STRONGHOLD_CORRIDOR, STRONGHOLD_CROSSING, STRONGHOLD_LIBRARY,
                WOODLAND_MANSION, PILLAGER_OUTPOST, IGLOO, RUINED_PORTAL,
                ANCIENT_CITY, ANCIENT_CITY_ICE_BOX, TRAIL_RUINS_COMMON, TRAIL_RUINS_RARE
        };
    }

    /**
     * Gibt alle Nether-Strukturen zurück
     */
    public static StructureType[] getNetherStructures() {
        return new StructureType[] {
                NETHER_BRIDGE, BASTION_TREASURE, BASTION_OTHER,
                BASTION_BRIDGE, BASTION_HOGLIN_STABLE
        };
    }

    /**
     * Gibt alle End-Strukturen zurück
     */
    public static StructureType[] getEndStructures() {
        return new StructureType[] {
                END_CITY_TREASURE
        };
    }
}