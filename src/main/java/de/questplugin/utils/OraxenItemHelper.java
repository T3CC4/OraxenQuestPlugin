package de.questplugin.utils;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.inventory.ItemStack;

public class OraxenItemHelper {
    public static ItemStack buildItem(String oraxenItemId, OraxenQuestPlugin plugin) {
        if (oraxenItemId == null || oraxenItemId.isEmpty()) {
            return null;
        }
        try {
            var builder = io.th0rgal.oraxen.api.OraxenItems.getItemById(oraxenItemId);
            return builder != null ? builder.build() : null;
        } catch (Exception e) {
            plugin.getPluginLogger().warn("Item-Fehler '" + oraxenItemId + "': " + e.getMessage());
            return null;
        }
    }

    public static boolean validate(String oraxenItemId) {
        try {
            return io.th0rgal.oraxen.api.OraxenItems.exists(oraxenItemId);
        } catch (Exception e) {
            return false;
        }
    }
}