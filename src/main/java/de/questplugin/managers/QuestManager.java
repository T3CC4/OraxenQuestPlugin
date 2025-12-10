package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class QuestManager {

    private final OraxenQuestPlugin plugin;
    private final Random random;
    private Economy economy;

    // Thread-safe Collections
    private final Set<UUID> trackedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> completedPlayers = ConcurrentHashMap.newKeySet();

    // Thread-safe Quest State mit ReadWriteLock
    private final ReadWriteLock questLock = new ReentrantReadWriteLock();
    private volatile Quest currentQuest;
    private volatile long questStartTime;
    private volatile long nextQuestAvailable;
    private volatile UUID lastCompletedPlayer;

    private BukkitTask questTask;

    // Cache für validierte Quests
    private final Map<String, Quest> validQuestCache = new ConcurrentHashMap<>();

    public QuestManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();

        setupEconomy();
        loadFromData();
        validateQuestCache();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault nicht gefunden - Geld-Belohnungen deaktiviert");
            return;
        }

        try {
            var rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                plugin.getPluginLogger().info("Economy geladen: " + economy.getName());
            } else {
                plugin.getLogger().warning("Kein Economy-Provider gefunden");
            }
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Fehler beim Laden von Vault: " + e.getMessage());
        }
    }

    private void validateQuestCache() {
        ConfigurationSection questsSection = plugin.getConfig().getConfigurationSection("quests");
        if (questsSection == null) return;

        int valid = 0;
        int invalid = 0;

        for (String questKey : questsSection.getKeys(false)) {
            ConfigurationSection questData = questsSection.getConfigurationSection(questKey);
            if (questData == null) continue;

            String requiredItem = questData.getString("required-item");
            String rewardItem = questData.getString("reward-item");
            double money = questData.getDouble("money-reward", 0);

            if (validateOraxenItems(requiredItem, rewardItem)) {
                validQuestCache.put(questKey, new Quest(requiredItem, rewardItem, money));
                valid++;
            } else {
                invalid++;
                plugin.getLogger().warning("Quest '" + questKey + "' ungültig: Items nicht in Oraxen");
            }
        }

        plugin.getPluginLogger().info("Quest-Cache: " + valid + " gültig, " + invalid + " ungültig");
    }

    private void loadFromData() {
        try {
            questLock.writeLock().lock();

            String requiredItem = plugin.getDataManager().getQuestRequiredItem();
            String rewardItem = plugin.getDataManager().getQuestRewardItem();
            double moneyReward = plugin.getDataManager().getQuestMoneyReward();

            if (requiredItem != null && rewardItem != null &&
                    validateOraxenItems(requiredItem, rewardItem)) {
                currentQuest = new Quest(requiredItem, rewardItem, moneyReward);
            } else {
                selectRandomQuest();
            }

            questStartTime = plugin.getDataManager().getQuestStartTime();
            nextQuestAvailable = plugin.getDataManager().getNextQuestAvailable();

            String lastPlayerUUID = plugin.getDataManager().getLastCompletedPlayerUUID();
            if (lastPlayerUUID != null && !lastPlayerUUID.isEmpty()) {
                try {
                    lastCompletedPlayer = UUID.fromString(lastPlayerUUID);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ungültige UUID: " + lastPlayerUUID);
                }
            }
        } finally {
            questLock.writeLock().unlock();
        }

        trackedPlayers.addAll(plugin.getDataManager().loadTrackedPlayers());
        plugin.getPluginLogger().info(trackedPlayers.size() + " Spieler werden getrackt");
    }

    private boolean validateOraxenItems(String... itemIds) {
        if (itemIds == null) return false;

        for (String itemId : itemIds) {
            if (itemId == null || itemId.isEmpty() || !OraxenItems.exists(itemId)) {
                return false;
            }
        }
        return true;
    }

    private void saveToData() {
        try {
            questLock.readLock().lock();

            if (currentQuest != null) {
                plugin.getDataManager().saveQuestData(
                        currentQuest.requiredItem,
                        currentQuest.rewardItem,
                        currentQuest.moneyReward,
                        questStartTime,
                        nextQuestAvailable,
                        lastCompletedPlayer != null ? lastCompletedPlayer.toString() : null
                );
            }

            plugin.getDataManager().saveTrackedPlayers(trackedPlayers);
        } finally {
            questLock.readLock().unlock();
        }
    }

    private void selectRandomQuest() {
        if (validQuestCache.isEmpty()) {
            plugin.getPluginLogger().severe("Keine gültigen Quests verfügbar!");
            currentQuest = null;
            return;
        }

        // Wähle zufällige Quest aus Cache
        List<Quest> quests = new ArrayList<>(validQuestCache.values());
        currentQuest = quests.get(random.nextInt(quests.size()));
        questStartTime = System.currentTimeMillis();

        plugin.getPluginLogger().info("Neue Quest: " + currentQuest.requiredItem +
                " → " + currentQuest.rewardItem);

        saveToData();
        announceNewQuest();
    }

    private void announceNewQuest() {
        try {
            questLock.readLock().lock();

            if (currentQuest == null) return;

            String message = ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    ChatColor.YELLOW + "⚡ " + ChatColor.GOLD + ChatColor.BOLD +
                    "NEUE QUEST VERFÜGBAR" + ChatColor.YELLOW + " ⚡\n" +
                    ChatColor.GRAY + "Benötigt: " + ChatColor.WHITE + currentQuest.requiredItem + "\n" +
                    ChatColor.GRAY + "Belohnung: " + ChatColor.GREEN + currentQuest.rewardItem;

            if (currentQuest.moneyReward > 0) {
                message += "\n" + ChatColor.GRAY + "Geld: " + ChatColor.GOLD +
                        String.format("%.2f", currentQuest.moneyReward) + "$";
            }

            message += "\n" + ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

            final String finalMessage = message;

            // Async Broadcast für Performance
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID uuid : trackedPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(finalMessage);
                    }
                }
            });
        } finally {
            questLock.readLock().unlock();
        }
    }

    public void addTrackedPlayer(UUID uuid) {
        if (trackedPlayers.add(uuid)) {
            // Async Save für Performance
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveToData);
        }
    }

    public void startQuestTimer() {
        questTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                questLock.readLock().lock();

                long now = System.currentTimeMillis();

                // Auto-Cycling nach 5 Stunden
                if (now >= nextQuestAvailable) {
                    long timeSinceStart = now - questStartTime;
                    long fiveHours = 5 * 60 * 60 * 1000;

                    if (timeSinceStart >= fiveHours) {
                        plugin.getPluginLogger().info("Quest-Timeout - Neue Quest wird ausgewählt");

                        questLock.readLock().unlock();
                        questLock.writeLock().lock();
                        try {
                            selectRandomQuest();
                            completedPlayers.clear();
                        } finally {
                            questLock.writeLock().unlock();
                            questLock.readLock().lock();
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getPluginLogger().severe("Fehler im Quest-Timer: " + e.getMessage());
            } finally {
                questLock.readLock().unlock();
            }
        }, 20L * 60L, 20L * 60L); // Jede Minute
    }

    public boolean isQuestAvailable() {
        try {
            questLock.readLock().lock();
            return System.currentTimeMillis() >= nextQuestAvailable && currentQuest != null;
        } finally {
            questLock.readLock().unlock();
        }
    }

    public long getTimeUntilAvailable() {
        try {
            questLock.readLock().lock();
            long diff = nextQuestAvailable - System.currentTimeMillis();
            return Math.max(0, diff);
        } finally {
            questLock.readLock().unlock();
        }
    }

    public Quest getCurrentQuest() {
        try {
            questLock.readLock().lock();
            return currentQuest;
        } finally {
            questLock.readLock().unlock();
        }
    }

    public boolean hasCompletedCurrentQuest(UUID playerUUID) {
        return completedPlayers.contains(playerUUID);
    }

    public void markQuestCompleted(UUID playerUUID) {
        completedPlayers.add(playerUUID);
    }

    public void completeQuestForPlayer(Player player) {
        try {
            questLock.writeLock().lock();

            if (currentQuest == null) {
                plugin.getLogger().warning("completeQuest aufgerufen aber Quest ist null");
                return;
            }

            // Geld geben
            if (economy != null && currentQuest.moneyReward > 0) {
                try {
                    EconomyResponse response = economy.depositPlayer(player, currentQuest.moneyReward);

                    if (response.transactionSuccess()) {
                        player.sendMessage(ChatColor.GOLD + "+" +
                                String.format("%.2f", response.amount) + "$ erhalten!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Fehler: " + response.errorMessage);
                        plugin.getPluginLogger().severe("Economy-Fehler für " + player.getName() +
                                ": " + response.errorMessage);
                    }
                } catch (Exception e) {
                    plugin.getPluginLogger().severe("Exception beim Geld geben: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Cooldown berechnen
            long cooldownMinutes = player.getUniqueId().equals(lastCompletedPlayer) ? 60 : 30;

            nextQuestAvailable = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000);
            lastCompletedPlayer = player.getUniqueId();

            player.sendMessage(ChatColor.YELLOW + "Nächste Quest in: " + cooldownMinutes + " Minuten");

            saveToData();

            plugin.getPluginLogger().info(player.getName() + " hat Quest abgeschlossen (" +
                    cooldownMinutes + "min Cooldown)");

        } finally {
            questLock.writeLock().unlock();
        }
    }

    public void shutdown() {
        if (questTask != null) {
            questTask.cancel();
        }
        saveToData();

        trackedPlayers.clear();
        completedPlayers.clear();
        validQuestCache.clear();
    }

    public void reload() {
        try {
            questLock.writeLock().lock();

            validQuestCache.clear();
            validateQuestCache();

            if (currentQuest == null ||
                    !validateOraxenItems(currentQuest.requiredItem, currentQuest.rewardItem)) {
                plugin.getPluginLogger().info("Quest nach Reload ungültig - wähle neue");
                selectRandomQuest();
            }
        } finally {
            questLock.writeLock().unlock();
        }
    }

    public static class Quest {
        final String requiredItem;
        final String rewardItem;
        final double moneyReward;

        Quest(String requiredItem, String rewardItem, double moneyReward) {
            this.requiredItem = requiredItem;
            this.rewardItem = rewardItem;
            this.moneyReward = moneyReward;
        }

        public String getRequiredItem() { return requiredItem; }
        public String getRewardItem() { return rewardItem; }
        public double getMoneyReward() { return moneyReward; }
    }
}