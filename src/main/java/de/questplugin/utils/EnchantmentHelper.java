package de.questplugin.utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * Zentrale Helper-Klasse für Enchantment-Level Berechnung
 *
 * Kombiniert Vanilla + AdvancedEnchantments für:
 * - Fortune/Luck (Block-Drops)
 * - Looting (Mob-Drops)
 * - Silk Touch (Drop-Prevention)
 *
 * WICHTIG: Gibt immer das HÖCHSTE Level zurück (Vanilla vs AE)
 */
public class EnchantmentHelper {

    // ==================== FORTUNE/LUCK ====================

    /**
     * Holt Fortune/Luck-Level für Block-Drops
     *
     * Kombiniert:
     * - Vanilla Fortune
     * - AdvancedEnchantments Fortune/Luck
     *
     * @param tool Das Werkzeug (Pickaxe, Axe, Shovel, etc.)
     * @return Höchstes Fortune-Level (0 wenn keine)
     */
    public static int getFortuneLevel(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) {
            return 0;
        }

        // Vanilla Fortune
        int vanillaFortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);

        // AdvancedEnchantments Fortune/Luck
        int aeFortune = AEAPIHelper.getFortuneLevel(tool);

        // Höchstes Level zurückgeben
        return Math.max(vanillaFortune, aeFortune);
    }

    // ==================== LOOTING ====================

    /**
     * Holt Looting-Level für Mob-Drops
     *
     * Kombiniert:
     * - Vanilla Looting
     * - AdvancedEnchantments Looting
     *
     * @param weapon Die Waffe (Schwert, Axt, etc.)
     * @return Höchstes Looting-Level (0 wenn keine)
     */
    public static int getLootingLevel(ItemStack weapon) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return 0;
        }

        // Vanilla Looting
        int vanillaLooting = weapon.getEnchantmentLevel(Enchantment.LOOTING);

        // AdvancedEnchantments Looting
        int aeLooting = AEAPIHelper.getLootingLevel(weapon);

        // Höchstes Level zurückgeben
        return Math.max(vanillaLooting, aeLooting);
    }

    // ==================== SILK TOUCH ====================

    /**
     * Prüft ob Tool Silk Touch hat (Vanilla oder AE)
     *
     * Bei Silk Touch sollten KEINE Custom-Drops gegeben werden!
     *
     * @param tool Das Werkzeug
     * @return true wenn Silk Touch aktiv ist
     */
    public static boolean hasSilkTouch(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) {
            return false;
        }

        // Vanilla Silk Touch
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return true;
        }

        // AdvancedEnchantments Silk Touch
        if (AEAPIHelper.isAvailable()) {
            int aeSilkTouch = AEAPIHelper.getEnchantmentLevel(tool, "Silk Touch");
            if (aeSilkTouch > 0) {
                return true;
            }
        }

        return false;
    }

    // ==================== EFFICIENCY ====================

    /**
     * Holt Efficiency-Level (für Veinminer-Detection)
     *
     * @param tool Das Werkzeug
     * @return Höchstes Efficiency-Level (0 wenn keine)
     */
    public static int getEfficiencyLevel(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) {
            return 0;
        }

        // Vanilla Efficiency
        int vanillaEfficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);

        // AdvancedEnchantments Efficiency
        int aeEfficiency = AEAPIHelper.getEfficiencyLevel(tool);

        return Math.max(vanillaEfficiency, aeEfficiency);
    }

    // ==================== UNBREAKING ====================

    /**
     * Holt Unbreaking-Level
     *
     * @param item Das Item
     * @return Höchstes Unbreaking-Level (0 wenn keine)
     */
    public static int getUnbreakingLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        // Vanilla Unbreaking
        int vanillaUnbreaking = item.getEnchantmentLevel(Enchantment.UNBREAKING);

        // AdvancedEnchantments Unbreaking (falls vorhanden)
        int aeUnbreaking = 0;
        if (AEAPIHelper.isAvailable()) {
            aeUnbreaking = AEAPIHelper.getEnchantmentLevel(item, "Unbreaking");
        }

        return Math.max(vanillaUnbreaking, aeUnbreaking);
    }

    // ==================== MENDING ====================

    /**
     * Prüft ob Item Mending hat
     *
     * @param item Das Item
     * @return true wenn Mending aktiv ist
     */
    public static boolean hasMending(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        // Vanilla Mending
        if (item.containsEnchantment(Enchantment.MENDING)) {
            return true;
        }

        // AdvancedEnchantments Mending
        if (AEAPIHelper.isAvailable()) {
            int aeMending = AEAPIHelper.getEnchantmentLevel(item, "Mending");
            if (aeMending > 0) {
                return true;
            }
        }

        return false;
    }

    // ==================== UTILITY ====================

    /**
     * Prüft ob Item irgendein Enchantment hat
     *
     * @param item Das Item
     * @return true wenn mindestens ein Enchantment vorhanden ist
     */
    public static boolean hasAnyEnchantment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        // Vanilla Enchantments
        if (!item.getEnchantments().isEmpty()) {
            return true;
        }

        // AdvancedEnchantments (falls vorhanden)
        // Kann nicht direkt geprüft werden, aber wenn Tool hat z.B. Looting...
        // Für Performance-Gründe nur grundlegende Prüfung
        return false;
    }

    /**
     * Debug: Gibt alle Enchantments eines Items aus
     *
     * @param item Das Item
     * @return String mit allen Enchantments
     */
    public static String getEnchantmentInfo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "No enchantments";
        }

        StringBuilder sb = new StringBuilder();

        // Vanilla
        sb.append("Vanilla: ");
        if (item.getEnchantments().isEmpty()) {
            sb.append("None");
        } else {
            item.getEnchantments().forEach((ench, level) ->
                    sb.append(ench.getKey().getKey()).append(" ").append(level).append(", ")
            );
        }

        sb.append(" | ");

        // AdvancedEnchantments
        sb.append("AE: ");
        if (AEAPIHelper.isAvailable()) {
            int fortune = AEAPIHelper.getFortuneLevel(item);
            int looting = AEAPIHelper.getLootingLevel(item);
            int silkTouch = AEAPIHelper.getEnchantmentLevel(item, "Silk Touch");

            if (fortune > 0) sb.append("Fortune ").append(fortune).append(", ");
            if (looting > 0) sb.append("Looting ").append(looting).append(", ");
            if (silkTouch > 0) sb.append("Silk Touch ").append(silkTouch).append(", ");

            if (fortune == 0 && looting == 0 && silkTouch == 0) {
                sb.append("None");
            }
        } else {
            sb.append("Not Available");
        }

        return sb.toString();
    }

    // ==================== ADVANCED ====================

    /**
     * Berechnet effektive Drop-Chance mit Enchantment-Bonus
     *
     * Nutzt DropMechanics für fortgeschrittene Berechnung
     *
     * @param baseChance Basis-Chance in %
     * @param enchantLevel Fortune/Looting Level
     * @return Finale Chance in %
     */
    public static double calculateDropChance(double baseChance, int enchantLevel) {
        return DropMechanics.calculateDropChance(baseChance, enchantLevel);
    }

    /**
     * Berechnet Drop-Menge mit Enchantment-Bonus
     *
     * @param minAmount Min-Menge
     * @param maxAmount Max-Menge
     * @param enchantLevel Fortune/Looting Level
     * @return Finale Menge
     */
    public static int calculateDropAmount(int minAmount, int maxAmount, int enchantLevel) {
        return DropMechanics.calculateDropAmount(minAmount, maxAmount, enchantLevel);
    }
}