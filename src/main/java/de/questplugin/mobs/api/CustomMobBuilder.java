package de.questplugin.mobs.api;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder-Pattern für Custom Mob Erstellung
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

    /**
     * Spawnt den Mob mit allen konfigurierten Eigenschaften
     */
    public CustomMob spawn() {
        if (location == null) {
            throw new IllegalStateException("Location muss gesetzt sein!");
        }

        CustomMob mob = api.spawnCustomMob(location, type);

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

        if (defendTarget != null) {
            DefendMode mode = defendMode != null ? defendMode : DefendMode.PASSIVE;
            if (defendRadius != null) {
                mob.setDefendTarget(defendTarget, defendRadius, mode);
            } else {
                mob.setDefendTarget(defendTarget, mode);
            }
        }

        abilities.forEach(mob::addAbility);

        return mob;
    }
}