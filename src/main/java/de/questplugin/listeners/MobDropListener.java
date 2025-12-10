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

/**
 * Listener für Mob-Drops
 */
public class MobDropListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public MobDropListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Looting-Level holen
        int lootingLevel = 0;
        if (entity.getKiller() instanceof Player) {
            Player killer = (Player) entity.getKiller();
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            lootingLevel = getLootingLevel(weapon);
        }

        // Custom Drops holen (über MobDropManager)
        List<ItemStack> customDrops = plugin.getMobDropManager()
                .getDrops(entity.getType(), lootingLevel);

        // Füge Drops hinzu
        event.getDrops().addAll(customDrops);
    }

    private int getLootingLevel(ItemStack weapon) {
        if (weapon == null || !weapon.hasItemMeta()) {
            return 0;
        }

        int vanillaLooting = weapon.getEnchantmentLevel(Enchantment.LOOTING);
        int aeLooting = AEAPIHelper.getLootingLevel(weapon);

        return Math.max(vanillaLooting, aeLooting);
    }
}