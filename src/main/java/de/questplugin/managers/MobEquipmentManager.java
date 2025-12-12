package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.MobEquipmentSlot;
import de.questplugin.utils.MobHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Mob-Equipment mit MobHelper für case-insensitive Mob-Namen
 */
public class MobEquipmentManager extends BaseManager {

    // Mob -> List<EquipmentEntry>
    private final Map<EntityType, List<EquipmentEntry>> mobEquipment = new ConcurrentHashMap<>();

    public MobEquipmentManager(OraxenQuestPlugin plugin) {
        super(plugin);
        loadMobEquipment();
    }

    private void loadMobEquipment() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob-equipment");
        if (section == null) {
            debug("Keine mob-equipment Sektion in Config");
            return;
        }

        mobEquipment.clear();
        int totalEquipment = 0;
        int invalidMobs = 0;

        for (String mobType : section.getKeys(false)) {
            // CASE-INSENSITIVE mit MobHelper
            EntityType entityType = MobHelper.parseConfigMob(mobType);

            if (entityType == null) {
                warn("Ungültiger Mob-Typ in mob-equipment: '" + mobType + "'");
                warn("  Nutze /quest mobs für gültige Mob-Namen");
                warn("  Beispiel: ZOMBIE, zombie, Zombie (alle funktionieren)");
                invalidMobs++;
                continue;
            }

            List<EquipmentEntry> equipment = loadEquipmentEntries(
                    section.getConfigurationSection(mobType),
                    "mob-equipment." + mobType
            );

            if (!equipment.isEmpty()) {
                mobEquipment.put(entityType, equipment);
                totalEquipment += equipment.size();

                debug("Mob-Equipment: " + entityType + " → " + equipment.size() + " Items");
                debug("  Geladen von Config-Key: '" + mobType + "'");

                // Zeige deutschen Namen wenn verfügbar
                String germanName = MobHelper.getGermanName(entityType);
                if (!germanName.equals(entityType.name())) {
                    debug("  Deutscher Name: " + germanName);
                }

                for (EquipmentEntry entry : equipment) {
                    debug("  - " + entry.slot.getDisplayName() + ": " +
                            entry.oraxenItemId + " (" + entry.chance + "%)");
                }
            }
        }

        info("Mob-Equipment: " + totalEquipment + " Items für " + mobEquipment.size() + " Mobs" +
                (invalidMobs > 0 ? " (" + invalidMobs + " ungültig)" : ""));
    }

    private List<EquipmentEntry> loadEquipmentEntries(ConfigurationSection section, String path) {
        List<EquipmentEntry> equipment = new ArrayList<>();

        if (section == null) {
            warn(path + ": Section ist null!");
            return equipment;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null) continue;

            String itemId = entrySection.getString("oraxen-item");
            String slotStr = entrySection.getString("slot");
            double chance = entrySection.getDouble("chance", 0);
            boolean dropOnDeath = entrySection.getBoolean("drop-on-death", true);
            float dropChance = (float) entrySection.getDouble("drop-chance", 1.0);

            // Validierung
            if (!validateItem(itemId)) {
                warn(path + "." + key + ": Item '" + itemId + "' ungültig");
                continue;
            }

            MobEquipmentSlot slot = MobEquipmentSlot.fromString(slotStr);
            if (slot == null) {
                warn(path + "." + key + ": Ungültiger Slot '" + slotStr + "'");
                warn("Verfügbar: MAIN_HAND, OFF_HAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS");
                continue;
            }

            if (chance <= 0 || chance > 100) {
                warn(path + "." + key + ": Ungültige Chance " + chance + "%");
                continue;
            }

            if (dropChance < 0 || dropChance > 1) {
                warn(path + "." + key + ": drop-chance muss 0.0-1.0 sein (ist " + dropChance + ")");
                dropChance = Math.max(0, Math.min(1, dropChance));
            }

            equipment.add(new EquipmentEntry(
                    itemId,
                    slot,
                    chance,
                    dropOnDeath,
                    dropChance
            ));
        }

        return equipment;
    }

    /**
     * Holt Equipment-Einträge für einen Mob-Typ
     */
    public List<EquipmentEntry> getEquipment(EntityType entityType) {
        return mobEquipment.getOrDefault(entityType, Collections.emptyList());
    }

    /**
     * Prüft ob Mob Equipment haben kann
     */
    public boolean hasEquipment(EntityType entityType) {
        return mobEquipment.containsKey(entityType);
    }

    @Override
    public void reload() {
        mobEquipment.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadMobEquipment();
    }

    /**
     * Equipment-Eintrag Datenklasse
     */
    public static class EquipmentEntry {
        final String oraxenItemId;
        final MobEquipmentSlot slot;
        final double chance;
        final boolean dropOnDeath;
        final float dropChance;

        EquipmentEntry(String oraxenItemId, MobEquipmentSlot slot, double chance,
                       boolean dropOnDeath, float dropChance) {
            this.oraxenItemId = oraxenItemId;
            this.slot = slot;
            this.chance = chance;
            this.dropOnDeath = dropOnDeath;
            this.dropChance = dropChance;
        }

        public String getOraxenItemId() { return oraxenItemId; }
        public MobEquipmentSlot getSlot() { return slot; }
        public double getChance() { return chance; }
        public boolean shouldDropOnDeath() { return dropOnDeath; }
        public float getDropChance() { return dropChance; }
    }
}