package de.questplugin;

import de.questplugin.utils.AEAPIHelper;
import de.questplugin.utils.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import de.questplugin.listeners.*;
import de.questplugin.managers.*;
import de.questplugin.commands.QuestCommand;

public class OraxenQuestPlugin extends JavaPlugin {

    private static OraxenQuestPlugin instance;

    // Manager
    private DataManager dataManager;
    private BlockDropManager blockDropManager;
    private MobDropManager mobDropManager;
    private QuestManager questManager;
    private ChestManager chestManager;

    // Listener (für Cleanup)
    private BlockBreakListener blockBreakListener;

    // Zentraler Logger mit debug-mode Support
    private PluginLogger pluginLogger;

    @Override
    public void onEnable() {
        instance = this;

        // Logger initialisieren (als erstes!)
        pluginLogger = new PluginLogger(this);

        // Oraxen Check
        if (Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            getLogger().severe("Oraxen nicht gefunden! Plugin wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // AdvancedEnchantments API
        if (AEAPIHelper.initialize()) {
            pluginLogger.info("✓ AdvancedEnchantments erkannt - Custom Enchants aktiv!");
        } else {
            pluginLogger.info("AdvancedEnchantments nicht gefunden - Vanilla Enchants");
        }

        // Config
        saveDefaultConfig();

        // Manager initialisieren
        dataManager = new DataManager(this);
        blockDropManager = new BlockDropManager(this);
        mobDropManager = new MobDropManager(this);
        chestManager = new ChestManager(this);
        questManager = new QuestManager(this);

        // Listener registrieren
        blockBreakListener = new BlockBreakListener(this);
        Bukkit.getPluginManager().registerEvents(blockBreakListener, this);
        Bukkit.getPluginManager().registerEvents(new MobDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NPCInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TradeCompleteListener(this), this);

        // Commands
        QuestCommand questCommand = new QuestCommand(this);
        getCommand("quest").setExecutor(questCommand);
        getCommand("quest").setTabCompleter(questCommand);

        // Quest Timer
        questManager.startQuestTimer();

        pluginLogger.info("OraxenQuestPlugin erfolgreich gestartet!");

        // Debug-Mode Status
        if (getConfig().getBoolean("debug-mode", false)) {
            pluginLogger.info("⚠ Debug-Mode ist AKTIV - Viele Logs werden produziert!");
        }
    }

    @Override
    public void onDisable() {
        // Stoppe alle Tasks
        if (questManager != null) {
            questManager.shutdown();
        }

        // Cleanup Listener
        if (blockBreakListener != null) {
            blockBreakListener.shutdown();
        }

        // Speichere Daten (synchron beim Shutdown!)
        if (chestManager != null) {
            chestManager.saveData();
        }

        if (dataManager != null) {
            dataManager.save(); // Synchron beim Shutdown
        }

        pluginLogger.info("OraxenQuestPlugin deaktiviert!");
    }

    // ==================== GETTER ====================

    public static OraxenQuestPlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public BlockDropManager getBlockDropManager() {
        return blockDropManager;
    }

    public MobDropManager getMobDropManager() {
        return mobDropManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public ChestManager getChestManager() {
        return chestManager;
    }

    /**
     * Zentraler Logger mit debug-mode Support
     */
    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }
}