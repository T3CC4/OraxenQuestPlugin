package de.questplugin.mobs.abilities;

import de.questplugin.mobs.api.AbstractMobAbility;
import de.questplugin.mobs.api.CustomMob;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Erweiterte Mob-Abilities
 *
 * OFFENSIVE:
 * - Fireball, Lightning, Explosion, ArrowRain, Poison
 *
 * DEFENSIVE:
 * - Shield, Heal, Absorb, Thorns
 *
 * CROWD CONTROL:
 * - Freeze, Web, Blind, Weakness, Slowness
 *
 * SUMMON:
 * - SummonZombies, SummonSkeletons, SummonVex
 *
 * SPECIAL:
 * - Leap, Pull, Swap, Clone, Meteor
 */
public class AdvancedAbilities {

    // ==================== OFFENSIVE ====================

    /**
     * Schießt Fireball auf Spieler
     */
    public static class FireballAbility extends AbstractMobAbility {
        public FireballAbility() {
            super("Fireball", 200); // 10 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Player target = findNearestPlayer(entity, 20);
            if (target == null) return false;

            Fireball fireball = entity.getWorld().spawn(
                    entity.getEyeLocation(),
                    Fireball.class
            );

            Vector direction = target.getLocation()
                    .toVector()
                    .subtract(entity.getLocation().toVector())
                    .normalize();

            fireball.setDirection(direction);
            fireball.setYield(2.0f);
            fireball.setShooter(entity);

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);

            return true;
        }
    }

    /**
     * Ruft Blitz auf nahegelegene Spieler
     */
    public static class LightningAbility extends AbstractMobAbility {
        public LightningAbility() {
            super("Lightning", 300); // 15 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Collection<Player> nearby = entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 12, 12, 12)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());

            if (nearby.isEmpty()) return false;

            // Warnung
            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);

            // 1 Sekunde Delay
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("OraxenQuestPlugin"),
                    () -> {
                        nearby.forEach(player -> {
                            player.getWorld().strikeLightning(player.getLocation());
                        });
                    },
                    20L
            );

            return true;
        }
    }

    /**
     * Erzeugt Explosion
     */
    public static class ExplosionAbility extends AbstractMobAbility {
        public ExplosionAbility() {
            super("Explosion", 400); // 20 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            // Warnung
            entity.getWorld().spawnParticle(
                    Particle.FLAME,
                    entity.getLocation(),
                    50,
                    1, 1, 1,
                    0.1
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);

            // Explosion nach 2 Sekunden
            Bukkit.getScheduler().runTaskLater(
                    Bukkit.getPluginManager().getPlugin("OraxenQuestPlugin"),
                    () -> {
                        if (entity.isValid() && !entity.isDead()) {
                            entity.getWorld().createExplosion(
                                    entity.getLocation(),
                                    3.0f,
                                    false,
                                    false,
                                    entity
                            );
                        }
                    },
                    40L
            );

            return true;
        }
    }

    /**
     * Schießt Pfeil-Regen
     */
    public static class ArrowRainAbility extends AbstractMobAbility {
        public ArrowRainAbility() {
            super("ArrowRain", 250); // 12.5 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Player target = findNearestPlayer(entity, 20);
            if (target == null) return false;

            Location targetLoc = target.getLocation();

            // Schieße 10 Pfeile in die Luft
            for (int i = 0; i < 10; i++) {
                Bukkit.getScheduler().runTaskLater(
                        Bukkit.getPluginManager().getPlugin("OraxenQuestPlugin"),
                        () -> {
                            if (entity.isValid() && !entity.isDead()) {
                                Arrow arrow = entity.getWorld().spawn(
                                        targetLoc.clone().add(
                                                Math.random() * 6 - 3,
                                                15,
                                                Math.random() * 6 - 3
                                        ),
                                        Arrow.class
                                );
                                arrow.setVelocity(new Vector(0, -1, 0));
                                arrow.setShooter(entity);
                                arrow.setDamage(3.0);
                            }
                        },
                        i * 2L
                );
            }

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);

            return true;
        }
    }

    /**
     * Vergiftet Spieler in Reichweite
     */
    public static class PoisonAbility extends AbstractMobAbility {
        public PoisonAbility() {
            super("Poison", 200); // 10 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Collection<Player> nearby = entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 8, 8, 8)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());

            if (nearby.isEmpty()) return false;

            nearby.forEach(player -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.POISON,
                        100, // 5 Sekunden
                        1    // Level II
                ));
            });

            entity.getWorld().spawnParticle(
                    Particle.SMOKE,
                    entity.getLocation(),
                    100,
                    3, 1, 3,
                    0
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_WITCH_THROW, 1.0f, 0.7f);

            return true;
        }
    }

    // ==================== DEFENSIVE ====================

    /**
     * Erzeugt temporären Schild
     */
    public static class ShieldAbility extends AbstractMobAbility {
        public ShieldAbility() {
            super("Shield", 400); // 20 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE,
                    100, // 5 Sekunden
                    2    // Level III
            ));

            entity.getWorld().spawnParticle(
                    Particle.SHRIEK,
                    entity.getLocation(),
                    50,
                    1, 1, 1,
                    0
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 2.0f);

            return true;
        }
    }

    /**
     * Heilt sich selbst komplett
     */
    public static class HealAbility extends AbstractMobAbility {
        public HealAbility() {
            super("Heal", 600); // 30 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            double maxHealth = entity.getAttribute(
                    org.bukkit.attribute.Attribute.MAX_HEALTH
            ).getValue();

            entity.setHealth(maxHealth);

            entity.getWorld().spawnParticle(
                    Particle.HEART,
                    entity.getLocation().add(0, 1, 0),
                    20,
                    0.5, 0.5, 0.5,
                    0
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

            return true;
        }
    }

    /**
     * Gibt Absorption Hearts
     */
    public static class AbsorbAbility extends AbstractMobAbility {
        public AbsorbAbility() {
            super("Absorb", 300); // 15 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.ABSORPTION,
                    200, // 10 Sekunden
                    2    // Level III = 8 Extra-Hearts
            ));

            entity.getWorld().spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    entity.getLocation(),
                    30,
                    0.5, 0.5, 0.5,
                    0.1
            );

            return true;
        }
    }

    /**
     * Gibt Thorns-Effekt (Angreifer nehmen Schaden)
     */
    public static class ThornsAbility extends AbstractMobAbility {
        public ThornsAbility() {
            super("Thorns", 250); // 12.5 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            // Speichere in CustomData dass Thorns aktiv ist
            mob.setData("thorns_active", true);
            mob.setData("thorns_end", System.currentTimeMillis() + 5000);

            entity.getWorld().spawnParticle(
                    Particle.CRIT,
                    entity.getLocation(),
                    50,
                    1, 1, 1,
                    0
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.BLOCK_ANVIL_LAND, 0.5f, 2.0f);

            return true;
        }
    }

    // ==================== CROWD CONTROL ====================

    /**
     * Friert Spieler ein (Slowness + Jump Disable)
     */
    public static class FreezeAbility extends AbstractMobAbility {
        public FreezeAbility() {
            super("Freeze", 300); // 15 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Collection<Player> nearby = entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 10, 10, 10)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());

            if (nearby.isEmpty()) return false;

            nearby.forEach(player -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 60, 4
                ));
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.JUMP_BOOST, 60, 250 // Kann nicht springen
                ));

                player.getWorld().spawnParticle(
                        Particle.SNOWFLAKE,
                        player.getLocation(),
                        50,
                        1, 1, 1,
                        0
                );
            });

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

            return true;
        }
    }

    /**
     * Spawnt Cobwebs um Spieler
     */
    public static class WebAbility extends AbstractMobAbility {
        public WebAbility() {
            super("Web", 350); // 17.5 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Player target = findNearestPlayer(entity, 15);
            if (target == null) return false;

            Location loc = target.getLocation();

            // Platziere Cobwebs um Spieler
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location webLoc = loc.clone().add(x, 0, z);
                    if (webLoc.getBlock().getType() == Material.AIR) {
                        webLoc.getBlock().setType(Material.COBWEB);

                        // Entferne nach 5 Sekunden
                        Bukkit.getScheduler().runTaskLater(
                                Bukkit.getPluginManager().getPlugin("OraxenQuestPlugin"),
                                () -> {
                                    if (webLoc.getBlock().getType() == Material.COBWEB) {
                                        webLoc.getBlock().setType(Material.AIR);
                                    }
                                },
                                100L
                        );
                    }
                }
            }

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.5f);

            return true;
        }
    }

    /**
     * Blendet Spieler
     */
    public static class BlindAbility extends AbstractMobAbility {
        public BlindAbility() {
            super("Blind", 200); // 10 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Collection<Player> nearby = entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 8, 8, 8)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());

            if (nearby.isEmpty()) return false;

            nearby.forEach(player -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS, 60, 0
                ));
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS, 60, 0
                ));
            });

            entity.getWorld().spawnParticle(
                    Particle.SQUID_INK,
                    entity.getLocation(),
                    100,
                    2, 1, 2,
                    0.1
            );

            return true;
        }
    }

    /**
     * Gibt Spielern Weakness
     */
    public static class WeaknessAbility extends AbstractMobAbility {
        public WeaknessAbility() {
            super("Weakness", 250); // 12.5 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Collection<Player> nearby = entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 10, 10, 10)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());

            if (nearby.isEmpty()) return false;

            nearby.forEach(player -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS, 100, 2
                ));
            });

            entity.getWorld().spawnParticle(
                    Particle.WITCH,
                    entity.getLocation(),
                    50,
                    2, 1, 2,
                    0
            );

            return true;
        }
    }

    /**
     * Verlangsamt Spieler massiv
     */
    public static class SlownessAbility extends AbstractMobAbility {
        public SlownessAbility() {
            super("Slowness", 200); // 10 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Collection<Player> nearby = entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 12, 12, 12)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());

            if (nearby.isEmpty()) return false;

            nearby.forEach(player -> {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 80, 3
                ));
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.MINING_FATIGUE, 80, 2
                ));
            });

            entity.getWorld().spawnParticle(
                    Particle.FALLING_DUST,
                    entity.getLocation(),
                    100,
                    3, 1, 3,
                    0,
                    Material.SOUL_SAND.createBlockData()
            );

            return true;
        }
    }

    // ==================== SUMMON ====================

    /**
     * Beschwört Zombies
     */
    public static class SummonZombiesAbility extends AbstractMobAbility {
        public SummonZombiesAbility() {
            super("SummonZombies", 600); // 30 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();
            Location loc = entity.getLocation();

            for (int i = 0; i < 3; i++) {
                Location spawnLoc = loc.clone().add(
                        Math.random() * 4 - 2,
                        0,
                        Math.random() * 4 - 2
                );

                Zombie minion = (Zombie) loc.getWorld()
                        .spawnEntity(spawnLoc, EntityType.ZOMBIE);
                minion.setCustomName("§7Beschwörung");
                minion.setCustomNameVisible(true);
                minion.setBaby(true);

                // Kopiere Target vom Boss
                if (entity instanceof Mob && ((Mob) entity).getTarget() != null) {
                    minion.setTarget(((Mob) entity).getTarget());
                }
            }

            entity.getWorld().spawnParticle(
                    Particle.PORTAL,
                    entity.getLocation(),
                    100,
                    2, 1, 2,
                    1
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.5f);

            return true;
        }
    }

    /**
     * Beschwört Skelette
     */
    public static class SummonSkeletonsAbility extends AbstractMobAbility {
        public SummonSkeletonsAbility() {
            super("SummonSkeletons", 600); // 30 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();
            Location loc = entity.getLocation();

            for (int i = 0; i < 2; i++) {
                Location spawnLoc = loc.clone().add(
                        Math.random() * 4 - 2,
                        0,
                        Math.random() * 4 - 2
                );

                Skeleton minion = (Skeleton) loc.getWorld()
                        .spawnEntity(spawnLoc, EntityType.SKELETON);
                minion.setCustomName("§7Skelett-Diener");
                minion.setCustomNameVisible(true);

                if (entity instanceof Mob && ((Mob) entity).getTarget() != null) {
                    minion.setTarget(((Mob) entity).getTarget());
                }
            }

            entity.getWorld().spawnParticle(
                    Particle.LARGE_SMOKE,
                    entity.getLocation(),
                    50,
                    2, 1, 2,
                    0.1
            );

            return true;
        }
    }

    /**
     * Beschwört Vexe (kleine fliegende Mobs)
     */
    public static class SummonVexAbility extends AbstractMobAbility {
        public SummonVexAbility() {
            super("SummonVex", 500); // 25 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();
            Location loc = entity.getLocation();

            for (int i = 0; i < 4; i++) {
                Location spawnLoc = loc.clone().add(
                        Math.random() * 3 - 1.5,
                        2,
                        Math.random() * 3 - 1.5
                );

                Vex minion = (Vex) loc.getWorld()
                        .spawnEntity(spawnLoc, EntityType.VEX);
                minion.setCustomName("§7Vex");

                if (entity instanceof Mob && ((Mob) entity).getTarget() != null) {
                    minion.setTarget(((Mob) entity).getTarget());
                }
            }

            entity.getWorld().spawnParticle(
                    Particle.WITCH,
                    entity.getLocation(),
                    100,
                    2, 2, 2,
                    0.5
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);

            return true;
        }
    }

    // ==================== SPECIAL ====================

    /**
     * Springt zu Spieler
     */
    public static class LeapAbility extends AbstractMobAbility {
        public LeapAbility() {
            super("Leap", 150); // 7.5 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Player target = findNearestPlayer(entity, 15);
            if (target == null) return false;

            Vector direction = target.getLocation()
                    .toVector()
                    .subtract(entity.getLocation().toVector())
                    .normalize()
                    .multiply(1.5)
                    .setY(0.8);

            entity.setVelocity(direction);

            entity.getWorld().spawnParticle(
                    Particle.CLOUD,
                    entity.getLocation(),
                    20,
                    0.5, 0.5, 0.5,
                    0.1
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

            return true;
        }
    }

    /**
     * Zieht Spieler zum Mob
     */
    public static class PullAbility extends AbstractMobAbility {
        public PullAbility() {
            super("Pull", 250); // 12.5 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Collection<Player> nearby = entity.getWorld()
                    .getNearbyEntities(entity.getLocation(), 15, 15, 15)
                    .stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .collect(Collectors.toList());

            if (nearby.isEmpty()) return false;

            nearby.forEach(player -> {
                Vector direction = entity.getLocation()
                        .toVector()
                        .subtract(player.getLocation().toVector())
                        .normalize()
                        .multiply(2)
                        .setY(0.5);

                player.setVelocity(direction);
            });

            entity.getWorld().spawnParticle(
                    Particle.SWEEP_ATTACK,
                    entity.getLocation(),
                    50,
                    3, 1, 3,
                    0
            );

            entity.getWorld().playSound(entity.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

            return true;
        }
    }

    /**
     * Tauscht Position mit zufälligem Spieler
     */
    public static class SwapAbility extends AbstractMobAbility {
        public SwapAbility() {
            super("Swap", 400); // 20 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Player target = findNearestPlayer(entity, 20);
            if (target == null) return false;

            Location mobLoc = entity.getLocation().clone();
            Location playerLoc = target.getLocation().clone();

            // Particle Effekte
            entity.getWorld().spawnParticle(
                    Particle.PORTAL,
                    mobLoc,
                    50,
                    0.5, 0.5, 0.5,
                    1
            );
            entity.getWorld().spawnParticle(
                    Particle.PORTAL,
                    playerLoc,
                    50,
                    0.5, 0.5, 0.5,
                    1
            );

            // Tausche Positionen
            entity.teleport(playerLoc);
            target.teleport(mobLoc);

            entity.getWorld().playSound(mobLoc,
                    Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
            entity.getWorld().playSound(playerLoc,
                    Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);

            return true;
        }
    }

    /**
     * Erstellt einen Klon von sich selbst
     */
    public static class CloneAbility extends AbstractMobAbility {
        public CloneAbility() {
            super("Clone", 800); // 40 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Location spawnLoc = entity.getLocation().clone().add(2, 0, 0);

            LivingEntity clone = (LivingEntity) entity.getWorld()
                    .spawnEntity(spawnLoc, entity.getType());

            clone.setCustomName(entity.getCustomName() + " §7(Klon)");
            clone.setCustomNameVisible(true);

            // Kopiere Attribute
            if (clone.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null) {
                double health = entity.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                ).getValue() * 0.5; // Hälfte der HP

                clone.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH
                ).setBaseValue(health);
                clone.setHealth(health);
            }

            // Kopiere Target
            if (entity instanceof Mob && ((Mob) entity).getTarget() != null) {
                ((Mob) clone).setTarget(((Mob) entity).getTarget());
            }

            entity.getWorld().spawnParticle(
                    Particle.EXPLOSION,
                    spawnLoc,
                    1,
                    0, 0, 0,
                    0
            );

            entity.getWorld().playSound(spawnLoc,
                    Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);

            return true;
        }
    }

    /**
     * Lässt Meteor auf Spieler fallen
     */
    public static class MeteorAbility extends AbstractMobAbility {
        public MeteorAbility() {
            super("Meteor", 600); // 30 Sekunden
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            Player target = findNearestPlayer(entity, 25);
            if (target == null) return false;

            Location targetLoc = target.getLocation();

            // Spawne Fireball hoch oben
            Location meteorLoc = targetLoc.clone().add(0, 30, 0);

            Fireball meteor = entity.getWorld().spawn(
                    meteorLoc,
                    Fireball.class
            );

            meteor.setDirection(new Vector(0, -1, 0));
            meteor.setYield(4.0f); // Große Explosion
            meteor.setShooter(entity);

            // Warnung
            entity.getWorld().spawnParticle(
                    Particle.FLAME,
                    targetLoc,
                    100,
                    2, 0, 2,
                    0.1
            );

            entity.getWorld().playSound(targetLoc,
                    Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);

            return true;
        }
    }

    // ==================== HELPER ====================

    private static Player findNearestPlayer(LivingEntity entity, double range) {
        return entity.getWorld()
                .getNearbyEntities(entity.getLocation(), range, range, range)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .min((p1, p2) -> Double.compare(
                        p1.getLocation().distance(entity.getLocation()),
                        p2.getLocation().distance(entity.getLocation())
                ))
                .orElse(null);
    }

    /**
     * Findet die nächste LivingEntity eines bestimmten Typs
     * @param source Quell-Entity
     * @param range Suchradius
     * @param entityClass Klasse der zu suchenden Entity (z.B. Player.class, Zombie.class)
     * @return Nächste Entity oder null
     */
    public static LivingEntity getNearestEntity(LivingEntity source, double range, Class<? extends LivingEntity> entityClass) {
        return source.getWorld()
                .getNearbyEntities(source.getLocation(), range, range, range)
                .stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(e -> entityClass.isInstance(e))
                .filter(e -> !e.equals(source))
                .min(Comparator.comparingDouble(e -> e.getLocation().distance(source.getLocation())))
                .orElse(null);
    }

    /**
     * Findet die nächste LivingEntity (beliebiger Typ)
     * @param source Quell-Entity
     * @param range Suchradius
     * @return Nächste Entity oder null
     */
    public static LivingEntity getNearestEntity(LivingEntity source, double range) {
        return getNearestEntity(source, range, LivingEntity.class);
    }
}