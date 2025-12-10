package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basis-Manager mit gemeinsamen Funktionen für alle Manager
 */
public abstract class BaseManager {

    protected final OraxenQuestPlugin plugin;
    protected volatile boolean debugMode;

    // Item-Cache für Performance (shared across managers)
    private static final Map<String, ItemStack> ITEM_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    public BaseManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.debugMode = plugin.getConfig().getBoolean("debug-mode", false);
    }

    /**
     * Debug-Log (nur wenn debug-mode aktiviert)
     */
    protected void debug(String message) {
        if (debugMode) {
            plugin.getPluginLogger().info(message);
        }
    }

    /**
     * Info-Log (immer)
     */
    protected void info(String message) {
        plugin.getPluginLogger().info(message);
    }

    /**
     * Warning-Log (immer)
     */
    protected void warn(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * Baut Oraxen-Item mit Caching
     */
    protected ItemStack buildItem(String itemId) {
        // Cache-Lookup
        ItemStack cached = ITEM_CACHE.get(itemId);
        if (cached != null) {
            return cached.clone();
        }

        try {
            ItemStack item = OraxenItems.getItemById(itemId).build();

            if (item != null && ITEM_CACHE.size() < MAX_CACHE_SIZE) {
                ITEM_CACHE.put(itemId, item.clone());
            }

            return item;
        } catch (Exception e) {
            warn("Fehler beim Erstellen von '" + itemId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Validiert Oraxen-Item
     */
    protected boolean validateItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }
        return OraxenItems.exists(itemId);
    }

    /**
     * Lädt Drop-Einträge aus Config-Section
     */
    protected List<DropEntry> loadDropEntries(ConfigurationSection section, String path) {
        List<DropEntry> drops = new ArrayList<>();

        if (section == null) {
            warn(path + ": Section ist null!");
            return drops;
        }

        for (String key : section.getKeys(false)) {
            String itemId = section.getString(key + ".oraxen-item");

            if (!validateItem(itemId)) {
                warn(path + "." + key + ": Item '" + itemId + "' ungültig/nicht vorhanden");
                continue;
            }

            double chance = section.getDouble(key + ".chance", 0);
            int minAmount = section.getInt(key + ".min-amount", 1);
            int maxAmount = section.getInt(key + ".max-amount", 1);

            if (!validateDropEntry(chance, minAmount, maxAmount, path + "." + key)) {
                continue;
            }

            drops.add(new DropEntry(itemId, chance, minAmount, maxAmount));
        }

        return drops;
    }

    /**
     * Validiert Drop-Eintrag
     */
    private boolean validateDropEntry(double chance, int minAmount, int maxAmount, String path) {
        if (chance <= 0 || chance > 100) {
            warn(path + ": Ungültige Chance " + chance + "%");
            return false;
        }

        if (minAmount < 1) {
            warn(path + ": min-amount muss mindestens 1 sein (ist " + minAmount + ")");
            return false;
        }

        if (maxAmount < minAmount) {
            warn(path + ": max-amount (" + maxAmount + ") < min-amount (" + minAmount + ")");
            return false;
        }

        return true;
    }

    /**
     * Setzt Debug-Mode
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        info(getClass().getSimpleName() + " Debug-Mode: " + (enabled ? "AN" : "AUS"));
    }

    /**
     * Reload-Methode für alle Manager
     */
    public abstract void reload();

    /**
     * Drop-Eintrag Datenklasse
     */
    protected static class DropEntry {
        final String oraxenItemId;
        final double chance;
        final int minAmount;
        final int maxAmount;

        DropEntry(String oraxenItemId, double chance, int minAmount, int maxAmount) {
            this.oraxenItemId = oraxenItemId;
            this.chance = chance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }
}