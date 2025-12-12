package de.questplugin;

import de.questplugin.utils.AEAPIHelper;
import de.questplugin.utils.PluginLogger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import de.questplugin.listeners.*;
import de.questplugin.managers.*;
import de.questplugin.commands.QuestCommand;
import de.questplugin.commands.RaidCommand;

public class OraxenQuestPlugin extends JavaPlugin {

    private static OraxenQuestPlugin instance;

    // Manager
    private DataManager dataManager;
    private BlockDropManager blockDropManager;
    private MobDropManager mobDropManager;
    private QuestManager questManager;
    private ChestManager chestManager;
    private CraftingManager craftingManager;
    private MobEquipmentManager mobEquipmentManager;
    private RaidManager raidManager;

    // Listener (für Cleanup)
    private BlockBreakListener blockBreakListener;

    // Zentraler Logger mit debug-mode Support
    private PluginLogger pluginLogger;

    private Economy economy = null;

    @Override
    public void onEnable() {
        instance = this;

        // Logger initialisieren (als erstes!)
        pluginLogger = new PluginLogger(this);

        if (!setupEconomy()) {
            getLogger().warning("Vault Economy nicht gefunden!");
            getLogger().warning("Geld-Belohnungen werden nicht funktionieren!");
        } else {
            getLogger().info("Vault Economy erfolgreich verbunden!");
        }

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
        craftingManager = new CraftingManager(this);
        mobEquipmentManager = new MobEquipmentManager(this);
        questManager = new QuestManager(this);
        raidManager = new RaidManager(this);

        // Listener registrieren
        blockBreakListener = new BlockBreakListener(this);
        Bukkit.getPluginManager().registerEvents(blockBreakListener, this);
        Bukkit.getPluginManager().registerEvents(new MobDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NPCInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TradeCompleteListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AnvilListener(this), this);

        // Commands
        QuestCommand questCommand = new QuestCommand(this);
        getCommand("quest").setExecutor(questCommand);
        getCommand("quest").setTabCompleter(questCommand);

        RaidCommand raidCommand = new RaidCommand(this);
        getCommand("raid").setExecutor(raidCommand);
        getCommand("raid").setTabCompleter(raidCommand);

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
        // Stoppe Raids
        if (raidManager != null) {
            raidManager.shutdown();
        }

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

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
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

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    public MobEquipmentManager getMobEquipmentManager() {
        return mobEquipmentManager;
    }

    public RaidManager getRaidManager() {
        return raidManager;
    }

    /**
     * Holt Vault Economy (kann null sein!)
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Prüft ob Vault Economy verfügbar ist
     */
    public boolean hasEconomy() {
        return economy != null;
    }

    /**
     * Zentraler Logger mit debug-mode Support
     */
    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }
}