package de.questplugin;

import de.questplugin.utils.AEAPIHelper;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import de.questplugin.listeners.*;
import de.questplugin.managers.*;
import de.questplugin.commands.QuestCommand;

public class OraxenQuestPlugin extends JavaPlugin {

    private static OraxenQuestPlugin instance;
    private DataManager dataManager;
    private DropManager dropManager;
    private QuestManager questManager;
    private ChestManager chestManager;

    @Override
    public void onEnable() {
        instance = this;

        // Prüfe ob Oraxen verfügbar ist
        if (Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            getLogger().severe("Oraxen nicht gefunden! Plugin wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // AdvancedEnchantments API initialisieren (optional)
        if (AEAPIHelper.initialize()) {
            getLogger().info("✓ AdvancedEnchantments API erkannt - Fortune/Luck & Looting Support aktiviert!");
        } else {
            getLogger().info("AdvancedEnchantments nicht gefunden - Vanilla Enchants werden genutzt");
        }

        // Config erstellen
        saveDefaultConfig();

        // Manager initialisieren
        dataManager = new DataManager(this);
        dropManager = new DropManager(this);
        chestManager = new ChestManager(this);
        questManager = new QuestManager(this);

        // Listener registrieren
        Bukkit.getPluginManager().registerEvents(new MobDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NPCInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TradeCompleteListener(this), this);

        // Commands registrieren
        getCommand("quest").setExecutor(new QuestCommand(this));

        // Quest Timer starten
        questManager.startQuestTimer();

        getLogger().info("OraxenQuestPlugin erfolgreich gestartet!");
    }

    @Override
    public void onDisable() {
        if (questManager != null) {
            questManager.shutdown();
        }
        if (chestManager != null) {
            chestManager.saveData();
        }
        getLogger().info("OraxenQuestPlugin deaktiviert!");
    }

    public static OraxenQuestPlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public DropManager getDropManager() {
        return dropManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }
}