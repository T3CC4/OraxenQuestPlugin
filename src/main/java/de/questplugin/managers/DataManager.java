package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final OraxenQuestPlugin plugin;
    private File dataFile;
    private FileConfiguration data;

    public DataManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        setupDataFile();
    }

    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte data.yml nicht erstellen!");
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte data.yml nicht speichern!");
            e.printStackTrace();
        }
    }

    // Quest-Daten
    public void saveQuestData(String currentQuestRequired, String currentQuestReward,
                              double moneyReward, long questStartTime, long nextQuestAvailable, String lastPlayerUUID) {
        data.set("quest.current.required-item", currentQuestRequired);
        data.set("quest.current.reward-item", currentQuestReward);
        data.set("quest.current.money-reward", moneyReward);
        data.set("quest.start-time", questStartTime);
        data.set("quest.next-available", nextQuestAvailable);
        data.set("quest.last-player-uuid", lastPlayerUUID);
        save();
    }

    public String getQuestRequiredItem() {
        return data.getString("quest.current.required-item");
    }

    public String getQuestRewardItem() {
        return data.getString("quest.current.reward-item");
    }

    public double getQuestMoneyReward() {
        return data.getDouble("quest.current.money-reward", 0);
    }

    public long getQuestStartTime() {
        return data.getLong("quest.start-time", System.currentTimeMillis());
    }

    public long getNextQuestAvailable() {
        return data.getLong("quest.next-available", System.currentTimeMillis());
    }

    public String getLastCompletedPlayerUUID() {
        return data.getString("quest.last-player-uuid");
    }

    // Tracked Players
    public void saveTrackedPlayers(Set<UUID> players) {
        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : players) {
            uuidStrings.add(uuid.toString());
        }
        data.set("quest.tracked-players", uuidStrings);
        save();
    }

    public Set<UUID> loadTrackedPlayers() {
        Set<UUID> players = new HashSet<>();
        List<String> uuidStrings = data.getStringList("quest.tracked-players");
        for (String uuidString : uuidStrings) {
            try {
                players.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültige UUID: " + uuidString);
            }
        }
        return players;
    }

    // Processed Chests
    public void saveProcessedChests(Set<Location> chests) {
        List<String> locationStrings = new ArrayList<>();
        for (Location loc : chests) {
            String locString = loc.getWorld().getName() + "," +
                    loc.getBlockX() + "," +
                    loc.getBlockY() + "," +
                    loc.getBlockZ();
            locationStrings.add(locString);
        }
        data.set("chests.processed", locationStrings);
        save();
    }

    public Set<Location> loadProcessedChests() {
        Set<Location> chests = new HashSet<>();
        List<String> locationStrings = data.getStringList("chests.processed");
        for (String locString : locationStrings) {
            try {
                String[] parts = locString.split(",");
                if (parts.length == 4) {
                    Location loc = new Location(
                            plugin.getServer().getWorld(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3])
                    );
                    chests.add(loc);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Ungültige Location: " + locString);
            }
        }
        return chests;
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }
}