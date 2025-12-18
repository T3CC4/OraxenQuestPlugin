package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Basis-Manager mit gemeinsamen Funktionen
 */
public abstract class BaseManager {

    protected final OraxenQuestPlugin plugin;
    protected boolean debugMode;

    public BaseManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.debugMode = plugin.getConfig().getBoolean("debug-mode", false);
    }

    /**
     * Baut Oraxen-Item aus ID
     */
    protected ItemStack buildItem(String oraxenItemId) {
        if (oraxenItemId == null || oraxenItemId.isEmpty()) {
            warn("Oraxen-Item ID ist null oder leer!");
            return null;
        }

        try {
            // Oraxen API direkt nutzen
            io.th0rgal.oraxen.items.ItemBuilder builder =
                    io.th0rgal.oraxen.api.OraxenItems.getItemById(oraxenItemId);

            if (builder != null) {
                return builder.build();
            }

            warn("Oraxen-Item nicht gefunden: '" + oraxenItemId + "'");
            return null;

        } catch (Exception e) {
            warn("Fehler beim Laden von Oraxen-Item '" + oraxenItemId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Validiert ob Item existiert
     */
    protected boolean validateItem(String oraxenItemId) {
        if (oraxenItemId == null || oraxenItemId.isEmpty()) {
            return false;
        }
        try {
            return io.th0rgal.oraxen.api.OraxenItems.exists(oraxenItemId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lädt Drop-Einträge aus ConfigurationSection
     */
    protected List<DropEntry> loadDropEntries(ConfigurationSection section, String path) {
        List<DropEntry> drops = new ArrayList<>();

        if (section == null) {
            warn(path + ": Section ist null!");
            return drops;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null) continue;

            String itemId = entrySection.getString("oraxen-item");
            double chance = entrySection.getDouble("chance", 0);
            int minAmount = entrySection.getInt("min-amount", 1);
            int maxAmount = entrySection.getInt("max-amount", 1);

            // Validierung
            if (!validateItem(itemId)) {
                warn(path + "." + key + ": Item '" + itemId + "' ungültig");
                continue;
            }

            if (chance <= 0 || chance > 100) {
                warn(path + "." + key + ": Ungültige Chance " + chance + "%");
                continue;
            }

            if (minAmount < 0 || maxAmount < minAmount) {
                warn(path + "." + key + ": Ungültige Mengen (min=" + minAmount + ", max=" + maxAmount + ")");
                continue;
            }

            drops.add(new DropEntry(itemId, chance, minAmount, maxAmount));
        }

        return drops;
    }

    // ==================== LOGGING ====================

    protected void info(String message) {
        plugin.getLogger().info(message);
    }

    protected void warn(String message) {
        plugin.getLogger().warning(message);
    }

    protected void debug(String message) {
        if (debugMode) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Reload-Methode für alle Manager
     */
    public abstract void reload();

    /**
     * Drop-Entry Datenklasse
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