package de.questplugin.utils;

import de.questplugin.enums.BiomeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Registry;
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
     * Gibt formatierte Liste aller Biome aus (für Hilfe-Commands) - Paper Adventure API
     */
    public static Component getFormattedBiomeListComponent() {
        Component component = Component.empty();

        // Overworld
        component = component.append(Component.text("=== OVERWORLD ===", NamedTextColor.GREEN)).append(Component.newline());
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.PLAINS, NamedTextColor.YELLOW);
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.FOREST, NamedTextColor.DARK_GREEN);
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.TAIGA, NamedTextColor.AQUA);
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.JUNGLE, NamedTextColor.GREEN);
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.DESERT, NamedTextColor.GOLD);
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.OCEAN, NamedTextColor.BLUE);
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.MOUNTAIN, NamedTextColor.GRAY);

        component = component.append(Component.newline());

        // Underground
        component = component.append(Component.text("=== UNDERGROUND ===", NamedTextColor.DARK_GRAY)).append(Component.newline());
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.UNDERGROUND, NamedTextColor.DARK_PURPLE);

        component = component.append(Component.newline());

        // Nether
        component = component.append(Component.text("=== NETHER ===", NamedTextColor.RED)).append(Component.newline());
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.NETHER, NamedTextColor.DARK_RED);

        component = component.append(Component.newline());

        // End
        component = component.append(Component.text("=== END ===", TextColor.color(0xFFAAFF))).append(Component.newline());
        component = appendCategoryComponent(component, BiomeType.BiomeCategory.END, NamedTextColor.DARK_PURPLE);

        return component;
    }

    /**
     * Legacy-Methode für ChatColor (Rückwärtskompatibilität)
     */
    @Deprecated
    public static String getFormattedBiomeList() {
        StringBuilder sb = new StringBuilder();

        // Overworld
        sb.append("§a=== OVERWORLD ===§r\n");
        appendCategory(sb, BiomeType.BiomeCategory.PLAINS, "§e");
        appendCategory(sb, BiomeType.BiomeCategory.FOREST, "§2");
        appendCategory(sb, BiomeType.BiomeCategory.TAIGA, "§b");
        appendCategory(sb, BiomeType.BiomeCategory.JUNGLE, "§a");
        appendCategory(sb, BiomeType.BiomeCategory.DESERT, "§6");
        appendCategory(sb, BiomeType.BiomeCategory.OCEAN, "§9");
        appendCategory(sb, BiomeType.BiomeCategory.MOUNTAIN, "§7");

        sb.append("\n");

        // Underground
        sb.append("§8=== UNDERGROUND ===§r\n");
        appendCategory(sb, BiomeType.BiomeCategory.UNDERGROUND, "§5");

        sb.append("\n");

        // Nether
        sb.append("§c=== NETHER ===§r\n");
        appendCategory(sb, BiomeType.BiomeCategory.NETHER, "§4");

        sb.append("\n");

        // End
        sb.append("§d=== END ===§r\n");
        appendCategory(sb, BiomeType.BiomeCategory.END, "§5");

        return sb.toString();
    }

    /**
     * Fügt Kategorie zur Component hinzu
     */
    private static Component appendCategoryComponent(Component component, BiomeType.BiomeCategory category, TextColor color) {
        BiomeType[] biomes = BiomeType.getByCategory(category);
        if (biomes.length == 0) return component;

        String biomeList = Arrays.stream(biomes)
                .map(b -> b.name().toLowerCase())
                .collect(Collectors.joining(", "));

        return component
                .append(Component.text(category.getDisplayName() + ": ", color))
                .append(Component.text(biomeList, NamedTextColor.WHITE))
                .append(Component.newline());
    }

    /**
     * Legacy-Methode für StringBuilder
     */
    @Deprecated
    private static void appendCategory(StringBuilder sb, BiomeType.BiomeCategory category, String colorCode) {
        BiomeType[] biomes = BiomeType.getByCategory(category);
        if (biomes.length == 0) return;

        sb.append(colorCode).append(category.getDisplayName()).append(": ")
                .append("§f");

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
     * Gibt Kurzinfo über ein Biome als Component
     */
    public static Component getBiomeInfoComponent(BiomeType biomeType) {
        return Component.text(biomeType.getDisplayName(), NamedTextColor.YELLOW)
                .append(Component.text(" (" + biomeType.getCategory().getDisplayName() + ")", NamedTextColor.GRAY))
                .append(Component.text(" - " + biomeType.getCategory().getDimension().getDisplayName(), NamedTextColor.DARK_GRAY));
    }

    /**
     * Legacy-Methode für String
     */
    @Deprecated
    public static String getBiomeInfo(BiomeType biomeType) {
        return "§e" + biomeType.getDisplayName() +
                "§7 (" + biomeType.getCategory().getDisplayName() + ")" +
                "§8 - " + biomeType.getCategory().getDimension().getDisplayName();
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
        return type != null ? type.getDisplayName() : biome.getKey().getKey();
    }

    /**
     * Gibt alle registrierten Biome aus Paper Registry zurück
     */
    public static List<Biome> getAllRegisteredBiomes() {
        List<Biome> biomes = new ArrayList<>();
        Registry.BIOME.forEach(biomes::add);
        return biomes;
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