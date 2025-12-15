package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.BiomeType;
import de.questplugin.enums.MobType;
import de.questplugin.mobs.api.CustomMob;
import de.questplugin.mobs.api.CustomMobAPI;
import de.questplugin.mobs.api.CustomMobBuilder;
import de.questplugin.mobs.api.DefendMode;
import de.questplugin.utils.StructureHelper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verwaltet Elite-Mobs die in bestimmten Biomen/Strukturen spawnen
 *
 * FEATURES:
 * - Biom-basiertes Elite-Spawning
 * - Struktur-basiertes Elite-Spawning
 * - Custom Abilities für Elite-Mobs
 * - Konfigurierbare Spawn-Chance
 */
public class EliteMobManager extends BaseManager implements Listener {

    private final CustomMobAPI mobAPI;
    private final Map<String, EliteMobConfig> biomeElites = new ConcurrentHashMap<>();
    private final Map<String, EliteMobConfig> structureElites = new ConcurrentHashMap<>();

    public EliteMobManager(OraxenQuestPlugin plugin) {
        super(plugin);
        this.mobAPI = plugin.getCustomMobAPI(); // Nutze zentrale API

        loadEliteMobs();

        // Registriere Listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        info("EliteMobManager initialisiert");
    }

    private void loadEliteMobs() {
        biomeElites.clear();
        structureElites.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("elite-mobs");
        if (section == null) {
            debug("Keine elite-mobs Sektion in Config");
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

    private void loadBiomeElites(ConfigurationSection section) {
        for (String biomeStr : section.getKeys(false)) {
            ConfigurationSection eliteSection = section.getConfigurationSection(biomeStr);
            if (eliteSection == null) continue;

            EliteMobConfig config = EliteMobConfig.load(biomeStr, eliteSection, plugin);
            if (config != null) {
                biomeElites.put(biomeStr.toLowerCase(), config);
                debug("Biom-Elite geladen: " + biomeStr);
            }
        }
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
     * Event Handler für Mob-Spawning
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

        // Prüfe Biom-Elites
        Biome biome = location.getBlock().getBiome();
        EliteMobConfig biomeConfig = findBiomeElite(biome, entity.getType());

        if (biomeConfig != null) {
            // Rolle für Spawn-Chance
            double roll = ThreadLocalRandom.current().nextDouble() * 100;

            if (roll < biomeConfig.getSpawnChance()) {
                // Event canceln und durch Elite ersetzen
                event.setCancelled(true);
                spawnEliteMob(location, biomeConfig);

                debug("Elite-Mob gespawnt (Biom): " + biomeConfig.getEliteName() +
                        " @ " + biome + " (" + roll + "/" + biomeConfig.getSpawnChance() + "%)");
            }
        }
    }

    /**
     * Findet Elite-Config für Biom und Mob-Typ
     */
    private EliteMobConfig findBiomeElite(Biome biome, EntityType mobType) {
        String biomeName = biome.name().toLowerCase();

        // Exakter Match
        EliteMobConfig config = biomeElites.get(biomeName);
        if (config != null && config.getMobType() == mobType) {
            return config;
        }

        // Wildcard-Match (z.B. "forest" für alle Forest-Typen)
        for (Map.Entry<String, EliteMobConfig> entry : biomeElites.entrySet()) {
            if (biomeName.contains(entry.getKey()) || entry.getKey().contains(biomeName)) {
                if (entry.getValue().getMobType() == mobType) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Spawnt einen Elite-Mob
     */
    private void spawnEliteMob(Location location, EliteMobConfig config) {
        try {
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
                builder.withAbility(abilityId);
            }

            // Jetzt spawnen
            CustomMob elite = builder.spawn();

            debug("Elite-Mob erstellt: " + config.getEliteName() +
                    " (Level " + config.getLevel() + ") mit " +
                    config.getAbilities().size() + " Abilities");

        } catch (Exception e) {
            warn("Fehler beim Spawnen von Elite-Mob: " + e.getMessage());
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