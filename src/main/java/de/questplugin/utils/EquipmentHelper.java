package de.questplugin.utils;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.MobEquipmentSlot;
import de.questplugin.managers.MobEquipmentManager;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Zentrale Helper-Klasse für Mob-Equipment Operationen
 *
 * Verhindert Code-Duplikation zwischen:
 * - RaidInstance.java
 * - CustomMob.java
 * - MobSpawnListener.java
 *
 * WICHTIG: Diese Klasse übernimmt die GESAMTE Equipment-Logik
 */
public class EquipmentHelper {

    /**
     * Wendet Equipment auf einen Mob an basierend auf MobEquipmentManager Config
     *
     * Diese Methode:
     * 1. Holt Equipment-Config für den Mob-Typ
     * 2. Würfelt für jedes Equipment-Item (Chance-basiert)
     * 3. Setzt Items in die entsprechenden Slots
     * 4. Setzt Drop-Chancen
     *
     * @param mob Der Mob auf den Equipment angewendet werden soll
     * @param equipmentManager Der Equipment-Manager
     * @param plugin Plugin-Instanz für Logging
     * @return true wenn Equipment angewendet wurde, false wenn nicht möglich
     */
    public static boolean applyEquipmentConfig(LivingEntity mob,
                                               MobEquipmentManager equipmentManager,
                                               OraxenQuestPlugin plugin) {
        // Prüfe ob Mob Equipment tragen kann
        if (!canWearEquipment(mob)) {
            plugin.getPluginLogger().debug("Mob " + mob.getType() + " kann kein Equipment tragen");
            return false;
        }

        // Hole Equipment-Liste aus Config
        List<MobEquipmentManager.EquipmentEntry> equipment =
                equipmentManager.getEquipment(mob.getType());

        if (equipment.isEmpty()) {
            return false;
        }

        EntityEquipment entityEquipment = mob.getEquipment();
        if (entityEquipment == null) {
            plugin.getPluginLogger().debug("Entity hat kein Equipment-Interface");
            return false;
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
                ItemStack item = OraxenItemHelper.buildItem(entry.getOraxenItemId(), plugin);

                if (item != null) {
                    // Setze Item in Slot
                    setEquipmentSlot(entityEquipment, entry.getSlot(), item);

                    // Setze Drop-Chance
                    setDropChance(entityEquipment, entry.getSlot(), entry.getDropChance());

                    equipped++;

                    plugin.getPluginLogger().debug("    → Equipt mit Drop-Chance " +
                            (entry.getDropChance() * 100) + "%");
                }
            }
        }

        if (equipped > 0) {
            plugin.getPluginLogger().debug("Total equipt: " + equipped + " Items");
        }

        return equipped > 0;
    }

    /**
     * Setzt ein einzelnes Item in einen Equipment-Slot
     *
     * @param equipment EntityEquipment
     * @param slot Der Slot
     * @param item Das Item
     */
    public static void setEquipmentSlot(EntityEquipment equipment,
                                        MobEquipmentSlot slot,
                                        ItemStack item) {
        switch (slot) {
            case MAIN_HAND:
                equipment.setItemInMainHand(item);
                break;
            case OFF_HAND:
                equipment.setItemInOffHand(item);
                break;
            case HELMET:
                equipment.setHelmet(item);
                break;
            case CHESTPLATE:
                equipment.setChestplate(item);
                break;
            case LEGGINGS:
                equipment.setLeggings(item);
                break;
            case BOOTS:
                equipment.setBoots(item);
                break;
        }
    }

    /**
     * Setzt Drop-Chance für einen Equipment-Slot
     *
     * @param equipment EntityEquipment
     * @param slot Der Slot
     * @param dropChance Drop-Chance (0.0 - 1.0)
     */
    public static void setDropChance(EntityEquipment equipment,
                                     MobEquipmentSlot slot,
                                     float dropChance) {
        switch (slot) {
            case MAIN_HAND:
                equipment.setItemInMainHandDropChance(dropChance);
                break;
            case OFF_HAND:
                equipment.setItemInOffHandDropChance(dropChance);
                break;
            case HELMET:
                equipment.setHelmetDropChance(dropChance);
                break;
            case CHESTPLATE:
                equipment.setChestplateDropChance(dropChance);
                break;
            case LEGGINGS:
                equipment.setLeggingsDropChance(dropChance);
                break;
            case BOOTS:
                equipment.setBootsDropChance(dropChance);
                break;
        }
    }

    /**
     * Prüft ob ein Mob Equipment tragen kann
     *
     * @param mob Der Mob
     * @return true wenn Equipment möglich
     */
    public static boolean canWearEquipment(LivingEntity mob) {
        return mob instanceof Mob && mob.getEquipment() != null;
    }

    /**
     * Validiert ob Oraxen-Item existiert
     *
     * @param oraxenItemId Die Oraxen-Item ID
     * @return true wenn Item existiert
     */
    public static boolean validateItem(String oraxenItemId) {
        if (oraxenItemId == null || oraxenItemId.isEmpty()) {
            return false;
        }

        try {
            return OraxenItems.exists(oraxenItemId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kopiert Equipment von einem Mob zu einem anderen
     *
     * Nützlich für:
     * - Mob-Cloning
     * - Equipment-Transfer
     *
     * @param source Quell-Mob
     * @param target Ziel-Mob
     * @return true wenn erfolgreich
     */
    public static boolean copyEquipment(LivingEntity source, LivingEntity target) {
        if (!canWearEquipment(source) || !canWearEquipment(target)) {
            return false;
        }

        EntityEquipment sourceEquip = source.getEquipment();
        EntityEquipment targetEquip = target.getEquipment();

        if (sourceEquip == null || targetEquip == null) {
            return false;
        }

        // Kopiere alle Slots
        targetEquip.setItemInMainHand(sourceEquip.getItemInMainHand());
        targetEquip.setItemInOffHand(sourceEquip.getItemInOffHand());
        targetEquip.setHelmet(sourceEquip.getHelmet());
        targetEquip.setChestplate(sourceEquip.getChestplate());
        targetEquip.setLeggings(sourceEquip.getLeggings());
        targetEquip.setBoots(sourceEquip.getBoots());

        // Kopiere Drop-Chancen
        targetEquip.setItemInMainHandDropChance(sourceEquip.getItemInMainHandDropChance());
        targetEquip.setItemInOffHandDropChance(sourceEquip.getItemInOffHandDropChance());
        targetEquip.setHelmetDropChance(sourceEquip.getHelmetDropChance());
        targetEquip.setChestplateDropChance(sourceEquip.getChestplateDropChance());
        targetEquip.setLeggingsDropChance(sourceEquip.getLeggingsDropChance());
        targetEquip.setBootsDropChance(sourceEquip.getBootsDropChance());

        return true;
    }

    /**
     * Entfernt alles Equipment von einem Mob
     *
     * @param mob Der Mob
     * @return true wenn erfolgreich
     */
    public static boolean clearEquipment(LivingEntity mob) {
        if (!canWearEquipment(mob)) {
            return false;
        }

        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return false;
        }

        equipment.setItemInMainHand(null);
        equipment.setItemInOffHand(null);
        equipment.setHelmet(null);
        equipment.setChestplate(null);
        equipment.setLeggings(null);
        equipment.setBoots(null);

        return true;
    }

    /**
     * Gibt Equipment-Info als String zurück (für Debug)
     *
     * @param mob Der Mob
     * @return String mit Equipment-Info
     */
    public static String getEquipmentInfo(LivingEntity mob) {
        if (!canWearEquipment(mob)) {
            return "Kann kein Equipment tragen";
        }

        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return "Kein Equipment-Interface";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Equipment von ").append(mob.getType()).append(":\n");

        ItemStack mainHand = equipment.getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir()) {
            sb.append("  Main Hand: ").append(mainHand.getType())
                    .append(" (Drop: ").append(equipment.getItemInMainHandDropChance() * 100).append("%)\n");
        }

        ItemStack offHand = equipment.getItemInOffHand();
        if (offHand != null && !offHand.getType().isAir()) {
            sb.append("  Off Hand: ").append(offHand.getType())
                    .append(" (Drop: ").append(equipment.getItemInOffHandDropChance() * 100).append("%)\n");
        }

        ItemStack helmet = equipment.getHelmet();
        if (helmet != null && !helmet.getType().isAir()) {
            sb.append("  Helmet: ").append(helmet.getType())
                    .append(" (Drop: ").append(equipment.getHelmetDropChance() * 100).append("%)\n");
        }

        ItemStack chestplate = equipment.getChestplate();
        if (chestplate != null && !chestplate.getType().isAir()) {
            sb.append("  Chestplate: ").append(chestplate.getType())
                    .append(" (Drop: ").append(equipment.getChestplateDropChance() * 100).append("%)\n");
        }

        ItemStack leggings = equipment.getLeggings();
        if (leggings != null && !leggings.getType().isAir()) {
            sb.append("  Leggings: ").append(leggings.getType())
                    .append(" (Drop: ").append(equipment.getLeggingsDropChance() * 100).append("%)\n");
        }

        ItemStack boots = equipment.getBoots();
        if (boots != null && !boots.getType().isAir()) {
            sb.append("  Boots: ").append(boots.getType())
                    .append(" (Drop: ").append(equipment.getBootsDropChance() * 100).append("%)\n");
        }

        if (sb.length() == ("Equipment von " + mob.getType() + ":\n").length()) {
            sb.append("  [Kein Equipment]");
        }

        return sb.toString();
    }
}