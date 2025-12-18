package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.raid.RaidInstance;
import de.questplugin.raid.RaidConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet alle aktiven Raids
 *
 * FIXES:
 * - Konstruktor-Aufruf korrigiert
 * - start() Methode wird korrekt aufgerufen
 * - Bessere Error-Messages
 */
public class RaidManager extends BaseManager {

    // RaidID -> RaidConfig
    private final Map<String, RaidConfig> raidConfigs = new ConcurrentHashMap<>();

    // Aktive Raids: UUID -> RaidInstance
    private final Map<UUID, RaidInstance> activeRaids = new ConcurrentHashMap<>();

    public RaidManager(OraxenQuestPlugin plugin) {
        super(plugin);
        loadRaidConfigs();
    }

    private void loadRaidConfigs() {
        // Lade raids.yml
        File raidsFile = new File(plugin.getDataFolder(), "raids.yml");
        if (!raidsFile.exists()) {
            plugin.saveResource("raids.yml", false);
        }

        org.bukkit.configuration.file.FileConfiguration raidsConfig =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(raidsFile);

        ConfigurationSection raidsSection = raidsConfig.getConfigurationSection("raids");
        if (raidsSection == null) {
            warn("Keine raids Sektion in raids.yml!");
            return;
        }

        raidConfigs.clear();
        int loaded = 0;

        for (String raidId : raidsSection.getKeys(false)) {
            ConfigurationSection raidSection = raidsSection.getConfigurationSection(raidId);
            if (raidSection == null) continue;

            RaidConfig config = RaidConfig.load(raidId, raidSection, plugin);
            if (config != null) {
                raidConfigs.put(raidId.toLowerCase(), config);
                loaded++;

                debug("Raid geladen: " + raidId);
                debug("  Wellen: " + config.getWaves().size());
                debug("  Biome: " + config.getAllowedBiomes().size() + " erlaubt");
            }
        }

        info("Raid-Configs: " + loaded + " geladen");
    }

    /**
     * Startet einen Raid für einen Spieler
     *
     * @param raidId Raid-ID aus raids.yml
     * @param player Spieler
     * @return true wenn erfolgreich gestartet, false bei Fehler
     */
    public boolean startRaid(String raidId, Player player) {
        raidId = raidId.toLowerCase();

        // Prüfe ob Config existiert
        RaidConfig config = raidConfigs.get(raidId);
        if (config == null) {
            debug("Raid-Config nicht gefunden: " + raidId);
            debug("Verfügbare Raids: " + raidConfigs.keySet());
            return false;
        }

        // Prüfe ob Spieler bereits in Raid
        if (isInRaid(player)) {
            debug("Spieler " + player.getName() + " bereits in Raid");
            return false;
        }

        // Prüfe Biom
        if (!config.isAllowedBiome(player.getLocation().getBlock().getBiome())) {
            debug("Biom nicht erlaubt: " + player.getLocation().getBlock().getBiome());
            debug("Erlaubte Biome: " + config.getAllowedBiomes());
            return false;
        }

        // Erstelle Raid-Instanz mit KORREKTER Parameter-Reihenfolge
        // WICHTIG: RaidConfig, Player, Plugin
        RaidInstance raid = new RaidInstance(config, player, plugin);
        activeRaids.put(player.getUniqueId(), raid);

        // Starte Raid
        raid.start();

        info("Raid '" + raidId + "' gestartet für " + player.getName());
        return true;
    }

    /**
     * Stoppt einen Raid vorzeitig
     *
     * @param player Spieler
     * @return true wenn gestoppt, false wenn nicht in Raid
     */
    public boolean stopRaid(Player player) {
        RaidInstance raid = activeRaids.remove(player.getUniqueId());
        if (raid != null) {
            raid.stop();
            info("Raid gestoppt für " + player.getName());
            return true;
        }
        return false;
    }

    /**
     * Prüft ob Spieler in Raid ist
     */
    public boolean isInRaid(Player player) {
        return activeRaids.containsKey(player.getUniqueId());
    }

    /**
     * Holt aktiven Raid eines Spielers
     */
    public RaidInstance getRaid(Player player) {
        return activeRaids.get(player.getUniqueId());
    }

    /**
     * Entfernt Raid aus aktiven Raids (wird von RaidInstance aufgerufen)
     */
    public void removeRaid(UUID playerUUID) {
        activeRaids.remove(playerUUID);
        debug("Raid entfernt für UUID: " + playerUUID);
    }

    /**
     * Gibt alle verfügbaren Raid-IDs zurück
     */
    public Set<String> getRaidIds() {
        return new HashSet<>(raidConfigs.keySet());
    }

    /**
     * Gibt RaidConfig zurück
     */
    public RaidConfig getRaidConfig(String raidId) {
        return raidConfigs.get(raidId.toLowerCase());
    }

    /**
     * Gibt alle aktiven Raids zurück
     */
    public Collection<RaidInstance> getActiveRaids() {
        return activeRaids.values();
    }

    /**
     * Stoppt alle aktiven Raids (beim Plugin-Disable)
     */
    public void shutdown() {
        if (activeRaids.isEmpty()) {
            debug("Keine aktiven Raids zum Stoppen");
            return;
        }

        info("Stoppe " + activeRaids.size() + " aktive Raids...");

        // Kopie erstellen um ConcurrentModificationException zu vermeiden
        for (RaidInstance raid : new ArrayList<>(activeRaids.values())) {
            try {
                raid.stop();
            } catch (Exception e) {
                warn("Fehler beim Stoppen von Raid: " + e.getMessage());
            }
        }

        activeRaids.clear();
        info("Alle Raids gestoppt");
    }

    @Override
    public void reload() {
        info("Lade RaidManager neu...");

        // Stoppe alle aktiven Raids vor Reload
        shutdown();

        raidConfigs.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadRaidConfigs();

        info("RaidManager neu geladen");
    }
}