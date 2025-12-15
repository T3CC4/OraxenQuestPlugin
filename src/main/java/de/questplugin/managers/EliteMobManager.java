package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.BiomeType;
import de.questplugin.enums.MobEquipmentSlot;
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
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Verwaltet Elite-Mobs mit Equipment-Support
 */
public class EliteMobManager extends BaseManager implements Listener {

    private final CustomMobAPI mobAPI;
    private final Map<String, EliteMobConfig> biomeElites = new ConcurrentHashMap<>();
    private final Map<String, EliteMobConfig> structureElites = new ConcurrentHashMap<>();

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

        try {
            Biome.valueOf(biomeStr.toUpperCase().replace(" ", "_"));
            return true;
        } catch (IllegalArgumentException e) {
            // Nicht gefunden
        }

        String normalized = biomeStr.toLowerCase().replace("_", "");
        for (Biome biome : Biome.values()) {
            String biomeName = biome.name().toLowerCase().replace("_", "");
            if (biomeName.contains(normalized) || normalized.contains(biomeName)) {
                return true;
            }
        }

        return false;
    }

    private List<Biome> getMatchingBiomes(String biomeStr) {
        List<Biome> matched = new ArrayList<>();

        if (biomeStr.equals("*")) {
            return Arrays.asList(Biome.values());
        }

        String normalized = biomeStr.toLowerCase().replace("_", "").replace(" ", "");

        for (Biome biome : Biome.values()) {
            String biomeName = biome.name().toLowerCase().replace("_", "");

            if (biomeName.equals(normalized)) {
                matched.add(biome);
                continue;
            }

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
            debug("Biom: " + biome + " (" + BiomeHelper.getGermanName(biome) + ")");
        }

        EliteMobConfig biomeConfig = findBiomeElite(biome, mobType);

        if (biomeConfig != null) {
            debug("Elite-Config gefunden: " + biomeConfig.getEliteName());

            double roll = ThreadLocalRandom.current().nextDouble() * 100;
            debug("  Roll: " + String.format("%.2f", roll));

            if (roll < biomeConfig.getSpawnChance()) {
                debug("  → ERFOLG! Spawne Elite-Mob");

                event.setCancelled(true);
                spawnEliteMob(location, biomeConfig);

                info("Elite-Mob gespawnt: " + biomeConfig.getEliteName() +
                        " @ " + biome);
            }
        }

        if (debugMode) {
            debug("=================");
        }
    }

    private EliteMobConfig findBiomeElite(Biome biome, EntityType mobType) {
        String biomeName = biome.name().toLowerCase();

        if (debugMode) {
            debug("Suche Elite für: " + biomeName + " (Mob: " + mobType + ")");
        }

        String normalizedBiome = biomeName.replace("_", "");

        for (Map.Entry<String, EliteMobConfig> entry : biomeElites.entrySet()) {
            String configKey = entry.getKey();
            EliteMobConfig config = entry.getValue();

            if (config.getMobType() != mobType) {
                continue;
            }

            String normalizedKey = configKey.replace("_", "");

            if (normalizedBiome.equals(normalizedKey)) {
                debug("  → Exakter Match: " + configKey);
                return config;
            }

            if (normalizedBiome.contains(normalizedKey)) {
                debug("  → Teilstring Match: '" + biomeName + "' contains '" + configKey + "'");
                return config;
            }

            if (normalizedKey.contains(normalizedBiome)) {
                debug("  → Reverse Match: '" + configKey + "' contains '" + biomeName + "'");
                return config;
            }

            if (biomeName.startsWith(configKey) || configKey.startsWith(biomeName.split("_")[0])) {
                debug("  → Prefix Match: " + configKey);
                return config;
            }
        }

        return null;
    }

    /**
     * Spawnt Elite-Mob mit Equipment
     */
    private void spawnEliteMob(Location location, EliteMobConfig config) {
        try {
            debug("Spawne Elite-Mob: " + config.getEliteName());
            debug("  Equipment: " + (config.hasEquipment() ? "JA" : "NEIN"));

            CustomMobBuilder builder = mobAPI.createMob(config.getMobType())
                    .at(location)
                    .withName(ChatColor.translateAlternateColorCodes('&', config.getEliteName()))
                    .withLevel(config.getLevel())
                    .withHealth(config.getHealth())
                    .withDamage(config.getDamage())
                    .withScale(config.getScale());

            for (String abilityId : config.getAbilities()) {
                debug("  Füge Ability hinzu: " + abilityId);
                builder.withAbility(abilityId);
            }

            // NEU: Equipment hinzufügen
            if (config.hasEquipment()) {
                for (Map.Entry<MobEquipmentSlot, EliteMobConfig.EquipmentItem> entry :
                        config.getEquipment().entrySet()) {
                    MobEquipmentSlot slot = entry.getKey();
                    EliteMobConfig.EquipmentItem item = entry.getValue();

                    debug("  Füge Equipment hinzu: " + slot + " → " + item.oraxenItemId);
                    builder.withOraxenEquipment(slot, item.oraxenItemId, item.dropChance);
                }
            }

            CustomMob elite = builder.spawn();

            if (elite != null && elite.isAlive()) {
                info("Elite-Mob erfolgreich gespawnt: " + config.getEliteName() +
                        " (Level " + config.getLevel() + ")" +
                        (config.hasEquipment() ? " mit Equipment" : ""));
            } else {
                warn("Elite-Mob gespawnt aber nicht alive: " + config.getEliteName());
            }

        } catch (Exception e) {
            warn("Fehler beim Spawnen von Elite-Mob: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isNaturalSpawn(CreatureSpawnEvent.SpawnReason reason) {
        return reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                reason == CreatureSpawnEvent.SpawnReason.CHUNK_GEN ||
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
     * Elite-Mob Konfiguration mit Equipment-Support
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
        private final Map<MobEquipmentSlot, EquipmentItem> equipment; // NEU

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
                    plugin.getLogger().warning("Ungültiger Elite Mob-Typ: " + typeStr);
                    return null;
                }

                int level = section.getInt("level", 5);
                double health = section.getDouble("health", 100.0);
                double damage = section.getDouble("damage", 10.0);
                double scale = section.getDouble("scale", 1.2);
                double spawnChance = section.getDouble("spawn-chance", 5.0);
                List<String> abilities = section.getStringList("abilities");

                // NEU: Equipment laden
                Map<MobEquipmentSlot, EquipmentItem> equipment = new HashMap<>();
                ConfigurationSection equipSection = section.getConfigurationSection("equipment");

                if (equipSection != null) {
                    for (String slotStr : equipSection.getKeys(false)) {
                        MobEquipmentSlot slot = MobEquipmentSlot.fromString(slotStr);
                        if (slot == null) {
                            plugin.getLogger().warning("Elite '" + id + "': Ungültiger Equipment-Slot '" + slotStr + "'");
                            continue;
                        }

                        ConfigurationSection itemSection = equipSection.getConfigurationSection(slotStr);
                        if (itemSection == null) continue;

                        String oraxenItemId = itemSection.getString("oraxen-item");
                        float dropChance = (float) itemSection.getDouble("drop-chance", 0.0);

                        if (oraxenItemId != null && !oraxenItemId.isEmpty()) {
                            equipment.put(slot, new EquipmentItem(oraxenItemId, dropChance));
                            plugin.getPluginLogger().debug("Elite '" + id + "': Equipment " +
                                    slot + " → " + oraxenItemId);
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

        // NEU: Equipment Methoden
        public Map<MobEquipmentSlot, EquipmentItem> getEquipment() {
            return equipment;
        }

        public boolean hasEquipment() {
            return !equipment.isEmpty();
        }

        /**
         * Equipment-Item für Elite-Mobs
         */
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