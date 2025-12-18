package de.questplugin.mobs.api;

import de.questplugin.enums.MobEquipmentSlot;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder-Pattern für Custom Mob Erstellung mit Equipment-Support
 */
public class CustomMobBuilder {

    private final CustomMobAPI api;
    private final EntityType type;
    private Location location;
    private String name;
    private int level = 1;
    private Double health;
    private Double damage;
    private Double speed;
    private Double scale;
    private LivingEntity defendTarget;
    private Double defendRadius;
    private DefendMode defendMode;
    private final List<MobAbility> abilities = new ArrayList<>();

    // NEU: Equipment System
    private final Map<MobEquipmentSlot, EquipmentEntry> equipment = new HashMap<>();
    private boolean useEquipmentConfig = false;

    public CustomMobBuilder(CustomMobAPI api, EntityType type) {
        this.api = api;
        this.type = type;
    }

    public CustomMobBuilder at(Location location) {
        this.location = location;
        return this;
    }

    public CustomMobBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CustomMobBuilder withLevel(int level) {
        this.level = level;
        return this;
    }

    public CustomMobBuilder withHealth(double health) {
        this.health = health;
        return this;
    }

    public CustomMobBuilder withDamage(double damage) {
        this.damage = damage;
        return this;
    }

    public CustomMobBuilder withSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public CustomMobBuilder withScale(double scale) {
        this.scale = scale;
        return this;
    }

    // ==================== DEFEND SYSTEM ====================

    public CustomMobBuilder defending(LivingEntity target) {
        this.defendTarget = target;
        this.defendMode = DefendMode.PASSIVE;
        return this;
    }

    public CustomMobBuilder defending(LivingEntity target, double radius) {
        this.defendTarget = target;
        this.defendRadius = radius;
        this.defendMode = DefendMode.PASSIVE;
        return this;
    }

    public CustomMobBuilder defending(LivingEntity target, DefendMode mode) {
        this.defendTarget = target;
        this.defendMode = mode;
        return this;
    }

    public CustomMobBuilder defending(LivingEntity target, double radius, DefendMode mode) {
        this.defendTarget = target;
        this.defendRadius = radius;
        this.defendMode = mode;
        return this;
    }

    // ==================== ABILITIES ====================

    public CustomMobBuilder withAbility(MobAbility ability) {
        this.abilities.add(ability);
        return this;
    }

    public CustomMobBuilder withAbility(String abilityId) {
        MobAbility ability = api.getAbilityManager().createAbilityInstance(abilityId);
        if (ability != null) {
            this.abilities.add(ability);
        }
        return this;
    }

    // ==================== EQUIPMENT SYSTEM ====================

    /**
     * Setzt ein Equipment-Item
     * @param slot Equipment-Slot
     * @param item Das Item
     * @param dropChance Drop-Chance (0.0 - 1.0)
     */
    public CustomMobBuilder withEquipment(MobEquipmentSlot slot, ItemStack item, float dropChance) {
        equipment.put(slot, new EquipmentEntry(item, dropChance));
        return this;
    }

    /**
     * Setzt Equipment mit Standard-Drop-Chance (0%)
     */
    public CustomMobBuilder withEquipment(MobEquipmentSlot slot, ItemStack item) {
        return withEquipment(slot, item, 0.0f);
    }

    /**
     * Setzt Oraxen-Item als Equipment
     * @param slot Equipment-Slot
     * @param oraxenItemId Oraxen-Item ID
     * @param dropChance Drop-Chance (0.0 - 1.0)
     */
    public CustomMobBuilder withOraxenEquipment(MobEquipmentSlot slot, String oraxenItemId, float dropChance) {
        try {
            io.th0rgal.oraxen.items.ItemBuilder builder =
                    io.th0rgal.oraxen.api.OraxenItems.getItemById(oraxenItemId);

            if (builder != null) {
                ItemStack item = builder.build();
                return withEquipment(slot, item, dropChance);
            } else {
                api.getPlugin().getPluginLogger().warn("Oraxen-Item nicht gefunden: " + oraxenItemId);
            }
        } catch (Exception e) {
            api.getPlugin().getPluginLogger().warn("Fehler beim Laden von Item '" +
                    oraxenItemId + "': " + e.getMessage());
        }
        return this;
    }

    /**
     * Setzt Oraxen-Item als Equipment mit Standard-Drop-Chance
     */
    public CustomMobBuilder withOraxenEquipment(MobEquipmentSlot slot, String oraxenItemId) {
        return withOraxenEquipment(slot, oraxenItemId, 0.0f);
    }

    /**
     * Wendet die Equipment-Config aus mob-equipment an
     */
    public CustomMobBuilder withEquipmentConfig() {
        this.useEquipmentConfig = true;
        return this;
    }

    // ==================== SPAWN ====================

    /**
     * Spawnt den Mob mit allen konfigurierten Eigenschaften
     */
    public CustomMob spawn() {
        if (location == null) {
            throw new IllegalStateException("Location muss gesetzt sein!");
        }

        CustomMob mob = api.spawnCustomMob(location, type);

        // Basis-Attribute
        mob.setLevel(level);

        if (name != null) {
            mob.setCustomName(name);
        }

        if (health != null) {
            mob.setHealth(health);
        }

        if (damage != null) {
            mob.setDamage(damage);
        }

        if (speed != null) {
            mob.setSpeed(speed);
        }

        if (scale != null) {
            mob.setScale(scale);
        }

        // Defend-System
        if (defendTarget != null) {
            DefendMode mode = defendMode != null ? defendMode : DefendMode.PASSIVE;
            if (defendRadius != null) {
                mob.setDefendTarget(defendTarget, defendRadius, mode);
            } else {
                mob.setDefendTarget(defendTarget, mode);
            }
        }

        // Abilities
        abilities.forEach(mob::addAbility);

        // Equipment
        if (!equipment.isEmpty() && mob.canWearEquipment()) {
            api.getPlugin().getPluginLogger().debug("Setze " + equipment.size() +
                    " Equipment-Items für " + type);

            for (Map.Entry<MobEquipmentSlot, EquipmentEntry> entry : equipment.entrySet()) {
                EquipmentEntry equipEntry = entry.getValue();
                mob.setEquipment(entry.getKey(), equipEntry.item, equipEntry.dropChance);
            }
        }

        // Equipment-Config anwenden (falls aktiviert)
        if (useEquipmentConfig) {
            mob.applyEquipmentConfig(api.getPlugin().getMobEquipmentManager());
        }

        return mob;
    }

    // ==================== HELPER ====================

    /**
         * Equipment-Entry für internen Gebrauch
         */
        private record EquipmentEntry(ItemStack item, float dropChance) {
    }
}