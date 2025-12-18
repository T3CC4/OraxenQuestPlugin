package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.utils.EnchantmentHelper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Listener für Mob-Drops
 *
 * OPTIMIERT: Nutzt EnchantmentHelper statt duplizierte Logik
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
        // OPTIMIERT: EnchantmentHelper statt duplizierte Methode
        int lootingLevel = 0;
        if (entity.getKiller() instanceof Player) {
            Player killer = (Player) entity.getKiller();
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            lootingLevel = EnchantmentHelper.getLootingLevel(weapon);
        }

        // Custom Drops holen (über MobDropManager)
        List<ItemStack> customDrops = plugin.getMobDropManager()
                .getDrops(entity.getType(), lootingLevel);

        // Füge Drops hinzu
        event.getDrops().addAll(customDrops);
    }
}