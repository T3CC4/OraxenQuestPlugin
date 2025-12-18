package de.questplugin.mobs.api;

import de.questplugin.enums.MobEquipmentSlot;
import de.questplugin.utils.EquipmentHelper;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

import java.util.*;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;

/**
 * Repräsentiert einen Custom Mob mit Equipment-Support
 *
 * OPTIMIERT: Nutzt EquipmentHelper statt duplizierte Equipment-Logik
 */
public class CustomMob {

    private final LivingEntity entity;
    private final CustomMobAPI api;
    private final List<MobAbility> abilities;
    private final Map<String, Object> customData;
    private int level;
    private String customName;
    private LivingEntity defendTarget;
    private double defendRadius;
    private DefendMode defendMode;
    private org.bukkit.Location guardPosition;

    public CustomMob(LivingEntity entity, CustomMobAPI api) {
        this.entity = entity;
        this.api = api;
        this.abilities = new ArrayList<>();
        this.customData = new HashMap<>();
        this.level = 1;
        this.defendTarget = null;
        this.defendRadius = 15.0;
        this.defendMode = DefendMode.PASSIVE;
        this.guardPosition = null;

        NamespacedKey key = new NamespacedKey(api.getPlugin().getName().toLowerCase(), "custom_mob");
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    // ==================== ATTRIBUTE ====================

    public CustomMob setLevel(int level) {
        this.level = level;
        updateAttributes();
        return this;
    }

    public int getLevel() {
        return level;
    }

    public CustomMob setCustomName(String name) {
        this.customName = name;
        entity.setCustomName(name);
        entity.setCustomNameVisible(true);
        return this;
    }

    public String getCustomName() {
        return customName;
    }

    public CustomMob setHealth(double health) {
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        entity.setHealth(health);
        return this;
    }

    public CustomMob setDamage(double damage) {
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
        }
        return this;
    }

    public CustomMob setSpeed(double speed) {
        entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
        return this;
    }

    public CustomMob setScale(double scale) {
        if (entity.getAttribute(Attribute.SCALE) != null) {
            entity.getAttribute(Attribute.SCALE).setBaseValue(scale);
        }
        return this;
    }

    public double getScale() {
        if (entity.getAttribute(Attribute.SCALE) != null) {
            return entity.getAttribute(Attribute.SCALE).getValue();
        }
        return 1.0;
    }

    // ==================== EQUIPMENT SYSTEM (OPTIMIERT) ====================

    /**
     * Gibt das Equipment des Mobs zurück (falls vorhanden)
     */
    public org.bukkit.inventory.EntityEquipment getEquipment() {
        if (entity instanceof Mob) {
            return entity.getEquipment();
        }
        return null;
    }

    /**
     * Prüft ob dieser Mob Equipment tragen kann
     *
     * OPTIMIERT: Nutzt EquipmentHelper
     */
    public boolean canWearEquipment() {
        return EquipmentHelper.canWearEquipment(entity);
    }

    /**
     * Setzt Equipment in einem Slot
     *
     * OPTIMIERT: Nutzt EquipmentHelper für Slot-Setting
     *
     * @param slot Equipment-Slot
     * @param item Das Item
     * @param dropChance Drop-Chance (0.0 - 1.0)
     * @return this für Chaining
     */
    public CustomMob setEquipment(MobEquipmentSlot slot,
                                  org.bukkit.inventory.ItemStack item,
                                  float dropChance) {
        if (!canWearEquipment()) {
            api.getPlugin().getPluginLogger().debug("Mob " + entity.getType() +
                    " kann kein Equipment tragen!");
            return this;
        }

        org.bukkit.inventory.EntityEquipment equipment = getEquipment();
        if (equipment == null) return this;

        // OPTIMIERT: Nutze EquipmentHelper statt Switch-Case
        EquipmentHelper.setEquipmentSlot(equipment, slot, item);
        EquipmentHelper.setDropChance(equipment, slot, dropChance);

        api.getPlugin().getPluginLogger().debug("Equipment gesetzt: " + slot +
                " → " + item.getType());
        return this;
    }

    /**
     * Setzt Equipment mit Standard-Drop-Chance (0%)
     */
    public CustomMob setEquipment(MobEquipmentSlot slot,
                                  org.bukkit.inventory.ItemStack item) {
        return setEquipment(slot, item, 0.0f);
    }

    /**
     * Wendet Equipment-Config auf den Mob an
     *
     * OPTIMIERT: Die gesamte Logik ist jetzt in EquipmentHelper!
     *
     * @param equipmentManager Der Equipment-Manager
     * @return this für Chaining
     */
    public CustomMob applyEquipmentConfig(de.questplugin.managers.MobEquipmentManager equipmentManager) {
        if (!canWearEquipment()) {
            return this;
        }

        // OPTIMIERT: Gesamte Equipment-Logik ist jetzt in EquipmentHelper
        // Keine Duplikation mehr mit RaidInstance!
        EquipmentHelper.applyEquipmentConfig(entity, equipmentManager, api.getPlugin());

        return this;
    }

    /**
     * Kopiert Equipment von diesem Mob zu einem anderen
     *
     * OPTIMIERT: Nutzt EquipmentHelper
     *
     * @param target Ziel-Mob
     * @return true wenn erfolgreich
     */
    public boolean copyEquipmentTo(LivingEntity target) {
        return EquipmentHelper.copyEquipment(entity, target);
    }

    /**
     * Entfernt alles Equipment
     *
     * OPTIMIERT: Nutzt EquipmentHelper
     *
     * @return this für Chaining
     */
    public CustomMob clearEquipment() {
        EquipmentHelper.clearEquipment(entity);
        return this;
    }

    /**
     * Debug: Gibt Equipment-Info aus
     *
     * OPTIMIERT: Nutzt EquipmentHelper
     */
    public String getEquipmentInfo() {
        return EquipmentHelper.getEquipmentInfo(entity);
    }

    // ==================== DEFEND SYSTEM ====================

    public CustomMob setDefendTarget(LivingEntity target, double radius, DefendMode mode) {
        this.defendTarget = target;
        this.defendRadius = radius;
        this.defendMode = mode;

        if (mode == DefendMode.GUARD_POSITION && target != null) {
            this.guardPosition = target.getLocation().clone();
        }

        if (target != null && entity instanceof Mob mob) {
            if (target instanceof org.bukkit.entity.Player player) {
                if (mob instanceof Tameable tameable) {
                    tameable.setOwner(player);
                    tameable.setTamed(true);
                }
            }
        }
        return this;
    }

    public CustomMob setDefendTarget(LivingEntity target, DefendMode mode) {
        return setDefendTarget(target, 15.0, mode);
    }

    public CustomMob setDefendTarget(LivingEntity target, double radius) {
        return setDefendTarget(target, radius, DefendMode.PASSIVE);
    }

    public CustomMob setDefendTarget(LivingEntity target) {
        return setDefendTarget(target, 15.0, DefendMode.PASSIVE);
    }

    public CustomMob setDefendMode(DefendMode mode) {
        this.defendMode = mode;
        if (mode == DefendMode.GUARD_POSITION && defendTarget != null) {
            this.guardPosition = defendTarget.getLocation().clone();
        }
        return this;
    }

    public DefendMode getDefendMode() {
        return defendMode;
    }

    public CustomMob setGuardPosition(org.bukkit.Location position) {
        this.guardPosition = position.clone();
        return this;
    }

    public org.bukkit.Location getGuardPosition() {
        return guardPosition != null ? guardPosition.clone() : null;
    }

    public CustomMob removeDefendTarget() {
        this.defendTarget = null;
        if (entity instanceof Tameable tameable) {
            tameable.setTamed(false);
            tameable.setOwner(null);
        }
        return this;
    }

    public LivingEntity getDefendTarget() {
        return defendTarget;
    }

    public double getDefendRadius() {
        return defendRadius;
    }

    public boolean hasValidDefendTarget() {
        return defendTarget != null && defendTarget.isValid() && !defendTarget.isDead();
    }

    public void attackDefender(LivingEntity attacker) {
        if (entity instanceof Mob mob && attacker != null) {
            mob.setTarget(attacker);
        }
    }

    // ==================== ABILITIES ====================

    public CustomMob addAbility(MobAbility ability) {
        abilities.add(ability);
        ability.onApply(this);
        return this;
    }

    public CustomMob removeAbility(MobAbility ability) {
        if (abilities.remove(ability)) {
            ability.onRemove(this);
        }
        return this;
    }

    public List<MobAbility> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    public void triggerAbilities() {
        abilities.forEach(ability -> ability.execute(this));
    }

    // ==================== DATA ====================

    public CustomMob setData(String key, Object value) {
        customData.put(key, value);
        return this;
    }

    public Object getData(String key) {
        return customData.get(key);
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public boolean isAlive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    public void kill() {
        if (isAlive()) {
            entity.setHealth(0);
        }
    }

    public void remove() {
        cleanup();
        if (entity != null) {
            entity.remove();
        }
    }

    // ==================== INTERNAL ====================

    private void updateAttributes() {
        double healthMultiplier = 1 + (level - 1) * 0.5;
        double damageMultiplier = 1 + (level - 1) * 0.3;

        double baseHealth = entity.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        setHealth(baseHealth * healthMultiplier);

        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            double baseDamage = entity.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue();
            setDamage(baseDamage * damageMultiplier);
        }
    }

    public void cleanup() {
        abilities.forEach(ability -> ability.onRemove(this));
        abilities.clear();
        customData.clear();
        defendTarget = null;
        guardPosition = null;
    }
}