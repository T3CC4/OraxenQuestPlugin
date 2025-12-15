package de.questplugin.mobs.api;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Beispiel-Fähigkeiten
 */
public class ExampleAbilities {

    /**
     * Teleportiert den Mob zu einem zufälligen nahegelegenen Spieler
     */
    public static class TeleportAbility extends AbstractMobAbility {

        public TeleportAbility() {
            super("Teleport", 200); // 10 Sekunden Cooldown
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            // Finde nahe Spieler
            Collection<Player> nearbyPlayers = entity.getWorld().getNearbyEntities(
                            entity.getLocation(), 20, 20, 20
                    ).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .toList();

            if (nearbyPlayers.isEmpty()) return false;

            Player target = nearbyPlayers.stream()
                    .skip((int) (Math.random() * nearbyPlayers.size()))
                    .findFirst()
                    .orElse(null);

            if (target != null) {
                Location targetLoc = target.getLocation();
                entity.teleport(targetLoc.add(
                        (Math.random() - 0.5) * 5,
                        0,
                        (Math.random() - 0.5) * 5
                ));
                return true;
            }
            return false;
        }
    }

    /**
     * Gibt dem Mob Regeneration
     */
    public static class RegenerationAbility extends AbstractMobAbility {

        public RegenerationAbility() {
            super("Regeneration", 300); // 15 Sekunden Cooldown
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    100, // 5 Sekunden
                    1   // Level II
            ));
            return true;
        }
    }

    /**
     * Gibt dem Mob kurzzeitig Geschwindigkeit
     */
    public static class SpeedBoostAbility extends AbstractMobAbility {

        public SpeedBoostAbility() {
            super("SpeedBoost", 400); // 20 Sekunden Cooldown
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    60, // 3 Sekunden
                    2   // Level III
            ));
            return true;
        }
    }

    /**
     * Schleudert nahe Spieler weg
     */
    public static class KnockbackAbility extends AbstractMobAbility {

        public KnockbackAbility() {
            super("Knockback", 160); // 8 Sekunden Cooldown
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();

            // Finde nahe Spieler
            Collection<Player> nearbyPlayers = entity.getWorld().getNearbyEntities(
                            entity.getLocation(), 5, 5, 5
                    ).stream()
                    .filter(e -> e instanceof Player)
                    .map(e -> (Player) e)
                    .toList();

            if (nearbyPlayers.isEmpty()) return false;

            nearbyPlayers.forEach(player -> {
                Vector direction = player.getLocation().toVector()
                        .subtract(entity.getLocation().toVector())
                        .normalize()
                        .multiply(2)
                        .setY(0.5);
                player.setVelocity(direction);
            });
            return true;
        }
    }

    /**
     * Macht den Mob kurzzeitig unsichtbar
     */
    public static class InvisibilityAbility extends AbstractMobAbility {

        public InvisibilityAbility() {
            super("Invisibility", 600); // 30 Sekunden Cooldown
        }

        @Override
        protected boolean performAbility(CustomMob mob) {
            LivingEntity entity = mob.getEntity();
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    100, // 5 Sekunden
                    0
            ));
            return true;
        }
    }
}
