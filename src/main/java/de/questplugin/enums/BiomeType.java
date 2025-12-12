package de.questplugin.enums;

import org.bukkit.block.Biome;

/**
 * Alle Minecraft-Biome für Version 1.21.10
 * Kategorisiert nach Dimensionen und Typ
 */
public enum BiomeType {

    // ========== OVERWORLD - PLAINS & FIELDS ==========
    PLAINS(Biome.PLAINS, BiomeCategory.PLAINS, "Ebene"),
    SUNFLOWER_PLAINS(Biome.SUNFLOWER_PLAINS, BiomeCategory.PLAINS, "Sonnenblumen-Ebene"),
    MEADOW(Biome.MEADOW, BiomeCategory.PLAINS, "Wiese"),
    SNOWY_PLAINS(Biome.SNOWY_PLAINS, BiomeCategory.SNOWY, "Verschneite Ebene"),

    // ========== OVERWORLD - FORESTS ==========
    FOREST(Biome.FOREST, BiomeCategory.FOREST, "Wald"),
    FLOWER_FOREST(Biome.FLOWER_FOREST, BiomeCategory.FOREST, "Blumenwald"),
    BIRCH_FOREST(Biome.BIRCH_FOREST, BiomeCategory.FOREST, "Birkenwald"),
    OLD_GROWTH_BIRCH_FOREST(Biome.OLD_GROWTH_BIRCH_FOREST, BiomeCategory.FOREST, "Alter Birkenwald"),
    DARK_FOREST(Biome.DARK_FOREST, BiomeCategory.FOREST, "Dichter Wald"),
    PALE_GARDEN(Biome.PALE_GARDEN, BiomeCategory.FOREST, "Blasser Garten"),

    // ========== OVERWORLD - TAIGA ==========
    TAIGA(Biome.TAIGA, BiomeCategory.TAIGA, "Taiga"),
    SNOWY_TAIGA(Biome.SNOWY_TAIGA, BiomeCategory.TAIGA, "Verschneite Taiga"),
    OLD_GROWTH_PINE_TAIGA(Biome.OLD_GROWTH_PINE_TAIGA, BiomeCategory.TAIGA, "Alter Kiefern-Taiga"),
    OLD_GROWTH_SPRUCE_TAIGA(Biome.OLD_GROWTH_SPRUCE_TAIGA, BiomeCategory.TAIGA, "Alter Fichten-Taiga"),

    // ========== OVERWORLD - JUNGLE ==========
    JUNGLE(Biome.JUNGLE, BiomeCategory.JUNGLE, "Dschungel"),
    SPARSE_JUNGLE(Biome.SPARSE_JUNGLE, BiomeCategory.JUNGLE, "Lichter Dschungel"),
    BAMBOO_JUNGLE(Biome.BAMBOO_JUNGLE, BiomeCategory.JUNGLE, "Bambusdschungel"),

    // ========== OVERWORLD - SAVANNA ==========
    SAVANNA(Biome.SAVANNA, BiomeCategory.SAVANNA, "Savanne"),
    SAVANNA_PLATEAU(Biome.SAVANNA_PLATEAU, BiomeCategory.SAVANNA, "Savannen-Plateau"),
    WINDSWEPT_SAVANNA(Biome.WINDSWEPT_SAVANNA, BiomeCategory.SAVANNA, "Windgepeitschte Savanne"),

    // ========== OVERWORLD - DESERT & BADLANDS ==========
    DESERT(Biome.DESERT, BiomeCategory.DESERT, "Wüste"),
    BADLANDS(Biome.BADLANDS, BiomeCategory.BADLANDS, "Tafelberge"),
    WOODED_BADLANDS(Biome.WOODED_BADLANDS, BiomeCategory.BADLANDS, "Bewaldete Tafelberge"),
    ERODED_BADLANDS(Biome.ERODED_BADLANDS, BiomeCategory.BADLANDS, "Erodierte Tafelberge"),

    // ========== OVERWORLD - SWAMP ==========
    SWAMP(Biome.SWAMP, BiomeCategory.SWAMP, "Sumpf"),
    MANGROVE_SWAMP(Biome.MANGROVE_SWAMP, BiomeCategory.SWAMP, "Mangrovensumpf"),

    // ========== OVERWORLD - MOUNTAINS ==========
    WINDSWEPT_HILLS(Biome.WINDSWEPT_HILLS, BiomeCategory.MOUNTAIN, "Windgepeitschte Hügel"),
    WINDSWEPT_GRAVELLY_HILLS(Biome.WINDSWEPT_GRAVELLY_HILLS, BiomeCategory.MOUNTAIN, "Windgepeitschte Kies-Hügel"),
    WINDSWEPT_FOREST(Biome.WINDSWEPT_FOREST, BiomeCategory.MOUNTAIN, "Windgepeitschter Wald"),
    GROVE(Biome.GROVE, BiomeCategory.MOUNTAIN, "Hain"),
    SNOWY_SLOPES(Biome.SNOWY_SLOPES, BiomeCategory.MOUNTAIN, "Verschneite Hänge"),
    FROZEN_PEAKS(Biome.FROZEN_PEAKS, BiomeCategory.MOUNTAIN, "Gefrorene Gipfel"),
    JAGGED_PEAKS(Biome.JAGGED_PEAKS, BiomeCategory.MOUNTAIN, "Zerklüftete Gipfel"),
    STONY_PEAKS(Biome.STONY_PEAKS, BiomeCategory.MOUNTAIN, "Steinige Gipfel"),

    // ========== OVERWORLD - OCEAN ==========
    OCEAN(Biome.OCEAN, BiomeCategory.OCEAN, "Ozean"),
    DEEP_OCEAN(Biome.DEEP_OCEAN, BiomeCategory.OCEAN, "Tiefer Ozean"),
    COLD_OCEAN(Biome.COLD_OCEAN, BiomeCategory.OCEAN, "Kalter Ozean"),
    DEEP_COLD_OCEAN(Biome.DEEP_COLD_OCEAN, BiomeCategory.OCEAN, "Tiefer kalter Ozean"),
    LUKEWARM_OCEAN(Biome.LUKEWARM_OCEAN, BiomeCategory.OCEAN, "Lauwarmer Ozean"),
    DEEP_LUKEWARM_OCEAN(Biome.DEEP_LUKEWARM_OCEAN, BiomeCategory.OCEAN, "Tiefer lauwarmer Ozean"),
    WARM_OCEAN(Biome.WARM_OCEAN, BiomeCategory.OCEAN, "Warmer Ozean"),
    FROZEN_OCEAN(Biome.FROZEN_OCEAN, BiomeCategory.OCEAN, "Gefrorener Ozean"),
    DEEP_FROZEN_OCEAN(Biome.DEEP_FROZEN_OCEAN, BiomeCategory.OCEAN, "Tiefer gefrorener Ozean"),

    // ========== OVERWORLD - BEACH & SHORE ==========
    BEACH(Biome.BEACH, BiomeCategory.BEACH, "Strand"),
    SNOWY_BEACH(Biome.SNOWY_BEACH, BiomeCategory.BEACH, "Verschneiter Strand"),
    STONY_SHORE(Biome.STONY_SHORE, BiomeCategory.BEACH, "Steinige Küste"),

    // ========== OVERWORLD - RIVER ==========
    RIVER(Biome.RIVER, BiomeCategory.RIVER, "Fluss"),
    FROZEN_RIVER(Biome.FROZEN_RIVER, BiomeCategory.RIVER, "Gefrorener Fluss"),

    // ========== OVERWORLD - SPECIAL ==========
    MUSHROOM_FIELDS(Biome.MUSHROOM_FIELDS, BiomeCategory.SPECIAL, "Pilzland"),
    ICE_SPIKES(Biome.ICE_SPIKES, BiomeCategory.SPECIAL, "Eisstachel-Ebene"),
    CHERRY_GROVE(Biome.CHERRY_GROVE, BiomeCategory.SPECIAL, "Kirschhain"),

    // ========== UNDERGROUND ==========
    DRIPSTONE_CAVES(Biome.DRIPSTONE_CAVES, BiomeCategory.UNDERGROUND, "Tropfsteinhöhlen"),
    LUSH_CAVES(Biome.LUSH_CAVES, BiomeCategory.UNDERGROUND, "Üppige Höhlen"),
    DEEP_DARK(Biome.DEEP_DARK, BiomeCategory.UNDERGROUND, "Tiefes Dunkel"),

    // ========== NETHER ==========
    NETHER_WASTES(Biome.NETHER_WASTES, BiomeCategory.NETHER, "Nether-Ödland"),
    SOUL_SAND_VALLEY(Biome.SOUL_SAND_VALLEY, BiomeCategory.NETHER, "Seelensandtal"),
    CRIMSON_FOREST(Biome.CRIMSON_FOREST, BiomeCategory.NETHER, "Karmesinwald"),
    WARPED_FOREST(Biome.WARPED_FOREST, BiomeCategory.NETHER, "Wirrwald"),
    BASALT_DELTAS(Biome.BASALT_DELTAS, BiomeCategory.NETHER, "Basaltdeltas"),

    // ========== THE END ==========
    THE_END(Biome.THE_END, BiomeCategory.END, "Das Ende"),
    END_HIGHLANDS(Biome.END_HIGHLANDS, BiomeCategory.END, "End-Hochland"),
    END_MIDLANDS(Biome.END_MIDLANDS, BiomeCategory.END, "End-Mittelland"),
    END_BARRENS(Biome.END_BARRENS, BiomeCategory.END, "End-Ödland"),
    SMALL_END_ISLANDS(Biome.SMALL_END_ISLANDS, BiomeCategory.END, "Kleine End-Inseln"),

    // ========== SPECIAL ==========
    THE_VOID(Biome.THE_VOID, BiomeCategory.VOID, "Die Leere");

    private final Biome bukkitBiome;
    private final BiomeCategory category;
    private final String displayName;

    BiomeType(Biome bukkitBiome, BiomeCategory category, String displayName) {
        this.bukkitBiome = bukkitBiome;
        this.category = category;
        this.displayName = displayName;
    }

    public Biome getBukkitBiome() {
        return bukkitBiome;
    }

    public BiomeCategory getCategory() {
        return category;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Konvertiert String zu BiomeType (case-insensitive)
     */
    public static BiomeType fromString(String name) {
        if (name == null) return null;

        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Findet BiomeType anhand des Bukkit-Biomes
     */
    public static BiomeType fromBukkitBiome(Biome biome) {
        if (biome == null) return null;

        for (BiomeType type : values()) {
            if (type.bukkitBiome == biome) {
                return type;
            }
        }
        return null;
    }

    // ==================== KATEGORIEN ====================

    /**
     * Gibt alle Biome einer Kategorie zurück
     */
    public static BiomeType[] getByCategory(BiomeCategory category) {
        return java.util.Arrays.stream(values())
                .filter(b -> b.category == category)
                .toArray(BiomeType[]::new);
    }

    /**
     * Gibt alle Overworld-Biome zurück
     */
    public static BiomeType[] getOverworldBiomes() {
        return java.util.Arrays.stream(values())
                .filter(b -> b.category.getDimension() == BiomeDimension.OVERWORLD)
                .toArray(BiomeType[]::new);
    }

    /**
     * Gibt alle Nether-Biome zurück
     */
    public static BiomeType[] getNetherBiomes() {
        return getByCategory(BiomeCategory.NETHER);
    }

    /**
     * Gibt alle End-Biome zurück
     */
    public static BiomeType[] getEndBiomes() {
        return getByCategory(BiomeCategory.END);
    }

    /**
     * Gibt alle Underground-Biome zurück
     */
    public static BiomeType[] getUndergroundBiomes() {
        return getByCategory(BiomeCategory.UNDERGROUND);
    }

    /**
     * Prüft ob Biome in einer bestimmten Kategorie ist
     */
    public boolean isInCategory(BiomeCategory category) {
        return this.category == category;
    }

    // ==================== ENUMS ====================

    /**
     * Biome-Kategorien
     */
    public enum BiomeCategory {
        // Overworld
        PLAINS(BiomeDimension.OVERWORLD, "Ebenen"),
        FOREST(BiomeDimension.OVERWORLD, "Wälder"),
        TAIGA(BiomeDimension.OVERWORLD, "Taigas"),
        JUNGLE(BiomeDimension.OVERWORLD, "Dschungel"),
        SAVANNA(BiomeDimension.OVERWORLD, "Savannen"),
        DESERT(BiomeDimension.OVERWORLD, "Wüsten"),
        BADLANDS(BiomeDimension.OVERWORLD, "Tafelberge"),
        SWAMP(BiomeDimension.OVERWORLD, "Sümpfe"),
        MOUNTAIN(BiomeDimension.OVERWORLD, "Berge"),
        OCEAN(BiomeDimension.OVERWORLD, "Ozeane"),
        BEACH(BiomeDimension.OVERWORLD, "Strände"),
        RIVER(BiomeDimension.OVERWORLD, "Flüsse"),
        SNOWY(BiomeDimension.OVERWORLD, "Verschneit"),
        SPECIAL(BiomeDimension.OVERWORLD, "Spezial"),

        // Underground
        UNDERGROUND(BiomeDimension.UNDERGROUND, "Unterirdisch"),

        // Nether
        NETHER(BiomeDimension.NETHER, "Nether"),

        // End
        END(BiomeDimension.END, "End"),

        // Void
        VOID(BiomeDimension.VOID, "Leere");

        private final BiomeDimension dimension;
        private final String displayName;

        BiomeCategory(BiomeDimension dimension, String displayName) {
            this.dimension = dimension;
            this.displayName = displayName;
        }

        public BiomeDimension getDimension() {
            return dimension;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Dimensionen
     */
    public enum BiomeDimension {
        OVERWORLD("Oberwelt"),
        UNDERGROUND("Untergrund"),
        NETHER("Nether"),
        END("End"),
        VOID("Leere");

        private final String displayName;

        BiomeDimension(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}