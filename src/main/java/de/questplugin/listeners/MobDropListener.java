package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.utils.AEAPIHelper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

import java.util.List;

public class MobDropListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public MobDropListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Hole Looting-Level wenn Killer ein Spieler ist
        int lootingLevel = 0;
        if (entity.getKiller() instanceof Player) {
            Player killer = entity.getKiller();
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            lootingLevel = getLootingLevel(weapon);
        }

        // Hole Custom Drops mit Looting
        List<ItemStack> customDrops = plugin.getDropManager().getMobDrops(entity.getType(), lootingLevel);

        // Debug-Log
        if (lootingLevel > 0 && !customDrops.isEmpty()) {
            plugin.getLogger().info("Mob-Drop mit Looting " + lootingLevel + ": " + customDrops.size() + " Items");
        }

        // Füge Custom Drops hinzu
        for (ItemStack drop : customDrops) {
            event.getDrops().add(drop);
        }
    }

    /**
     * Holt das Looting-Level von der Waffe
     * - Vanilla Looting
     * - AdvancedEnchantments Looting
     */
    private int getLootingLevel(ItemStack weapon) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return 0;
        }

        // Vanilla Looting
        int vanillaLooting = weapon.getEnchantmentLevel(Enchantment.LOOTING);

        // AdvancedEnchantments Looting
        int aeLooting = AEAPIHelper.getLootingLevel(weapon);

        // Nutze das höchste Level
        int maxLevel = Math.max(vanillaLooting, aeLooting);

        if (aeLooting > 0) {
            plugin.getLogger().fine("AE Looting Level " + aeLooting + " erkannt (Vanilla: " + vanillaLooting + ")");
        }

        return maxLevel;
    }
}