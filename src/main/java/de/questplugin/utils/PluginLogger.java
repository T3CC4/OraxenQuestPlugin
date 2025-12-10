package de.questplugin.utils;

import de.questplugin.OraxenQuestPlugin;

/**
 * Zentrale Logging-Utility mit debug-mode Support
 *
 * Alle Logs gehen durch diese Klasse, die automatisch
 * debug-mode aus der Config prüft
 */
public class PluginLogger {

    private final OraxenQuestPlugin plugin;

    public PluginLogger(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Debug-Log - NUR wenn debug-mode: true
     */
    public void debug(String message) {
        if (isDebugMode()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Info-Log - IMMER (wichtige Infos)
     * Nutze sparsam!
     */
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    /**
     * Warning-Log - IMMER
     */
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * Error-Log - IMMER
     */
    public void severe(String message) {
        plugin.getLogger().severe(message);
    }

    /**
     * Fine-Log (für tiefe Debug-Infos)
     */
    public void fine(String message) {
        if (isDebugMode()) {
            plugin.getLogger().fine(message);
        }
    }

    /**
     * Prüft debug-mode aus Config
     */
    private boolean isDebugMode() {
        return plugin.getConfig().getBoolean("debug-mode", false);
    }

    /**
     * Statische Methode für schnellen Zugriff
     */
    public static PluginLogger get(OraxenQuestPlugin plugin) {
        return new PluginLogger(plugin);
    }
}