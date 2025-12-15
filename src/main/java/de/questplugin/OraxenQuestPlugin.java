package de.questplugin;

import de.questplugin.mobs.abilities.AdvancedAbilities;
import de.questplugin.mobs.api.ExampleAbilities;
import de.questplugin.mobs.api.CustomMobAPI;
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
    private EliteMobManager eliteMobManager;

    // Custom Mobs API
    private CustomMobAPI customMobAPI;

    // Listener
    private BlockBreakListener blockBreakListener;
    private EliteDropListener eliteDropListener;

    // Logger
    private PluginLogger pluginLogger;

    private Economy economy = null;

    @Override
    public void onEnable() {
        instance = this;

        // Logger initialisieren
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
        saveResource("raids.yml", false);
        saveResource("recipes.yml", false);

        // Custom Mobs API initialisieren
        customMobAPI = new CustomMobAPI(this);
        registerDefaultAbilities();

        // Manager initialisieren
        dataManager = new DataManager(this);
        blockDropManager = new BlockDropManager(this);
        mobDropManager = new MobDropManager(this);
        chestManager = new ChestManager(this);
        craftingManager = new CraftingManager(this);
        mobEquipmentManager = new MobEquipmentManager(this);
        questManager = new QuestManager(this);
        raidManager = new RaidManager(this);
        eliteMobManager = new EliteMobManager(this);

        // Listener registrieren
        blockBreakListener = new BlockBreakListener(this);
        Bukkit.getPluginManager().registerEvents(blockBreakListener, this);
        Bukkit.getPluginManager().registerEvents(new MobDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NPCInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TradeCompleteListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AnvilListener(this), this);

        eliteDropListener = new EliteDropListener(this);
        Bukkit.getPluginManager().registerEvents(eliteDropListener, this);
        pluginLogger.info("✓ Elite-Drop-System aktiv!");

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
        pluginLogger.info("✓ Elite-Mobs System aktiv!");
        pluginLogger.info("✓ " + getRegisteredAbilitiesCount() + " Abilities registriert");

        if (getConfig().getBoolean("debug-mode", false)) {
            pluginLogger.info("⚠ Debug-Mode ist AKTIV - Viele Logs werden produziert!");
        }
    }

    /**
     * Registriert Standard-Abilities
     */
    private void registerDefaultAbilities() {
        var abilityManager = customMobAPI.getAbilityManager();

        // Registriere alle Standard-Abilities
        abilityManager.registerAbility("Teleport", new ExampleAbilities.TeleportAbility());
        abilityManager.registerAbility("Regeneration", new ExampleAbilities.RegenerationAbility());
        abilityManager.registerAbility("SpeedBoost", new ExampleAbilities.SpeedBoostAbility());
        abilityManager.registerAbility("Knockback", new ExampleAbilities.KnockbackAbility());
        abilityManager.registerAbility("Invisibility", new ExampleAbilities.InvisibilityAbility());

        // OFFENSIVE
        abilityManager.registerAbility("Fireball", new AdvancedAbilities.FireballAbility());
        abilityManager.registerAbility("Lightning", new AdvancedAbilities.LightningAbility());
        abilityManager.registerAbility("Explosion", new AdvancedAbilities.ExplosionAbility());
        abilityManager.registerAbility("ArrowRain", new AdvancedAbilities.ArrowRainAbility());
        abilityManager.registerAbility("Poison", new AdvancedAbilities.PoisonAbility());

// DEFENSIVE
        abilityManager.registerAbility("Shield", new AdvancedAbilities.ShieldAbility());
        abilityManager.registerAbility("Heal", new AdvancedAbilities.HealAbility());
        abilityManager.registerAbility("Absorb", new AdvancedAbilities.AbsorbAbility());
        abilityManager.registerAbility("Thorns", new AdvancedAbilities.ThornsAbility());

// CROWD CONTROL
        abilityManager.registerAbility("Freeze", new AdvancedAbilities.FreezeAbility());
        abilityManager.registerAbility("Web", new AdvancedAbilities.WebAbility());
        abilityManager.registerAbility("Blind", new AdvancedAbilities.BlindAbility());
        abilityManager.registerAbility("Weakness", new AdvancedAbilities.WeaknessAbility());
        abilityManager.registerAbility("Slowness", new AdvancedAbilities.SlownessAbility());

// SUMMON
        abilityManager.registerAbility("SummonZombies", new AdvancedAbilities.SummonZombiesAbility());
        abilityManager.registerAbility("SummonSkeletons", new AdvancedAbilities.SummonSkeletonsAbility());
        abilityManager.registerAbility("SummonVex", new AdvancedAbilities.SummonVexAbility());

// SPECIAL
        abilityManager.registerAbility("Leap", new AdvancedAbilities.LeapAbility());
        abilityManager.registerAbility("Pull", new AdvancedAbilities.PullAbility());
        abilityManager.registerAbility("Swap", new AdvancedAbilities.SwapAbility());
        abilityManager.registerAbility("Clone", new AdvancedAbilities.CloneAbility());
        abilityManager.registerAbility("Meteor", new AdvancedAbilities.MeteorAbility());

        pluginLogger.debug("Standard-Abilities registriert:");
        pluginLogger.debug("  - Teleport (10s Cooldown)");
        pluginLogger.debug("  - Regeneration (15s Cooldown)");
        pluginLogger.debug("  - SpeedBoost (20s Cooldown)");
        pluginLogger.debug("  - Knockback (8s Cooldown)");
        pluginLogger.debug("  - Invisibility (30s Cooldown)");
    }

    /**
     * Gibt Anzahl registrierter Abilities zurück
     */
    private int getRegisteredAbilitiesCount() {
        return customMobAPI.getAbilityManager().getRegisteredAbilities().size();
    }

    @Override
    public void onDisable() {
        // FIX: Markiere DataManager dass Plugin disabled wird
        if (dataManager != null) {
            dataManager.setDisabling();
        }

        // Stoppe Raids
        if (raidManager != null) {
            raidManager.shutdown();
        }

        // Stoppe Elite-Mobs
        if (eliteMobManager != null) {
            eliteMobManager.shutdown();
        }

        // Stoppe Custom Mobs API
        if (customMobAPI != null) {
            customMobAPI.shutdown();
        }

        // Stoppe alle Tasks
        if (questManager != null) {
            questManager.shutdown();
        }

        // Cleanup Listener
        if (blockBreakListener != null) {
            blockBreakListener.shutdown();
        }

        // Speichere Daten
        if (chestManager != null) {
            chestManager.saveData();
        }

        if (dataManager != null) {
            dataManager.save();
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

    public EliteDropListener getEliteDropListener() {
        return eliteDropListener;
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

    public EliteMobManager getEliteMobManager() {
        return eliteMobManager;
    }

    public CustomMobAPI getCustomMobAPI() {
        return customMobAPI;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public PluginLogger getPluginLogger() {
        return pluginLogger;
    }
}