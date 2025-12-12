package de.questplugin.raid;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.utils.BiomeHelper;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Konfiguration eines Raids
 * Nutzt BiomeHelper für case-insensitive Biome-Namen
 */
public class RaidConfig {

    private final String id;
    private final String displayName;
    private final Set<Biome> allowedBiomes;
    private final int preparationTime; // Sekunden
    private final List<WaveConfig> waves;
    private final double difficultyMultiplier;
    private final int spawnRadius;
    private final RewardConfig rewards;

    private RaidConfig(String id, String displayName, Set<Biome> allowedBiomes,
                       int preparationTime, List<WaveConfig> waves,
                       double difficultyMultiplier, int spawnRadius,
                       RewardConfig rewards) {
        this.id = id;
        this.displayName = displayName;
        this.allowedBiomes = allowedBiomes;
        this.preparationTime = preparationTime;
        this.waves = waves;
        this.difficultyMultiplier = difficultyMultiplier;
        this.spawnRadius = spawnRadius;
        this.rewards = rewards;
    }

    /**
     * Lädt RaidConfig aus ConfigurationSection
     */
    public static RaidConfig load(String id, ConfigurationSection section, OraxenQuestPlugin plugin) {
        try {
            String displayName = section.getString("display-name", id);

            // Biome laden - CASE-INSENSITIVE mit BiomeHelper
            List<String> biomeStrings = section.getStringList("allowed-biomes");
            Set<Biome> allowedBiomes = new HashSet<>();

            if (biomeStrings.isEmpty() || biomeStrings.contains("*")) {
                // Alle Biome erlauben
                allowedBiomes.addAll(Arrays.asList(Biome.values()));
                plugin.getPluginLogger().debug("Raid '" + id + "': Alle Biome erlaubt");
            } else {
                for (String biomeStr : biomeStrings) {
                    // Normalisiere zu UPPER_CASE für Bukkit
                    String normalized = biomeStr.toUpperCase().replace(" ", "_");

                    try {
                        Biome biome = Biome.valueOf(normalized);
                        allowedBiomes.add(biome);
                        plugin.getPluginLogger().debug("Raid '" + id + "': Biom hinzugefügt: " + biome);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Raid '" + id + "': Ungültiges Biom '" + biomeStr +
                                "' (versucht: " + normalized + ")");
                        plugin.getLogger().warning("  Nutze /quest biomes für gültige Biom-Namen");
                    }
                }

                if (allowedBiomes.isEmpty()) {
                    plugin.getLogger().warning("Raid '" + id + "': Keine gültigen Biome konfiguriert!");
                    return null;
                }
            }

            int preparationTime = section.getInt("preparation-time", 10);
            double difficultyMultiplier = section.getDouble("difficulty-multiplier", 1.0);
            int spawnRadius = section.getInt("spawn-radius", 20);

            // Wellen laden
            List<WaveConfig> waves = new ArrayList<>();
            ConfigurationSection wavesSection = section.getConfigurationSection("waves");

            if (wavesSection != null) {
                for (String waveKey : wavesSection.getKeys(false)) {
                    ConfigurationSection waveSection = wavesSection.getConfigurationSection(waveKey);
                    if (waveSection != null) {
                        WaveConfig wave = WaveConfig.load(waveKey, waveSection, plugin);
                        if (wave != null) {
                            waves.add(wave);
                        }
                    }
                }
            }

            // Belohnungen laden
            RewardConfig rewards = RewardConfig.load(section.getConfigurationSection("rewards"), plugin);

            if (waves.isEmpty()) {
                plugin.getLogger().warning("Raid '" + id + "' hat keine Wellen!");
                return null;
            }

            plugin.getPluginLogger().info("Raid '" + id + "' geladen: " +
                    waves.size() + " Wellen, " +
                    allowedBiomes.size() + " Biome");

            return new RaidConfig(id, displayName, allowedBiomes, preparationTime,
                    waves, difficultyMultiplier, spawnRadius, rewards);

        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Laden von Raid '" + id + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ==================== GETTER ====================

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Biome> getAllowedBiomes() {
        return allowedBiomes;
    }

    public boolean isAllowedBiome(Biome biome) {
        return allowedBiomes.contains(biome);
    }

    public int getPreparationTime() {
        return preparationTime;
    }

    public List<WaveConfig> getWaves() {
        return waves;
    }

    public int getTotalWaves() {
        return waves.size();
    }

    public double getDifficultyMultiplier() {
        return difficultyMultiplier;
    }

    public int getSpawnRadius() {
        return spawnRadius;
    }

    public RewardConfig getRewards() {
        return rewards;
    }

    /**
     * Gibt deutschen Biom-Namen zurück
     */
    public String getBiomeDisplayNames() {
        if (allowedBiomes.size() >= Biome.values().length - 5) {
            return "Alle";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Biome biome : allowedBiomes) {
            if (count > 0) sb.append(", ");
            sb.append(BiomeHelper.getGermanName(biome));
            count++;
            if (count >= 5) {
                sb.append(" ...");
                break;
            }
        }
        return sb.toString();
    }
}