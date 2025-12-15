package de.questplugin.mobs.api;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;

import java.awt.*;
import java.util.*;
import java.util.List;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;

/**
 * Repräsentiert einen Custom Mob
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

        // Markiere Entity als Custom Mob
        // FIX: Nutze plugin.getName() statt String.valueOf(plugin)
        // getName() gibt nur den Plugin-Namen zurück (ohne Version/Spaces)
        NamespacedKey key = new NamespacedKey(api.getPlugin().getName().toLowerCase(), "custom_mob");
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * Setzt das Level des Mobs
     */
    public CustomMob setLevel(int level) {
        this.level = level;
        updateAttributes();
        return this;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Setzt einen Custom Namen
     */
    public CustomMob setCustomName(String name) {
        this.customName = name;
        entity.setCustomName(name);
        entity.setCustomNameVisible(true);
        return this;
    }

    public String getCustomName() {
        return customName;
    }

    /**
     * Setzt die Gesundheit
     */
    public CustomMob setHealth(double health) {
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        entity.setHealth(health);
        return this;
    }

    /**
     * Setzt den Schaden
     */
    public CustomMob setDamage(double damage) {
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
        }
        return this;
    }

    /**
     * Setzt die Geschwindigkeit
     */
    public CustomMob setSpeed(double speed) {
        entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
        return this;
    }

    /**
     * Setzt die Größe des Mobs (Scale)
     * @param scale Skalierungsfaktor (1.0 = normal, 2.0 = doppelt so groß, 0.5 = halb so groß)
     */
    public CustomMob setScale(double scale) {
        if (entity.getAttribute(Attribute.SCALE) != null) {
            entity.getAttribute(Attribute.SCALE).setBaseValue(scale);
        }
        return this;
    }

    /**
     * Holt die aktuelle Größe
     */
    public double getScale() {
        if (entity.getAttribute(Attribute.SCALE) != null) {
            return entity.getAttribute(Attribute.SCALE).getValue();
        }
        return 1.0;
    }

    /**
     * Setzt ein Entity zum Verteidigen (Spieler oder Mob)
     * @param target Das zu verteidigende Entity
     * @param radius Radius in dem verteidigt wird
     * @param mode Defend-Modus (PASSIVE, AGGRESSIVE, etc.)
     */
    public CustomMob setDefendTarget(LivingEntity target, double radius, DefendMode mode) {
        this.defendTarget = target;
        this.defendRadius = radius;
        this.defendMode = mode;

        if (mode == DefendMode.GUARD_POSITION && target != null) {
            this.guardPosition = target.getLocation().clone();
        }

        if (target != null && entity instanceof Mob mob) {
            // Setze Mob als "Tame" wenn Target ein Spieler ist
            if (target instanceof org.bukkit.entity.Player player) {
                if (mob instanceof Tameable tameable) {
                    tameable.setOwner(player);
                    tameable.setTamed(true);
                }
            }
        }
        return this;
    }

    /**
     * Setzt Defend-Target mit Standard-Radius und Modus
     */
    public CustomMob setDefendTarget(LivingEntity target, DefendMode mode) {
        return setDefendTarget(target, 15.0, mode);
    }

    /**
     * Setzt Defend-Target mit Standard-Radius und PASSIVE Modus (wie Hunde)
     */
    public CustomMob setDefendTarget(LivingEntity target, double radius) {
        return setDefendTarget(target, radius, DefendMode.PASSIVE);
    }

    /**
     * Setzt ein Entity zum Verteidigen mit Standard-Radius und PASSIVE Modus
     */
    public CustomMob setDefendTarget(LivingEntity target) {
        return setDefendTarget(target, 15.0, DefendMode.PASSIVE);
    }

    /**
     * Setzt den Defend-Modus
     */
    public CustomMob setDefendMode(DefendMode mode) {
        this.defendMode = mode;
        if (mode == DefendMode.GUARD_POSITION && defendTarget != null) {
            this.guardPosition = defendTarget.getLocation().clone();
        }
        return this;
    }

    /**
     * Holt den aktuellen Defend-Modus
     */
    public DefendMode getDefendMode() {
        return defendMode;
    }

    /**
     * Setzt eine Guard-Position (nur für GUARD_POSITION Modus)
     */
    public CustomMob setGuardPosition(org.bukkit.Location position) {
        this.guardPosition = position.clone();
        return this;
    }

    /**
     * Holt die Guard-Position
     */
    public org.bukkit.Location getGuardPosition() {
        return guardPosition != null ? guardPosition.clone() : null;
    }

    /**
     * Entfernt das Defend-Target
     */
    public CustomMob removeDefendTarget() {
        this.defendTarget = null;
        if (entity instanceof Tameable tameable) {
            tameable.setTamed(false);
            tameable.setOwner(null);
        }
        return this;
    }

    /**
     * Holt das aktuelle Defend-Target
     */
    public LivingEntity getDefendTarget() {
        return defendTarget;
    }

    /**
     * Holt den Defend-Radius
     */
    public double getDefendRadius() {
        return defendRadius;
    }

    /**
     * Prüft ob das Defend-Target noch gültig ist
     */
    public boolean hasValidDefendTarget() {
        return defendTarget != null && defendTarget.isValid() && !defendTarget.isDead();
    }

    /**
     * Greift Angreifer vom Defend-Target an
     * @param attacker Der Angreifer
     */
    public void attackDefender(LivingEntity attacker) {
        if (entity instanceof Mob mob && attacker != null) {
            mob.setTarget(attacker);
        }
    }

    /**
     * Fügt eine Fähigkeit hinzu
     */
    public CustomMob addAbility(MobAbility ability) {
        abilities.add(ability);
        ability.onApply(this);
        return this;
    }

    /**
     * Entfernt eine Fähigkeit
     */
    public CustomMob removeAbility(MobAbility ability) {
        if (abilities.remove(ability)) {
            ability.onRemove(this);
        }
        return this;
    }

    /**
     * Holt alle Fähigkeiten
     */
    public List<MobAbility> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    /**
     * Aktiviert alle Fähigkeiten
     */
    public void triggerAbilities() {
        abilities.forEach(ability -> ability.execute(this));
    }

    /**
     * Speichert Custom Daten
     */
    public CustomMob setData(String key, Object value) {
        customData.put(key, value);
        return this;
    }

    /**
     * Holt Custom Daten
     */
    public Object getData(String key) {
        return customData.get(key);
    }

    /**
     * Holt die Entity
     */
    public LivingEntity getEntity() {
        return entity;
    }

    /**
     * Prüft ob der Mob noch lebt
     */
    public boolean isAlive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    /**
     * Tötet den Mob
     */
    public void kill() {
        if (isAlive()) {
            entity.setHealth(0);
        }
    }

    /**
     * Entfernt den Mob
     */
    public void remove() {
        cleanup();
        if (entity != null) {
            entity.remove();
        }
    }

    /**
     * Aktualisiert Attribute basierend auf Level
     */
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

    /**
     * Cleanup beim Entfernen
     */
    public void cleanup() {
        abilities.forEach(ability -> ability.onRemove(this));
        abilities.clear();
        customData.clear();
        defendTarget = null;
        guardPosition = null;
    }
}