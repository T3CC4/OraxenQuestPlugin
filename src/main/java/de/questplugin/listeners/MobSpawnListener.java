package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.MobEquipmentManager;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Listener für Mob-Spawn mit Custom Equipment
 */
public class MobSpawnListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public MobSpawnListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        // Nur für Mobs mit Equipment-Support
        if (!(entity instanceof Mob)) {
            return;
        }

        // Prüfe ob Equipment konfiguriert
        if (!plugin.getMobEquipmentManager().hasEquipment(entity.getType())) {
            return;
        }

        plugin.getPluginLogger().debug("=== Mob Spawn ===");
        plugin.getPluginLogger().debug("Typ: " + entity.getType());
        plugin.getPluginLogger().debug("Grund: " + event.getSpawnReason());

        // Hole Equipment-Liste
        List<MobEquipmentManager.EquipmentEntry> equipment =
                plugin.getMobEquipmentManager().getEquipment(entity.getType());

        if (equipment.isEmpty()) {
            plugin.getPluginLogger().debug("Keine Equipment-Einträge");
            return;
        }

        EntityEquipment entityEquipment = entity.getEquipment();
        if (entityEquipment == null) {
            plugin.getPluginLogger().debug("Entity hat kein Equipment-Interface");
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int equipped = 0;

        // Verarbeite jedes Equipment
        for (MobEquipmentManager.EquipmentEntry entry : equipment) {
            double roll = random.nextDouble() * 100;
            boolean success = roll < entry.getChance();

            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getPluginLogger().debug("  Equipment: " + entry.getOraxenItemId());
                plugin.getPluginLogger().debug("    Slot: " + entry.getSlot().getDisplayName());
                plugin.getPluginLogger().debug("    Chance: " + entry.getChance() + "%");
                plugin.getPluginLogger().debug("    Roll: " + String.format("%.2f", roll));
                plugin.getPluginLogger().debug("    " + (success ? "✓ EQUIPT" : "✗ SKIP"));
            }

            if (success) {
                ItemStack item = OraxenItems.getItemById(entry.getOraxenItemId()).build();

                if (item != null) {
                    // Setze Item in Slot
                    switch (entry.getSlot()) {
                        case MAIN_HAND:
                            entityEquipment.setItemInMainHand(item);
                            break;
                        case OFF_HAND:
                            entityEquipment.setItemInOffHand(item);
                            break;
                        case HELMET:
                            entityEquipment.setHelmet(item);
                            break;
                        case CHESTPLATE:
                            entityEquipment.setChestplate(item);
                            break;
                        case LEGGINGS:
                            entityEquipment.setLeggings(item);
                            break;
                        case BOOTS:
                            entityEquipment.setBoots(item);
                            break;
                    }

                    // Setze Drop-Chance
                    switch (entry.getSlot()) {
                        case MAIN_HAND:
                            entityEquipment.setItemInMainHandDropChance(entry.getDropChance());
                            break;
                        case OFF_HAND:
                            entityEquipment.setItemInOffHandDropChance(entry.getDropChance());
                            break;
                        case HELMET:
                            entityEquipment.setHelmetDropChance(entry.getDropChance());
                            break;
                        case CHESTPLATE:
                            entityEquipment.setChestplateDropChance(entry.getDropChance());
                            break;
                        case LEGGINGS:
                            entityEquipment.setLeggingsDropChance(entry.getDropChance());
                            break;
                        case BOOTS:
                            entityEquipment.setBootsDropChance(entry.getDropChance());
                            break;
                    }

                    equipped++;

                    plugin.getPluginLogger().debug("    → Equipt mit Drop-Chance " +
                            (entry.getDropChance() * 100) + "%");
                }
            }
        }

        if (equipped > 0) {
            plugin.getPluginLogger().debug("Total equipt: " + equipped + " Items");
        }

        plugin.getPluginLogger().debug("=================");
    }
}