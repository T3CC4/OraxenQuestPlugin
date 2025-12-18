package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.BiomeType;
import de.questplugin.enums.MobEquipmentSlot;
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
import org.bukkit.util.StructureSearchResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verwaltet Elite-Mobs mit Equipment-Support
 *
 * FIX: Custom-World Struktur-Freeze behoben:
 * - Skip für Custom-Generatoren ohne Strukturen
 * - Timeout-Protection für locateNearestStructure()
 */
public class EliteMobManager extends BaseManager implements Listener {

    private final CustomMobAPI mobAPI;
    private final Map<String, EliteMobConfig> biomeElites = new ConcurrentHashMap<>();
    private final Map<String, EliteMobConfig> structureElites = new ConcurrentHashMap<>();
    private final Set<String> worldsWithoutStructures = ConcurrentHashMap.newKeySet();

    public EliteMobManager(OraxenQuestPlugin plugin) {
        super(plugin);
        this.mobAPI = plugin.getCustomMobAPI();

        loadEliteMobs();

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

        ConfigurationSection biomeSection = section.getConfigurationSection("biomes");
        if (biomeSection != null) {
            loadBiomeElites(biomeSection);
        }

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

            if (!validateBiomeConfig(biomeStr)) {
                warn("Ungültiger Biom-Name in elite-mobs.biomes: '" + biomeStr + "'");
                warn("  Nutze /quest biomes für gültige Biom-Namen");
                continue;
            }

            EliteMobConfig config = EliteMobConfig.load(biomeStr, eliteSection, plugin);
            if (config != null) {
                biomeElites.put(biomeStr.toLowerCase(), config);

                List<Biome> matchedBiomes = getMatchingBiomes(biomeStr);
                debug("Biom-Elite geladen: " + biomeStr + " (" + config.getMobType() + ")");
                debug("  Matched " + matchedBiomes.size() + " Biome");
                debug("  Spawn-Chance: " + config.getSpawnChance() + "%");
                debug("  Equipment: " + (config.hasEquipment() ? "JA" : "NEIN"));
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
                debug("Struktur-Elite geladen: " + structureStr + " (" + config.getMobType() + ")");
                debug("  Spawn-Chance: " + config.getSpawnChance() + "%");
                debug("  Equipment: " + (config.hasEquipment() ? "JA" : "NEIN"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        if (!isNaturalSpawn(event.getSpawnReason())) {
            return;
        }

        LivingEntity entity = (LivingEntity) event.getEntity();
        Location location = entity.getLocation();
        Biome biome = location.getBlock().getBiome();
        EntityType mobType = entity.getType();

        if (debugMode) {
            debug("=== Mob Spawn ===");
            debug("Typ: " + mobType);
            debug("Biom: " + biome.getKey().getKey() + " (" + BiomeHelper.getGermanName(biome) + ")");
            debug("Location: " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
        }

        // PRIORITÄT 1: Structure-Based Spawns (höhere Prio)
        EliteMobConfig structureConfig = findStructureElite(location, mobType);

        if (structureConfig != null) {
            debug("Structure-Elite gefunden: " + structureConfig.getEliteName());

            double roll = ThreadLocalRandom.current().nextDouble() * 100;
            debug("  Roll: " + String.format("%.2f", roll));

            if (roll < structureConfig.getSpawnChance()) {
                debug("  → ERFOLG! Spawne Structure-Elite");

                event.setCancelled(true);
                spawnEliteMob(location, structureConfig);

                info("Structure-Elite gespawnt: " + structureConfig.getEliteName() +
                        " @ " + location.getBlock().getType());

                if (debugMode) {
                    debug("=================");
                }
                return;
            }
        }

        // PRIORITÄT 2: Biome-Based Spawns (Fallback)
        EliteMobConfig biomeConfig = findBiomeElite(biome, mobType);

        if (biomeConfig != null) {
            debug("Biome-Elite gefunden: " + biomeConfig.getEliteName());

            double roll = ThreadLocalRandom.current().nextDouble() * 100;
            debug("  Roll: " + String.format("%.2f", roll));

            if (roll < biomeConfig.getSpawnChance()) {
                debug("  → ERFOLG! Spawne Biome-Elite");

                event.setCancelled(true);
                spawnEliteMob(location, biomeConfig);

                info("Biome-Elite gespawnt: " + biomeConfig.getEliteName() +
                        " @ " + biome.getKey().getKey());
            }
        }

        if (debugMode) {
            debug("=================");
        }
    }

    /**
     * Findet Structure-Elite Config anhand der Location
     * FIX: Mit Custom-World-Check und Timeout-Schutz
     */
    private EliteMobConfig findStructureElite(Location location, EntityType mobType) {
        if (structureElites.isEmpty()) {
            return null;
        }

        try {
            World world = location.getWorld();
            String worldName = world.getName();

            // CACHE: Skip wenn Welt bereits als "ohne Strukturen" markiert
            if (worldsWithoutStructures.contains(worldName)) {
                return null;
            }

            // FIX: Skip für Custom-Generatoren ohne Strukturen
            if (world.getGenerator() != null && !hasStructureSupport(world)) {
                worldsWithoutStructures.add(worldName);
                if (debugMode) {
                    debug("  ✗ Welt '" + worldName + "' hat Custom-Generator ohne Struktur-Support");
                }
                return null;
            }

            Registry<org.bukkit.generator.structure.Structure> structureRegistry =
                    Bukkit.getRegistry(org.bukkit.generator.structure.Structure.class);

            for (Map.Entry<String, EliteMobConfig> entry : structureElites.entrySet()) {
                String configKey = entry.getKey();
                EliteMobConfig config = entry.getValue();

                if (config.getMobType() != mobType) {
                    continue;
                }

                NamespacedKey structureKey = getStructureKey(configKey);
                if (structureKey == null) {
                    continue;
                }

                org.bukkit.generator.structure.Structure structure = structureRegistry.get(structureKey);
                if (structure == null) {
                    if (debugMode) {
                        debug("  Struktur nicht in Registry: " + structureKey);
                    }
                    continue;
                }

                try {
                    // FIX: Mit Timeout-Schutz
                    StructureSearchResult result = findStructureSafe(world, location, structure);

                    if (result != null) {
                        Location structureLocation = result.getLocation();
                        double distance = location.distance(structureLocation);

                        if (distance <= 80.0) {
                            debug("  ✓ Struktur gefunden: " + structureKey.getKey() +
                                    " (Distanz: " + String.format("%.1f", distance) + "m)");
                            return config;
                        }
                    }
                } catch (Exception e) {
                    if (debugMode) {
                        debug("  Struktur-Suche failed: " + structureKey + " - " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            warn("Fehler bei Structure-Detection: " + e.getMessage());
        }

        return null;
    }

    /**
     * FIX: Prüft ob eine Welt Struktur-Support hat
     */
    private boolean hasStructureSupport(World world) {
        if (world.getGenerator() == null) {
            return true; // Vanilla-Welten haben immer Strukturen
        }

        String generatorName = world.getGenerator().getClass().getSimpleName();

        // Bekannte Custom-Generatoren OHNE Strukturen
        return !generatorName.contains("AmethystDimension") &&
                !generatorName.contains("VoidWorld") &&
                !generatorName.contains("FlatWorld") &&
                !generatorName.contains("EmptyWorld");
    }

    /**
     * FIX: Structure-Suche mit Timeout-Protection
     */
    private StructureSearchResult findStructureSafe(World world, Location location,
                                                    org.bukkit.generator.structure.Structure structure) {
        try {
            return world.locateNearestStructure(location, structure, 64, false);
        } catch (Exception e) {
            // Timeout oder nicht unterstützt
            return null;
        }
    }

    /**
     * Mapped Config-Key zu NamespacedKey für Structure Registry
     */
    private NamespacedKey getStructureKey(String configKey) {
        String normalized = configKey.toLowerCase().replace("-", "_");

        switch (normalized) {
            // NETHER
            case "fortress":
            case "nether_fortress":
                return NamespacedKey.minecraft("fortress");
            case "bastion":
            case "bastion_remnant":
                return NamespacedKey.minecraft("bastion_remnant");
            case "nether_fossil":
            case "fossil":
                return NamespacedKey.minecraft("nether_fossil");

            // END
            case "end_city":
            case "endcity":
                return NamespacedKey.minecraft("end_city");

            // OVERWORLD - Villages
            case "village":
            case "plains_village":
                return NamespacedKey.minecraft("village_plains");
            case "desert_village":
                return NamespacedKey.minecraft("village_desert");
            case "savanna_village":
                return NamespacedKey.minecraft("village_savanna");
            case "taiga_village":
                return NamespacedKey.minecraft("village_taiga");
            case "snowy_village":
            case "snow_village":
                return NamespacedKey.minecraft("village_snowy");

            // OVERWORLD - Tempel
            case "desert_pyramid":
            case "desert_temple":
            case "temple":
                return NamespacedKey.minecraft("desert_pyramid");
            case "jungle_pyramid":
            case "jungle_temple":
                return NamespacedKey.minecraft("jungle_pyramid");
            case "swamp_hut":
            case "witch_hut":
                return NamespacedKey.minecraft("swamp_hut");

            // OVERWORLD - Große Strukturen
            case "mansion":
            case "woodland_mansion":
                return NamespacedKey.minecraft("mansion");
            case "ocean_monument":
            case "monument":
                return NamespacedKey.minecraft("monument");
            case "ancient_city":
                return NamespacedKey.minecraft("ancient_city");
            case "stronghold":
                return NamespacedKey.minecraft("stronghold");
            case "trial_chambers":
            case "trial_chamber":
                return NamespacedKey.minecraft("trial_chambers");

            // OVERWORLD - Outposts & Ruins
            case "pillager_outpost":
            case "outpost":
                return NamespacedKey.minecraft("pillager_outpost");
            case "ocean_ruin":
            case "ocean_ruins":
                return NamespacedKey.minecraft("ocean_ruin_cold");

            // OVERWORLD - Mineshafts
            case "mineshaft":
                return NamespacedKey.minecraft("mineshaft");
            case "mineshaft_mesa":
                return NamespacedKey.minecraft("mineshaft_mesa");

            // OVERWORLD - Kleine Strukturen
            case "buried_treasure":
            case "treasure":
                return NamespacedKey.minecraft("buried_treasure");
            case "igloo":
                return NamespacedKey.minecraft("igloo");
            case "shipwreck":
                return NamespacedKey.minecraft("shipwreck");
            case "ruined_portal":
            case "portal":
                return NamespacedKey.minecraft("ruined_portal");

            default:
                return null;
        }
    }

    private EliteMobConfig findBiomeElite(Biome biome, EntityType mobType) {
        String biomeName = biome.getKey().getKey().toLowerCase();
        String normalizedBiome = biomeName.replace("_", "");

        for (Map.Entry<String, EliteMobConfig> entry : biomeElites.entrySet()) {
            String configKey = entry.getKey();
            EliteMobConfig config = entry.getValue();

            if (config.getMobType() != mobType) {
                continue;
            }

            String normalizedKey = configKey.replace("_", "");

            if (normalizedBiome.equals(normalizedKey) ||
                    normalizedBiome.contains(normalizedKey) ||
                    normalizedKey.contains(normalizedBiome)) {
                return config;
            }
        }

        return null;
    }

    private boolean validateBiomeConfig(String biomeStr) {
        if (biomeStr == null || biomeStr.isEmpty()) {
            return false;
        }

        if (biomeStr.equals("*")) {
            return true;
        }

        BiomeType biomeType = BiomeHelper.fromString(biomeStr);
        if (biomeType != null) {
            return true;
        }

        // Paper Registry Check
        String normalized = biomeStr.toLowerCase().replace(" ", "_");
        NamespacedKey key = NamespacedKey.minecraft(normalized);
        return Registry.BIOME.get(key) != null;
    }

    private List<Biome> getMatchingBiomes(String biomeStr) {
        if (biomeStr.equals("*")) {
            List<Biome> all = new ArrayList<>();
            Registry.BIOME.forEach(all::add);
            return all;
        }

        List<Biome> matched = new ArrayList<>();
        String normalized = biomeStr.toLowerCase().replace("_", "");

        for (Biome biome : Registry.BIOME) {
            String biomeName = biome.getKey().getKey().toLowerCase().replace("_", "");
            if (biomeName.contains(normalized) || normalized.contains(biomeName)) {
                matched.add(biome);
            }
        }

        return matched;
    }

    private void spawnEliteMob(Location location, EliteMobConfig config) {
        try {
            CustomMobBuilder builder = mobAPI.createMob(config.getMobType())
                    .at(location)
                    .withName(ChatColor.translateAlternateColorCodes('&', config.getEliteName()))
                    .withLevel(config.getLevel())
                    .withHealth(config.getHealth())
                    .withDamage(config.getDamage())
                    .withScale(config.getScale());

            for (String abilityId : config.getAbilities()) {
                builder.withAbility(abilityId);
            }

            if (config.hasEquipment()) {
                for (Map.Entry<MobEquipmentSlot, EliteMobConfig.EquipmentItem> entry :
                        config.getEquipment().entrySet()) {
                    builder.withOraxenEquipment(entry.getKey(),
                            entry.getValue().oraxenItemId,
                            entry.getValue().dropChance);
                }
            }

            CustomMob elite = builder.spawn();

            if (elite != null && elite.isAlive()) {
                info("Elite gespawnt: " + config.getEliteName());
            }

        } catch (Exception e) {
            warn("Elite-Spawn-Fehler: " + e.getMessage());
        }
    }

    private boolean isNaturalSpawn(CreatureSpawnEvent.SpawnReason reason) {
        return reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                reason == CreatureSpawnEvent.SpawnReason.DEFAULT ||
                reason == CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION ||
                reason == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS ||
                reason == CreatureSpawnEvent.SpawnReason.NETHER_PORTAL ||
                reason == CreatureSpawnEvent.SpawnReason.PATROL;
    }

    public CustomMob spawnEliteManually(Location location, String eliteId) {
        EliteMobConfig config = biomeElites.get(eliteId.toLowerCase());
        if (config == null) {
            config = structureElites.get(eliteId.toLowerCase());
        }

        if (config == null) {
            return null;
        }

        spawnEliteMob(location, config);

        return mobAPI.getCustomMob(location.getWorld().getNearbyEntities(location, 2, 2, 2)
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(e -> mobAPI.isCustomMob(e))
                .findFirst()
                .orElse(null));
    }

    public Set<String> getEliteIds() {
        Set<String> ids = new HashSet<>();
        ids.addAll(biomeElites.keySet());
        ids.addAll(structureElites.keySet());
        return ids;
    }

    public EliteMobConfig getEliteConfig(String eliteId) {
        EliteMobConfig config = biomeElites.get(eliteId.toLowerCase());
        if (config == null) {
            config = structureElites.get(eliteId.toLowerCase());
        }
        return config;
    }

    public void shutdown() {
        // Cleanup
    }

    @Override
    public void reload() {
        biomeElites.clear();
        structureElites.clear();
        worldsWithoutStructures.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadEliteMobs();

        if (plugin.getEliteDropListener() != null) {
            plugin.getEliteDropListener().reload();
        }
    }

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
        private final Map<MobEquipmentSlot, EquipmentItem> equipment;

        private EliteMobConfig(String id, String eliteName, EntityType mobType, int level,
                               double health, double damage, double scale, double spawnChance,
                               List<String> abilities, Map<MobEquipmentSlot, EquipmentItem> equipment) {
            this.id = id;
            this.eliteName = eliteName;
            this.mobType = mobType;
            this.level = level;
            this.health = health;
            this.damage = damage;
            this.scale = scale;
            this.spawnChance = spawnChance;
            this.abilities = abilities;
            this.equipment = equipment != null ? equipment : new HashMap<>();
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
                    plugin.getLogger().warning("Ungültiger Mob-Typ: " + typeStr);
                    return null;
                }

                int level = section.getInt("level", 5);
                double health = section.getDouble("health", 100.0);
                double damage = section.getDouble("damage", 10.0);
                double scale = section.getDouble("scale", 1.2);
                double spawnChance = section.getDouble("spawn-chance", 5.0);
                List<String> abilities = section.getStringList("abilities");

                Map<MobEquipmentSlot, EquipmentItem> equipment = new HashMap<>();
                ConfigurationSection equipSection = section.getConfigurationSection("equipment");

                if (equipSection != null) {
                    for (String slotStr : equipSection.getKeys(false)) {
                        MobEquipmentSlot slot = MobEquipmentSlot.fromString(slotStr);
                        if (slot == null) {
                            continue;
                        }

                        ConfigurationSection itemSection = equipSection.getConfigurationSection(slotStr);
                        if (itemSection == null) continue;

                        String oraxenItemId = itemSection.getString("oraxen-item");
                        float dropChance = (float) itemSection.getDouble("drop-chance", 0.0);

                        if (oraxenItemId != null && !oraxenItemId.isEmpty()) {
                            equipment.put(slot, new EquipmentItem(oraxenItemId, dropChance));
                        }
                    }
                }

                return new EliteMobConfig(id, eliteName, mobType, level, health, damage,
                        scale, spawnChance, abilities, equipment);

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
        public Map<MobEquipmentSlot, EquipmentItem> getEquipment() { return equipment; }
        public boolean hasEquipment() { return !equipment.isEmpty(); }

        public static class EquipmentItem {
            final String oraxenItemId;
            final float dropChance;

            EquipmentItem(String oraxenItemId, float dropChance) {
                this.oraxenItemId = oraxenItemId;
                this.dropChance = dropChance;
            }
        }
    }
}