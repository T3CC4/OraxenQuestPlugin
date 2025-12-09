package de.questplugin.utils;

import net.advancedplugins.ae.api.AEAPI;
import org.bukkit.inventory.ItemStack;

/**
 * Utility-Klasse für AdvancedEnchantments API
 * Basierend auf: https://www.spigotmc.org/resources/advancedenchantments-api.76819/
 *
 * AEAPI Methoden sind STATISCH - kein getInstance() nötig!
 *
 * Wichtige Methoden:
 * - AEAPI.getEnchantmentLevel(ItemStack, String) → gibt Level zurück, 0 wenn nicht vorhanden
 * - AEAPI.applyEnchant(String, int, ItemStack) → wendet Enchant an
 * - AEAPI.removeEnchant(ItemStack, String) → entfernt Enchant
 */
public class AEAPIHelper {

    private static boolean available = false;

    /**
     * Initialisiert die AEAPI (wird beim Plugin-Start aufgerufen)
     */
    public static boolean initialize() {
        try {
            // Prüfe ob AEAPI Klasse existiert
            Class.forName("net.advancedplugins.ae.api.AEAPI");
            available = true;
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            available = false;
            return false;
        }
    }

    /**
     * Prüft ob AdvancedEnchantments verfügbar ist
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Prüft ob ein Item ein bestimmtes Custom Enchantment hat
     *
     * @param item Das zu prüfende Item
     * @param enchantName Name des Enchantments (z.B. "Looting", "Luck")
     * @return true wenn das Enchantment vorhanden ist
     */
    public static boolean hasEnchantment(ItemStack item, String enchantName) {
        return getEnchantmentLevel(item, enchantName) > 0;
    }

    /**
     * Holt das Level eines Custom Enchantments
     *
     * @param item Das Item
     * @param enchantName Name des Enchantments
     * @return Level des Enchantments, 0 wenn nicht vorhanden
     */
    public static int getEnchantmentLevel(ItemStack item, String enchantName) {
        if (!isAvailable() || item == null || enchantName == null) {
            return 0;
        }

        try {
            // getEnchantmentLevel gibt 0 zurück wenn Enchant nicht vorhanden ist
            return AEAPI.getEnchantLevel(enchantName, item);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Holt das höchste Level von mehreren möglichen Enchantment-Namen
     *
     * @param item Das Item
     * @param enchantNames Array von möglichen Namen (z.B. ["Looting", "looting", "LOOTING"])
     * @return Höchstes gefundenes Level
     */
    public static int getHighestEnchantmentLevel(ItemStack item, String... enchantNames) {
        if (!isAvailable() || item == null || enchantNames == null) {
            return 0;
        }

        int maxLevel = 0;

        for (String enchantName : enchantNames) {
            int level = getEnchantmentLevel(item, enchantName);
            maxLevel = Math.max(maxLevel, level);
        }

        return maxLevel;
    }

    /**
     * Wendet ein Custom Enchantment auf ein Item an
     *
     * @param item Das Item
     * @param enchantName Name des Enchantments
     * @param level Level des Enchantments
     * @return Das modifizierte Item
     */
    public static ItemStack applyEnchantment(ItemStack item, String enchantName, int level) {
        if (!isAvailable() || item == null || enchantName == null) {
            return item;
        }

        try {
            return AEAPI.applyEnchant(enchantName, level, item);
        } catch (Exception e) {
            return item;
        }
    }

    /**
     * Entfernt ein Custom Enchantment von einem Item
     *
     * @param item Das Item
     * @param enchantName Name des Enchantments
     * @return Das modifizierte Item
     */
    public static ItemStack removeEnchantment(ItemStack item, String enchantName) {
        if (!isAvailable() || item == null || enchantName == null) {
            return item;
        }

        try {
            return AEAPI.removeEnchantment(item, enchantName);
        } catch (Exception e) {
            return item;
        }
    }

    // ========== HÄUFIG VERWENDETE ENCHANTMENTS ==========

    /**
     * Holt Looting-Level (für Mob-Drops)
     */
    public static int getLootingLevel(ItemStack weapon) {
        return getHighestEnchantmentLevel(weapon, "Looting", "looting", "LOOTING");
    }

    /**
     * Holt Fortune/Luck-Level (für Block-Drops)
     */
    public static int getFortuneLevel(ItemStack tool) {
        return getHighestEnchantmentLevel(tool, "Fortune", "Luck", "fortune", "luck");
    }

    /**
     * Holt Veinminer-Level
     */
    public static int getVeinminerLevel(ItemStack tool) {
        return getHighestEnchantmentLevel(tool, "Veinminer", "veinminer", "VeinMiner");
    }

    /**
     * Holt Efficiency-Level
     */
    public static int getEfficiencyLevel(ItemStack tool) {
        return getHighestEnchantmentLevel(tool, "Efficiency", "efficiency", "EFFICIENCY");
    }

    // ========== BLOCK METADATA KOMPATIBILITÄT ==========

    /**
     * Prüft ob ein Block von AdvancedEnchantments ignoriert werden soll
     * Siehe: https://ae.advancedplugins.net/for-developers/plugin-compatiblity-issues
     *
     * @param block Der zu prüfende Block
     * @return true wenn Block ignoriert werden soll
     */
    public static boolean shouldIgnoreBlock(org.bukkit.block.Block block) {
        if (!isAvailable() || block == null) {
            return false;
        }

        try {
            // Nutze Reflection falls AEAPI.ignoreBlockEvent() existiert
            Class<?> aeapiClass = Class.forName("net.advancedplugins.ae.api.AEAPI");
            java.lang.reflect.Method method = aeapiClass.getMethod("ignoreBlockEvent", org.bukkit.block.Block.class);
            return (boolean) method.invoke(null, block);
        } catch (NoSuchMethodException e) {
            // Methode existiert nicht - alte AE Version
            return false;
        } catch (Exception e) {
            // Anderer Fehler
            return false;
        }
    }

    /**
     * Markiert einen Block um von AdvancedEnchantments ignoriert zu werden
     * Verhindert Duplicate-Drops wenn beide Plugins BlockBreakEvent nutzen
     *
     * @param block Der zu markierende Block
     */
    public static void setIgnoreBlockEvent(org.bukkit.block.Block block) {
        if (!isAvailable() || block == null) {
            return;
        }

        try {
            // Nutze Reflection falls AEAPI.setIgnoreBlockEvent() existiert
            Class<?> aeapiClass = Class.forName("net.advancedplugins.ae.api.AEAPI");
            java.lang.reflect.Method method = aeapiClass.getMethod("setIgnoreBlockEvent", org.bukkit.block.Block.class);
            method.invoke(null, block);
        } catch (Exception e) {
            // Ignorieren
        }
    }
}