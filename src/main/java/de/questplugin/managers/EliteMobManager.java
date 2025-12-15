package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.BiomeType;
import de.questplugin.enums.MobType;
import de.questplugin.mobs.api.CustomMob;
import de.questplugin.mobs.api.CustomMobAPI;
import de.questplugin.mobs.api.CustomMobBuilder;
import de.questplugin.utils.BiomeHelper;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Verwaltet Elite-Mobs die in bestimmten Biomen/Strukturen spawnen
 *
 * FIXES:
 * - Bessere Biom-Validierung mit BiomeHelper
 * - Unterstützt BiomeType Enum für sichere Namen
 * - Erweiterte Debug-Ausgaben
 * - Besseres Wildcard-Matching
 */
public class EliteMobManager extends BaseManager implements Listener {

    private final CustomMobAPI mobAPI;
    private final Map<String, EliteMobConfig> biomeElites = new ConcurrentHashMap<>();
    private final Map<String, EliteMobConfig> structureElites = new ConcurrentHashMap<>();

    public EliteMobManager(OraxenQuestPlugin plugin) {
        super(plugin);
        this.mobAPI = plugin.getCustomMobAPI();

        loadEliteMobs();

        // Registriere Listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        info("EliteMobManager initialisiert - " +
                biomeElites.size() + " Biom-Elites, " +
                structureElites.size() + " Struktur-Elites");
    }

    private void loadEliteMobs() {
        biomeElites.clear();
        structureElites.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("elite-mobs");
        if (section == null) {
            warn("Keine elite-mobs Sektion in Config!");
            warn("Elite-Mobs werden NICHT spawnen!");
            return;
        }

        // Biom-Elites
        ConfigurationSection biomeSection = section.getConfigurationSection("biomes");
        if (biomeSection != null) {
            loadBiomeElites(biomeSection);
        }

        // Struktur-Elites
        ConfigurationSection structureSection = section.getConfigurationSection("structures");
        if (structureSection != null) {
            loadStructureElites(structureSection);
        }

        info("Elite-Mobs: " + biomeElites.size() + " Biom-Configs, " +
                structureElites.size() + " Struktur-Configs geladen");
    }

    /**
     * Lädt Biom-Elites mit verbesserter Validierung
     */
    private void loadBiomeElites(ConfigurationSection section) {
        for (String biomeStr : section.getKeys(false)) {
            ConfigurationSection eliteSection = section.getConfigurationSection(biomeStr);
            if (eliteSection == null) continue;

            // VALIDIERE Biom-Namen mit BiomeHelper
            if (!validateBiomeConfig(biomeStr)) {
                warn("Ungültiger Biom-Name in elite-mobs.biomes: '" + biomeStr + "'");
                warn("  Nutze /quest biomes für gültige Biom-Namen");
                warn("  Beispiele: DEEP_DARK, deep_dark, forest, PLAINS");
                continue;
            }

            EliteMobConfig config = EliteMobConfig.load(biomeStr, eliteSection, plugin);
            if (config != null) {
                biomeElites.put(biomeStr.toLowerCase(), config);

                // Zeige welche Biome matched werden
                List<Biome> matchedBiomes = getMatchingBiomes(biomeStr);
                debug("Biom-Elite geladen: " + biomeStr + " (" + config.getMobType() + ")");
                debug("  Matched " + matchedBiomes.size() + " Biome:");

                if (matchedBiomes.size() <= 10) {
                    // Zeige alle wenn wenige
                    for (Biome biome : matchedBiomes) {
                        debug("    - " + biome.name() + " (" + BiomeHelper.getGermanName(biome) + ")");
                    }
                } else {
                    // Zeige nur erste 5
                    debug("    " + matchedBiomes.stream()
                            .limit(5)
                            .map(b -> b.name())
                            .collect(Collectors.joining(", ")) + " ... (+" + (matchedBiomes.size() - 5) + " mehr)");
                }
                debug("  Spawn-Chance: " + config.getSpawnChance() + "%");
                debug("  Mob-Typ: " + config.getMobType());
            }
        }
    }

    /**
     * Validiert Biom-Config-Namen
     */
    private boolean validateBiomeConfig(String biomeStr) {
        if (biomeStr == null || biomeStr.isEmpty()) {
            return false;
        }

        // Wildcard erlauben
        if (biomeStr.equals("*")) {
            return true;
        }

        // Versuche als BiomeType zu parsen
        BiomeType biomeType = BiomeHelper.fromString(biomeStr);
        if (biomeType != null) {
            return true;
        }

        // Versuche als Bukkit Biome
        try {
            Biome.valueOf(biomeStr.toUpperCase().replace(" ", "_"));
            return true;
        } catch (IllegalArgumentException e) {
            // Nicht gefunden
        }

        // Prüfe ob es ein Teilstring eines Biomes ist
        String normalized = biomeStr.toLowerCase().replace("_", "");
        for (Biome biome : Biome.values()) {
            String biomeName = biome.name().toLowerCase().replace("_", "");
            if (biomeName.contains(normalized) || normalized.contains(biomeName)) {
                return true; // Teilstring-Match ist ok
            }
        }

        return false;
    }

    /**
     * Holt alle Biome die zu einem Config-String matchen
     */
    private List<Biome> getMatchingBiomes(String biomeStr) {
        List<Biome> matched = new ArrayList<>();

        // Wildcard = alle
        if (biomeStr.equals("*")) {
            return Arrays.asList(Biome.values());
        }

        String normalized = biomeStr.toLowerCase().replace("_", "").replace(" ", "");

        for (Biome biome : Biome.values()) {
            String biomeName = biome.name().toLowerCase().replace("_", "");

            // Exakter Match
            if (biomeName.equals(normalized)) {
                matched.add(biome);
                continue;
            }

            // Teilstring-Match (z.B. "forest" matched "birch_forest")
            if (biomeName.contains(normalized) || normalized.contains(biomeName)) {
                matched.add(biome);
            }
        }

        return matched;
    }

    private void loadStructureElites(ConfigurationSection section) {
        for (String structureStr : section.getKeys(false)) {
            ConfigurationSection eliteSection = section.getConfigurationSection(structureStr);
            if (eliteSection == null) continue;

            EliteMobConfig config = EliteMobConfig.load(structureStr, eliteSection, plugin);
            if (config != null) {
                structureElites.put(structureStr.toLowerCase(), config);
                debug("Struktur-Elite geladen: " + structureStr);
            }
        }
    }

    /**
     * Event Handler für Mob-Spawning mit verbessertem Debug
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        // Nur natürliches Spawning
        if (!isNaturalSpawn(event.getSpawnReason())) {
            return;
        }

        LivingEntity entity = (LivingEntity) event.getEntity();
        Location location = entity.getLocation();
        Biome biome = location.getBlock().getBiome();
        EntityType mobType = entity.getType();

        // DEBUG: Zeige jeden Spawn-Versuch
        if (debugMode) {
            debug("=== Mob Spawn ===");
            debug("Typ: " + mobType);
            debug("Biom: " + biome + " (" + BiomeHelper.getGermanName(biome) + ")");
            debug("Grund: " + event.getSpawnReason());
            debug("Location: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        }

        // Prüfe Biom-Elites
        EliteMobConfig biomeConfig = findBiomeElite(biome, mobType);

        if (biomeConfig != null) {
            debug("Elite-Config gefunden: " + biomeConfig.getEliteName());
            debug("  Chance: " + biomeConfig.getSpawnChance() + "%");

            // Rolle für Spawn-Chance
            double roll = ThreadLocalRandom.current().nextDouble() * 100;
            debug("  Roll: " + String.format("%.2f", roll));

            if (roll < biomeConfig.getSpawnChance()) {
                debug("  → ERFOLG! Spawne Elite-Mob");

                // Event canceln und durch Elite ersetzen
                event.setCancelled(true);
                spawnEliteMob(location, biomeConfig);

                info("Elite-Mob gespawnt: " + biomeConfig.getEliteName() +
                        " @ " + biome + " (" + String.format("%.1f", roll) +
                        "/" + biomeConfig.getSpawnChance() + "%)");
            } else {
                debug("  → FAIL (Roll >= Chance)");
            }
        } else if (debugMode) {
            debug("  → Keine Elite-Config für diesen Mob-Typ");
        }

        if (debugMode) {
            debug("=================");
        }
    }

    /**
     * Findet Elite-Config für Biom und Mob-Typ mit verbessertem Matching
     */
    private EliteMobConfig findBiomeElite(Biome biome, EntityType mobType) {
        String biomeName = biome.name().toLowerCase();

        if (debugMode) {
            debug("Suche Elite für: " + biomeName + " (Mob: " + mobType + ")");
        }

        // 1. Exakter Match mit normalisierten Namen
        String normalizedBiome = biomeName.replace("_", "");

        for (Map.Entry<String, EliteMobConfig> entry : biomeElites.entrySet()) {
            String configKey = entry.getKey();
            EliteMobConfig config = entry.getValue();

            // Prüfe Mob-Typ
            if (config.getMobType() != mobType) {
                continue;
            }

            String normalizedKey = configKey.replace("_", "");

            // Exakter Match
            if (normalizedBiome.equals(normalizedKey)) {
                debug("  → Exakter Match: " + configKey);
                return config;
            }

            // Teilstring-Match
            if (normalizedBiome.contains(normalizedKey)) {
                debug("  → Teilstring Match: '" + biomeName + "' contains '" + configKey + "'");
                return config;
            }

            if (normalizedKey.contains(normalizedBiome)) {
                debug("  → Reverse Match: '" + configKey + "' contains '" + biomeName + "'");
                return config;
            }

            // Prefix-Match (z.B. "forest" matched "forest_*")
            if (biomeName.startsWith(configKey) || configKey.startsWith(biomeName.split("_")[0])) {
                debug("  → Prefix Match: " + configKey);
                return config;
            }
        }

        if (debugMode) {
            debug("  → Kein Elite-Config gefunden");
            debug("  Verfügbare Configs: " + biomeElites.keySet());
        }
        return null;
    }

    /**
     * Spawnt einen Elite-Mob mit besserer Error-Behandlung
     */
    private void spawnEliteMob(Location location, EliteMobConfig config) {
        try {
            debug("Spawne Elite-Mob: " + config.getEliteName());
            debug("  Level: " + config.getLevel());
            debug("  Health: " + config.getHealth());
            debug("  Damage: " + config.getDamage());
            debug("  Scale: " + config.getScale());
            debug("  Abilities: " + config.getAbilities().size());

            // Builder mit Abilities VORHER
            CustomMobBuilder builder = mobAPI.createMob(config.getMobType())
                    .at(location)
                    .withName(ChatColor.translateAlternateColorCodes('&', config.getEliteName()))
                    .withLevel(config.getLevel())
                    .withHealth(config.getHealth())
                    .withDamage(config.getDamage())
                    .withScale(config.getScale());

            // Abilities zum Builder hinzufügen
            for (String abilityId : config.getAbilities()) {
                debug("  Füge Ability hinzu: " + abilityId);
                builder.withAbility(abilityId);
            }

            // Jetzt spawnen
            CustomMob elite = builder.spawn();

            if (elite != null && elite.isAlive()) {
                info("Elite-Mob erfolgreich gespawnt: " + config.getEliteName() +
                        " (Level " + config.getLevel() + ") mit " +
                        config.getAbilities().size() + " Abilities");
            } else {
                warn("Elite-Mob gespawnt aber nicht alive: " + config.getEliteName());
            }

        } catch (Exception e) {
            warn("Fehler beim Spawnen von Elite-Mob: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prüft ob Spawn-Reason natürlich ist
     */
    private boolean isNaturalSpawn(CreatureSpawnEvent.SpawnReason reason) {
        return reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                reason == CreatureSpawnEvent.SpawnReason.CHUNK_GEN ||
                reason == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION ||
                reason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS ||
                reason == CreatureSpawnEvent.SpawnReason.NETHER_PORTAL ||
                reason == CreatureSpawnEvent.SpawnReason.PATROL;
    }

    /**
     * Manuell einen Elite-Mob spawnen (für Commands)
     */
    public CustomMob spawnEliteManually(Location location, String eliteId) {
        EliteMobConfig config = biomeElites.get(eliteId.toLowerCase());
        if (config == null) {
            config = structureElites.get(eliteId.toLowerCase());
        }

        if (config == null) {
            return null;
        }

        spawnEliteMob(location, config);

        // Hole gespawnten Mob
        return mobAPI.getCustomMob(location.getWorld().getNearbyEntities(location, 2, 2, 2)
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(e -> mobAPI.isCustomMob(e))
                .findFirst()
                .orElse(null));
    }

    /**
     * Gibt alle verfügbaren Elite-IDs zurück
     */
    public Set<String> getEliteIds() {
        Set<String> ids = new HashSet<>();
        ids.addAll(biomeElites.keySet());
        ids.addAll(structureElites.keySet());
        return ids;
    }

    /**
     * Gibt Elite-Config zurück
     */
    public EliteMobConfig getEliteConfig(String eliteId) {
        EliteMobConfig config = biomeElites.get(eliteId.toLowerCase());
        if (config == null) {
            config = structureElites.get(eliteId.toLowerCase());
        }
        return config;
    }

    public void shutdown() {
        // NICHT mobAPI.shutdown() - wird zentral vom Plugin verwaltet
    }

    @Override
    public void reload() {
        biomeElites.clear();
        structureElites.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadEliteMobs();

        if (plugin.getEliteDropListener() != null) {
            plugin.getEliteDropListener().reload();
        }
    }

    /**
     * Elite-Mob Konfiguration
     */
    public static class EliteMobConfig {
        private final String id;
        private final String eliteName;
        private final EntityType mobType;
        private final int level;
        private final double health;
        private final double damage;
        private final double scale;
        private final double spawnChance;
        private final List<String> abilities;

        private EliteMobConfig(String id, String eliteName, EntityType mobType, int level,
                               double health, double damage, double scale, double spawnChance,
                               List<String> abilities) {
            this.id = id;
            this.eliteName = eliteName;
            this.mobType = mobType;
            this.level = level;
            this.health = health;
            this.damage = damage;
            this.scale = scale;
            this.spawnChance = spawnChance;
            this.abilities = abilities;
        }

        public static EliteMobConfig load(String id, ConfigurationSection section, OraxenQuestPlugin plugin) {
            try {
                String eliteName = section.getString("name", "&6&lElite");
                String typeStr = section.getString("type");

                if (typeStr == null) {
                    plugin.getLogger().warning("Elite '" + id + "' hat keinen type!");
                    return null;
                }

                EntityType mobType;
                try {
                    mobType = EntityType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ungültiger Elite Mob-Typ: " + typeStr);
                    return null;
                }

                int level = section.getInt("level", 5);
                double health = section.getDouble("health", 100.0);
                double damage = section.getDouble("damage", 10.0);
                double scale = section.getDouble("scale", 1.2);
                double spawnChance = section.getDouble("spawn-chance", 5.0);
                List<String> abilities = section.getStringList("abilities");

                return new EliteMobConfig(id, eliteName, mobType, level, health, damage,
                        scale, spawnChance, abilities);

            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Laden von Elite '" + id + "': " + e.getMessage());
                return null;
            }
        }

        public String getId() { return id; }
        public String getEliteName() { return eliteName; }
        public EntityType getMobType() { return mobType; }
        public int getLevel() { return level; }
        public double getHealth() { return health; }
        public double getDamage() { return damage; }
        public double getScale() { return scale; }
        public double getSpawnChance() { return spawnChance; }
        public List<String> getAbilities() { return abilities; }
    }
}