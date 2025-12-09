package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class QuestManager {

    private final OraxenQuestPlugin plugin;
    private Quest currentQuest;
    private long questStartTime;
    private long nextQuestAvailable;
    private UUID lastCompletedPlayer;
    private BukkitTask questTask;
    private final Random random;
    private Economy economy;
    private final Set<UUID> trackedPlayers;

    public QuestManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.trackedPlayers = new HashSet<>();
        setupEconomy();
        loadFromData();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault nicht gefunden!");
            return;
        }

        try {
            var rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                plugin.getLogger().warning("Keine Economy-Provider gefunden!");
                return;
            }
            economy = rsp.getProvider();
            if (economy != null) {
                plugin.getLogger().info("Vault Economy erfolgreich geladen: " + economy.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Laden von Vault: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFromData() {
        // Lade Quest-Daten
        String requiredItem = plugin.getDataManager().getQuestRequiredItem();
        String rewardItem = plugin.getDataManager().getQuestRewardItem();
        double moneyReward = plugin.getDataManager().getQuestMoneyReward();

        if (requiredItem != null && rewardItem != null) {
            // Validiere ob Items existieren
            if (validateOraxenItems(requiredItem, rewardItem)) {
                currentQuest = new Quest(requiredItem, rewardItem, moneyReward);
                //plugin.getLogger().info("Quest aus Daten geladen: " + requiredItem + " -> " + rewardItem);
            } else {
                //plugin.getLogger().warning("Gespeicherte Quest ungültig, wähle neue Quest");
                selectRandomQuest();
            }
        } else {
            selectRandomQuest();
        }

        questStartTime = plugin.getDataManager().getQuestStartTime();
        nextQuestAvailable = plugin.getDataManager().getNextQuestAvailable();

        // Lade letzten Spieler
        String lastPlayerUUID = plugin.getDataManager().getLastCompletedPlayerUUID();
        if (lastPlayerUUID != null && !lastPlayerUUID.isEmpty()) {
            try {
                lastCompletedPlayer = UUID.fromString(lastPlayerUUID);
            } catch (IllegalArgumentException e) {
                //plugin.getLogger().warning("Ungültige UUID für letzten Spieler: " + lastPlayerUUID);
            }
        }

        // Lade Tracked Players
        trackedPlayers.addAll(plugin.getDataManager().loadTrackedPlayers());
        plugin.getLogger().info(trackedPlayers.size() + " getrackte Spieler geladen");
    }

    private boolean validateOraxenItems(String... itemIds) {
        for (String itemId : itemIds) {
            if (itemId == null || itemId.isEmpty()) {
                return false;
            }
            // Nutze die offizielle Oraxen API Methode
            if (!OraxenItems.exists(itemId)) {
                plugin.getLogger().warning("Oraxen Item nicht gefunden: " + itemId);
                return false;
            }
        }
        return true;
    }

    private void saveToData() {
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
    }

    private void selectRandomQuest() {
        ConfigurationSection questsSection = plugin.getConfig().getConfigurationSection("quests");
        if (questsSection == null) {
            plugin.getLogger().warning("Keine Quests in der Config gefunden!");
            currentQuest = null;
            return;
        }

        List<String> questKeys = new ArrayList<>(questsSection.getKeys(false));
        if (questKeys.isEmpty()) {
            plugin.getLogger().warning("Keine Quests konfiguriert!");
            currentQuest = null;
            return;
        }

        // Mische Quest-Keys für zufällige Auswahl
        Collections.shuffle(questKeys);

        // Versuche gültige Quest zu finden
        for (String questKey : questKeys) {
            ConfigurationSection questData = questsSection.getConfigurationSection(questKey);

            if (questData != null) {
                String requiredItem = questData.getString("required-item");
                String rewardItem = questData.getString("reward-item");
                double money = questData.getDouble("money-reward", 0);

                // Validiere Items
                if (validateOraxenItems(requiredItem, rewardItem)) {
                    currentQuest = new Quest(requiredItem, rewardItem, money);
                    questStartTime = System.currentTimeMillis();

                    plugin.getLogger().info("Neue Quest ausgewählt: " + requiredItem + " -> " + rewardItem);

                    // Speichere neue Quest
                    saveToData();

                    // Announce neue Quest an alle getrackte Spieler
                    announceNewQuest();
                    return;
                } else {
                    plugin.getLogger().warning("Quest '" + questKey + "' übersprungen - ungültige Oraxen Items");
                }
            }
        }

        // Keine gültige Quest gefunden
        plugin.getLogger().severe("Keine gültige Quest gefunden! Alle Oraxen Items fehlen!");
        currentQuest = null;
    }

    private void announceNewQuest() {
        if (currentQuest == null) return;

        String message = ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                ChatColor.YELLOW + "⚡ " + ChatColor.GOLD + ChatColor.BOLD + "NEUE QUEST VERFÜGBAR" + ChatColor.YELLOW + " ⚡\n" +
                ChatColor.GRAY + "Benötigt: " + ChatColor.WHITE + currentQuest.requiredItem + "\n" +
                ChatColor.GRAY + "Belohnung: " + ChatColor.GREEN + currentQuest.rewardItem;

        if (currentQuest.moneyReward > 0) {
            message += "\n" + ChatColor.GRAY + "Geld: " + ChatColor.GOLD + currentQuest.moneyReward + "$";
        }

        message += "\n" + ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        for (UUID uuid : trackedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    public void addTrackedPlayer(UUID uuid) {
        if (trackedPlayers.add(uuid)) {
            // Speichere nur wenn neu hinzugefügt
            saveToData();
        }
    }

    public void startQuestTimer() {
        questTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            // Prüfe ob Quest verfügbar ist
            if (now >= nextQuestAvailable) {
                // Quest nach 5 Stunden automatisch wechseln wenn niemand sie gemacht hat
                long timeSinceStart = now - questStartTime;
                long fiveHours = 5 * 60 * 60 * 1000;

                if (timeSinceStart >= fiveHours) {
                    plugin.getLogger().info("Quest-Timeout nach 5 Stunden - Neue Quest wird ausgewählt");
                    selectRandomQuest();
                }
            }
        }, 20L * 60L, 20L * 60L); // Jede Minute prüfen
    }

    public boolean isQuestAvailable() {
        return System.currentTimeMillis() >= nextQuestAvailable && currentQuest != null;
    }

    public long getTimeUntilAvailable() {
        long diff = nextQuestAvailable - System.currentTimeMillis();
        return diff > 0 ? diff : 0;
    }

    public Quest getCurrentQuest() {
        return currentQuest;
    }

    public void completeQuestForPlayer(Player player) {
        if (currentQuest == null) {
            plugin.getLogger().warning("completeQuestForPlayer aufgerufen aber currentQuest ist null");
            return;
        }

        //plugin.getLogger().info("completeQuestForPlayer für " + player.getName());
        //plugin.getLogger().info("Economy verfügbar: " + (economy != null));
        //plugin.getLogger().info("Geld-Belohnung: " + currentQuest.moneyReward);

        // Geld geben (wird NUR hier gegeben, nicht im Trade-Listener)
        if (economy != null && currentQuest.moneyReward > 0) {
            try {
                double balanceBefore = economy.getBalance(player);
                plugin.getLogger().info("Balance vorher: " + balanceBefore);

                // Nutze EconomyResponse wie in der offiziellen Dokumentation
                EconomyResponse response = economy.depositPlayer(player, currentQuest.moneyReward);

                if (response.transactionSuccess()) {
                    double balanceAfter = economy.getBalance(player);
                    //plugin.getLogger().info("✓ Transaktion erfolgreich!");
                    //plugin.getLogger().info("Geld gegeben: " + response.amount);
                    //plugin.getLogger().info("Balance nachher: " + balanceAfter);

                    player.sendMessage(ChatColor.GOLD + "+" + String.format("%.2f", response.amount) + "$ erhalten!");
                } else {
                    //plugin.getLogger().severe("✗ Transaktion fehlgeschlagen!");
                    //plugin.getLogger().severe("Fehler: " + response.errorMessage);
                    player.sendMessage(ChatColor.RED + "Fehler beim Geld geben: " + response.errorMessage);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Exception beim Geld geben: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (economy == null) {
                plugin.getLogger().warning("Economy ist null - Vault oder Economy-Plugin nicht geladen?");
                player.sendMessage(ChatColor.RED + "Economy-System nicht verfügbar!");
            }
            if (currentQuest.moneyReward <= 0) {
                plugin.getLogger().info("Keine Geld-Belohnung konfiguriert");
            }
        }

        // Cooldown setzen - IMMER, auch wenn es nur eine Quest gibt
        long cooldownMinutes;
        if (player.getUniqueId().equals(lastCompletedPlayer)) {
            // Selber Spieler hat letzte Quest gemacht: 1 Stunde
            cooldownMinutes = 60;
            player.sendMessage(ChatColor.YELLOW + "Du hast die letzte Quest bereits abgeschlossen!");
            player.sendMessage(ChatColor.YELLOW + "Nächste Quest in: 1 Stunde");
        } else {
            // Anderer Spieler: 30 Minuten
            cooldownMinutes = 30;
            player.sendMessage(ChatColor.YELLOW + "Nächste Quest in: " + cooldownMinutes + " Minuten");
        }

        nextQuestAvailable = System.currentTimeMillis() + (cooldownMinutes * 60 * 1000);
        lastCompletedPlayer = player.getUniqueId();

        // Speichere Änderungen
        saveToData();

        plugin.getLogger().info(player.getName() + " hat Quest abgeschlossen - Cooldown: " + cooldownMinutes + " Minuten");
        plugin.getLogger().info("nextQuestAvailable: " + new java.util.Date(nextQuestAvailable));
    }

    public void shutdown() {
        if (questTask != null) {
            questTask.cancel();
        }
        // Finale Speicherung
        saveToData();
    }

    public void reload() {
        // Wenn keine Quest aktiv ist, versuche eine zu finden
        if (currentQuest == null) {
            plugin.getLogger().info("Keine aktive Quest - suche neue Quest nach Reload");
            selectRandomQuest();
        } else {
            // Validiere aktuelle Quest nach Reload
            if (!validateOraxenItems(currentQuest.requiredItem, currentQuest.rewardItem)) {
                plugin.getLogger().warning("Aktuelle Quest nach Reload ungültig - suche neue Quest");
                selectRandomQuest();
            }
        }
    }

    public static class Quest {
        private final String requiredItem;
        private final String rewardItem;
        private final double moneyReward;

        public Quest(String requiredItem, String rewardItem, double moneyReward) {
            this.requiredItem = requiredItem;
            this.rewardItem = rewardItem;
            this.moneyReward = moneyReward;
        }

        public String getRequiredItem() {
            return requiredItem;
        }

        public String getRewardItem() {
            return rewardItem;
        }

        public double getMoneyReward() {
            return moneyReward;
        }
    }
}