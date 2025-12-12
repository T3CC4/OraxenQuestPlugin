package de.questplugin.utils;

import de.questplugin.enums.BiomeType;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper-Klasse für Biome-Operationen
 */
public class BiomeHelper {

    /**
     * Gibt alle verfügbaren Biome-Namen zurück (für Command-Tab-Complete)
     */
    public static List<String> getAllBiomeNames() {
        List<String> names = new ArrayList<>();
        for (BiomeType type : BiomeType.values()) {
            names.add(type.name().toLowerCase());
        }
        return names;
    }

    /**
     * Findet BiomeType anhand des Namens (case-insensitive)
     */
    public static BiomeType fromString(String name) {
        return BiomeType.fromString(name);
    }

    /**
     * Findet BiomeType anhand des Bukkit-Biomes
     */
    public static BiomeType fromBukkitBiome(Biome biome) {
        return BiomeType.fromBukkitBiome(biome);
    }

    /**
     * Prüft ob ein Biome-Name mit einem BiomeType matcht
     */
    public static boolean matchesBiome(String biomeName, BiomeType biomeType) {
        String normalized = biomeName.toLowerCase().replace("_", "");
        String typeNormalized = biomeType.name().toLowerCase().replace("_", "");

        // Exakter Match
        if (normalized.equals(typeNormalized)) {
            return true;
        }

        // Teilstring-Match
        return typeNormalized.contains(normalized) || normalized.contains(typeNormalized);
    }

    /**
     * Gibt formatierte Liste aller Biome aus (für Hilfe-Commands)
     */
    public static String getFormattedBiomeList() {
        StringBuilder sb = new StringBuilder();

        // Overworld
        sb.append(ChatColor.GREEN + "=== OVERWORLD ===" + ChatColor.RESET + "\n");
        appendCategory(sb, BiomeType.BiomeCategory.PLAINS, ChatColor.YELLOW);
        appendCategory(sb, BiomeType.BiomeCategory.FOREST, ChatColor.DARK_GREEN);
        appendCategory(sb, BiomeType.BiomeCategory.TAIGA, ChatColor.AQUA);
        appendCategory(sb, BiomeType.BiomeCategory.JUNGLE, ChatColor.GREEN);
        appendCategory(sb, BiomeType.BiomeCategory.DESERT, ChatColor.GOLD);
        appendCategory(sb, BiomeType.BiomeCategory.OCEAN, ChatColor.BLUE);
        appendCategory(sb, BiomeType.BiomeCategory.MOUNTAIN, ChatColor.GRAY);

        sb.append("\n");

        // Underground
        sb.append(ChatColor.DARK_GRAY + "=== UNDERGROUND ===" + ChatColor.RESET + "\n");
        appendCategory(sb, BiomeType.BiomeCategory.UNDERGROUND, ChatColor.DARK_PURPLE);

        sb.append("\n");

        // Nether
        sb.append(ChatColor.RED + "=== NETHER ===" + ChatColor.RESET + "\n");
        appendCategory(sb, BiomeType.BiomeCategory.NETHER, ChatColor.DARK_RED);

        sb.append("\n");

        // End
        sb.append(ChatColor.LIGHT_PURPLE + "=== END ===" + ChatColor.RESET + "\n");
        appendCategory(sb, BiomeType.BiomeCategory.END, ChatColor.DARK_PURPLE);

        return sb.toString();
    }

    /**
     * Fügt Kategorie zur formatierten Liste hinzu
     */
    private static void appendCategory(StringBuilder sb, BiomeType.BiomeCategory category, ChatColor color) {
        BiomeType[] biomes = BiomeType.getByCategory(category);
        if (biomes.length == 0) return;

        sb.append(color).append(category.getDisplayName()).append(": ")
                .append(ChatColor.WHITE);

        String biomeList = Arrays.stream(biomes)
                .map(b -> b.name().toLowerCase())
                .collect(Collectors.joining(", "));

        sb.append(biomeList).append("\n");
    }

    /**
     * Gibt alle Biome einer Dimension zurück
     */
    public static BiomeType[] getBiomesByDimension(String dimension) {
        switch (dimension.toLowerCase()) {
            case "overworld":
                return BiomeType.getOverworldBiomes();
            case "nether":
                return BiomeType.getNetherBiomes();
            case "end":
                return BiomeType.getEndBiomes();
            case "underground":
                return BiomeType.getUndergroundBiomes();
            default:
                return BiomeType.values();
        }
    }

    /**
     * Gibt alle Biome einer Kategorie zurück
     */
    public static BiomeType[] getBiomesByCategory(BiomeType.BiomeCategory category) {
        return BiomeType.getByCategory(category);
    }

    /**
     * Konvertiert BiomeType zu Bukkit Biome
     */
    public static Biome toBukkitBiome(BiomeType biomeType) {
        return biomeType.getBukkitBiome();
    }

    /**
     * Gibt Kurzinfo über ein Biome
     */
    public static String getBiomeInfo(BiomeType biomeType) {
        return ChatColor.YELLOW + biomeType.getDisplayName() +
                ChatColor.GRAY + " (" + biomeType.getCategory().getDisplayName() + ")" +
                ChatColor.DARK_GRAY + " - " + biomeType.getCategory().getDimension().getDisplayName();
    }

    /**
     * Prüft ob Biome zur Dimension gehört
     */
    public static boolean isInDimension(BiomeType biomeType, BiomeType.BiomeDimension dimension) {
        return biomeType.getCategory().getDimension() == dimension;
    }

    /**
     * Filtert Biome-Liste nach Dimension
     */
    public static List<BiomeType> filterByDimension(List<BiomeType> biomes, BiomeType.BiomeDimension dimension) {
        return biomes.stream()
                .filter(b -> isInDimension(b, dimension))
                .collect(Collectors.toList());
    }

    /**
     * Gibt alle Biome-Namen einer Kategorie zurück (für Tab-Complete)
     */
    public static List<String> getBiomeNamesByCategory(BiomeType.BiomeCategory category) {
        return Arrays.stream(BiomeType.getByCategory(category))
                .map(b -> b.name().toLowerCase())
                .collect(Collectors.toList());
    }

    /**
     * Gibt deutschen Namen eines Biomes zurück
     */
    public static String getGermanName(Biome biome) {
        BiomeType type = BiomeType.fromBukkitBiome(biome);
        return type != null ? type.getDisplayName() : biome.name();
    }

    /**
     * Erstellt Biome-Auswahl für Config (wie in raids.yml)
     */
    public static String getConfigExample() {
        return "# Biome-Auswahl:\n" +
                "# - Einzelne Biome: PLAINS, FOREST, DESERT\n" +
                "# - Alle Biome: [\"*\"]\n" +
                "# - Kategorie (manuell): PLAINS, SUNFLOWER_PLAINS, MEADOW\n" +
                "# - Dimension (manuell): NETHER_WASTES, CRIMSON_FOREST, WARPED_FOREST\n" +
                "#\n" +
                "# Nutze /quest biomes für komplette Liste";
    }

    /**
     * Validiert Biome-Namen aus Config
     */
    public static boolean isValidBiome(String biomeName) {
        if (biomeName == null || biomeName.isEmpty()) {
            return false;
        }

        // Wildcard erlauben
        if (biomeName.equals("*")) {
            return true;
        }

        return BiomeType.fromString(biomeName) != null;
    }

    /**
     * Konvertiert Config-String zu BiomeType
     */
    public static BiomeType parseConfigBiome(String biomeName) {
        if (!isValidBiome(biomeName)) {
            return null;
        }

        return BiomeType.fromString(biomeName);
    }
}