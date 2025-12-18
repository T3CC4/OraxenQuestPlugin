package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Verwaltet persistente Daten (data.yml)
 * - Quest-State
 * - Tracked Players
 * - Processed Chests
 *
 * FIX: Verhindert async Tasks während Plugin-Disable
 */
public class DataManager {

    private final OraxenQuestPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    // Performance: Verhindere excessive Saves
    private volatile boolean saveScheduled = false;
    private volatile boolean pluginDisabling = false; // NEU
    private static final long SAVE_DELAY_TICKS = 100L; // 5 Sekunden

    public DataManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        setupDataFile();
    }

    /**
     * Erstellt data.yml wenn nicht vorhanden
     */
    private void setupDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                plugin.getPluginLogger().info("data.yml erstellt");
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Konnte data.yml nicht erstellen: " + e.getMessage());
                e.printStackTrace();
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Speichert Daten synchron
     */
    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Fehler beim Speichern von data.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Speichert Daten asynchron (Performance)
     * WICHTIG: Nur wenn Plugin NICHT deaktiviert wird!
     */
    public void saveAsync() {
        // FIX: Kein async wenn Plugin disabled wird
        if (pluginDisabling) {
            save(); // Synchron speichern
            return;
        }

        if (saveScheduled) {
            return; // Bereits ein Save geplant
        }

        saveScheduled = true;

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            save();
            saveScheduled = false;
        }, SAVE_DELAY_TICKS);
    }

    /**
     * Speichert Daten als CompletableFuture
     */
    public CompletableFuture<Void> saveAsyncFuture() {
        return CompletableFuture.runAsync(() -> save());
    }

    /**
     * Markiert dass Plugin disabled wird
     * Ab jetzt nur noch synchrone Saves!
     */
    public void setDisabling() {
        this.pluginDisabling = true;
    }

    // ==================== QUEST DATEN ====================

    /**
     * Speichert Quest-Daten
     */
    public void saveQuestData(String requiredItem, String rewardItem, double moneyReward,
                              long startTime, long nextAvailable, String lastPlayerUUID) {
        data.set("quest.current.required-item", requiredItem);
        data.set("quest.current.reward-item", rewardItem);
        data.set("quest.current.money-reward", moneyReward);
        data.set("quest.start-time", startTime);
        data.set("quest.next-available", nextAvailable);
        data.set("quest.last-player-uuid", lastPlayerUUID);

        // FIX: Prüfe ob Plugin disabled wird
        if (pluginDisabling) {
            save(); // Synchron
        } else {
            saveAsync(); // Asynchron nur wenn Plugin läuft
        }
    }

    public String getQuestRequiredItem() {
        return data.getString("quest.current.required-item");
    }

    public String getQuestRewardItem() {
        return data.getString("quest.current.reward-item");
    }

    public double getQuestMoneyReward() {
        return data.getDouble("quest.current.money-reward", 0.0);
    }

    public long getQuestStartTime() {
        return data.getLong("quest.start-time", System.currentTimeMillis());
    }

    public long getNextQuestAvailable() {
        return data.getLong("quest.next-available", System.currentTimeMillis());
    }

    public String getLastCompletedPlayerUUID() {
        return data.getString("quest.last-player-uuid", "");
    }

    // ==================== TRACKED PLAYERS ====================

    /**
     * Speichert getrackte Spieler
     */
    public void saveTrackedPlayers(Set<UUID> players) {
        List<String> uuidStrings = new ArrayList<>(players.size());
        for (UUID uuid : players) {
            uuidStrings.add(uuid.toString());
        }
        data.set("quest.tracked-players", uuidStrings);

        // FIX: Prüfe ob Plugin disabled wird
        if (pluginDisabling) {
            save(); // Synchron
        } else {
            saveAsync();
        }
    }

    /**
     * Lädt getrackte Spieler
     */
    public Set<UUID> loadTrackedPlayers() {
        Set<UUID> players = new HashSet<>();
        List<String> uuidStrings = data.getStringList("quest.tracked-players");

        for (String uuidString : uuidStrings) {
            try {
                players.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültige UUID in tracked-players: " + uuidString);
            }
        }

        return players;
    }

    // ==================== PROCESSED CHESTS ====================

    /**
     * Speichert verarbeitete Kisten
     */
    public void saveProcessedChests(Set<Location> chests) {
        List<String> locationStrings = new ArrayList<>(chests.size());

        for (Location loc : chests) {
            if (loc.getWorld() == null) {
                plugin.getLogger().warning("Location ohne World: " + loc);
                continue;
            }

            locationStrings.add(serializeLocation(loc));
        }

        data.set("chests.processed", locationStrings);

        // FIX: Prüfe ob Plugin disabled wird
        if (pluginDisabling) {
            save(); // Synchron
        } else {
            saveAsync();
        }
    }

    /**
     * Lädt verarbeitete Kisten
     */
    public Set<Location> loadProcessedChests() {
        Set<Location> chests = new HashSet<>();
        List<String> locationStrings = data.getStringList("chests.processed");

        for (String locString : locationStrings) {
            try {
                Location loc = deserializeLocation(locString);
                if (loc != null) {
                    chests.add(loc);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ungültige Location: " + locString);
            }
        }

        return chests;
    }

    // ==================== LOCATION SERIALISIERUNG ====================

    /**
     * Serialisiert Location zu String
     * Format: world,x,y,z
     */
    private String serializeLocation(Location loc) {
        return String.format("%s,%d,%d,%d",
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
    }

    /**
     * Deserialisiert Location von String
     */
    private Location deserializeLocation(String locString) {
        String[] parts = locString.split(",");

        if (parts.length != 4) {
            plugin.getLogger().warning("Ungültiges Location-Format: " + locString);
            return null;
        }

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            plugin.getLogger().warning("Welt nicht gefunden: " + parts[0]);
            return null;
        }

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);

            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Ungültige Koordinaten: " + locString);
            return null;
        }
    }

    // ==================== UTILITY ====================

    /**
     * Lädt data.yml neu
     */
    public void reload() {
        data = YamlConfiguration.loadConfiguration(dataFile);
        plugin.getPluginLogger().info("data.yml neu geladen");
    }

    /**
     * Gibt data.yml Config zurück (für erweiterte Nutzung)
     */
    public FileConfiguration getData() {
        return data;
    }

    /**
     * Prüft ob data.yml existiert
     */
    public boolean exists() {
        return dataFile.exists();
    }

    /**
     * Gibt Dateigröße zurück
     */
    public long getFileSize() {
        return dataFile.length();
    }

    /**
     * Erstellt Backup von data.yml
     */
    public boolean createBackup() {
        try {
            File backupFile = new File(plugin.getDataFolder(),
                    "data.yml.backup." + System.currentTimeMillis());

            if (dataFile.exists()) {
                java.nio.file.Files.copy(
                        dataFile.toPath(),
                        backupFile.toPath()
                );
                plugin.getPluginLogger().info("Backup erstellt: " + backupFile.getName());
                return true;
            }
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Backup-Fehler: " + e.getMessage());
        }
        return false;
    }

    /**
     * Bereinigt alte Backups (behält nur die letzten 5)
     */
    public void cleanupOldBackups() {
        File[] backups = plugin.getDataFolder().listFiles(
                (dir, name) -> name.startsWith("data.yml.backup.")
        );

        if (backups == null || backups.length <= 5) {
            return;
        }

        // Sortiere nach Datum (älteste zuerst)
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

        // Lösche alle außer den letzten 5
        int toDelete = backups.length - 5;
        for (int i = 0; i < toDelete; i++) {
            if (backups[i].delete()) {
                plugin.getPluginLogger().info("Altes Backup gelöscht: " + backups[i].getName());
            }
        }
    }
}