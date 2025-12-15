package de.questplugin.mobs.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitTask;

/**
 * Verwaltet das Defend-Verhalten von Custom Mobs
 */
public class DefendBehaviorManager implements Listener {

    private final CustomMobAPI api;
    private BukkitTask defendTask;

    public DefendBehaviorManager(CustomMobAPI api) {
        this.api = api;
        Bukkit.getPluginManager().registerEvents(this, api.getPlugin());
        startDefendChecker();
    }

    /**
     * Startet Task der prüft ob Mobs ihr Target verteidigen müssen
     */
    private void startDefendChecker() {
        defendTask = Bukkit.getScheduler().runTaskTimer(api.getPlugin(), () -> {
            api.getActiveMobs().forEach(this::checkDefendBehavior);
        }, 20L, 10L); // Alle 0.5 Sekunden
    }

    /**
     * Prüft ob ein Mob sein Target verteidigen muss
     */
    private void checkDefendBehavior(CustomMob mob) {
        if (!mob.isAlive() || !mob.hasValidDefendTarget()) return;

        LivingEntity defendTarget = mob.getDefendTarget();
        LivingEntity entity = mob.getEntity();
        DefendMode mode = mob.getDefendMode();

        if (!(entity instanceof Mob mobEntity)) return;

        double distance = entity.getLocation().distance(defendTarget.getLocation());

        switch (mode) {
            case PASSIVE:
                // Wie Hunde: Folgt nur, greift nur an wenn Target attackiert wird
                // Bewegung passiert automatisch wenn Mob gezähmt ist
                // Angriff wird nur in onDefendTargetAttacked Event gehandhabt
                break;

            case AGGRESSIVE:
                // Greift alle feindlichen Entities im Radius an
                if (distance > mob.getDefendRadius()) {
                    // Zu weit weg - keine Aktion
                    return;
                }

                // Suche nach Angreifern
                defendTarget.getWorld().getNearbyEntities(
                                defendTarget.getLocation(),
                                mob.getDefendRadius(),
                                mob.getDefendRadius(),
                                mob.getDefendRadius()
                        ).stream()
                        .filter(e -> e instanceof LivingEntity)
                        .map(e -> (LivingEntity) e)
                        .filter(e -> e != entity && e != defendTarget)
                        .filter(e -> isHostile(e, defendTarget) || isHostileType(e))
                        .findFirst()
                        .ifPresent(attacker -> mobEntity.setTarget(attacker));
                break;

            case FOLLOW_ONLY:
                // Folgt nur dem Target, greift nie an
                // Entferne Target falls gesetzt
                if (mobEntity.getTarget() != null) {
                    mobEntity.setTarget(null);
                }
                break;

            case GUARD_POSITION:
                // Bleibt an Position, greift nur im Radius an
                org.bukkit.Location guardPos = mob.getGuardPosition();
                if (guardPos == null) {
                    guardPos = entity.getLocation();
                    mob.setGuardPosition(guardPos);
                }

                double distanceFromGuard = entity.getLocation().distance(guardPos);

                // Wenn zu weit von Guard-Position, gehe zurück
                if (distanceFromGuard > 5.0 && mobEntity.getTarget() == null) {
                    // Teleportiere zurück zur Position
                    entity.teleport(guardPos);
                    return;
                }

                // Greife Entities in Guard-Radius an
                guardPos.getWorld().getNearbyEntities(
                                guardPos,
                                mob.getDefendRadius(),
                                mob.getDefendRadius(),
                                mob.getDefendRadius()
                        ).stream()
                        .filter(e -> e instanceof LivingEntity)
                        .map(e -> (LivingEntity) e)
                        .filter(e -> e != entity && e != defendTarget)
                        .filter(e -> isHostile(e, defendTarget) || isHostileType(e))
                        .findFirst()
                        .ifPresent(attacker -> mobEntity.setTarget(attacker));
                break;
        }
    }

    /**
     * Prüft ob eine Entity feindselig gegenüber dem Target ist
     */
    private boolean isHostile(LivingEntity entity, LivingEntity target) {
        if (entity instanceof Mob mob) {
            return mob.getTarget() == target;
        }
        return false;
    }

    /**
     * Prüft ob eine Entity grundsätzlich feindselig ist
     */
    private boolean isHostileType(LivingEntity entity) {
        return entity instanceof org.bukkit.entity.Monster;
    }

    /**
     * Wenn das Defend-Target angegriffen wird
     */
    @EventHandler
    public void onDefendTargetAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        // Finde alle Mobs die dieses Target verteidigen
        api.getActiveMobs().stream()
                .filter(mob -> mob.getDefendTarget() == victim)
                .filter(CustomMob::isAlive)
                .filter(mob -> mob.getDefendMode() != DefendMode.FOLLOW_ONLY) // FOLLOW_ONLY greift nie an
                .forEach(mob -> mob.attackDefender(attacker));
    }

    /**
     * Wenn das Defend-Target stirbt
     */
    @EventHandler
    public void onDefendTargetDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();

        // Entferne Target von allen Mobs die es verteidigt haben
        api.getActiveMobs().stream()
                .filter(mob -> mob.getDefendTarget() == deadEntity)
                .forEach(CustomMob::removeDefendTarget);
    }

    /**
     * Verhindert dass Mobs ihr Defend-Target angreifen
     */
    @EventHandler
    public void onTargetDefendTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof LivingEntity attacker)) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        CustomMob customMob = api.getCustomMob(attacker);
        if (customMob == null) return;

        // Verhindere Angriff auf eigenes Defend-Target
        if (customMob.getDefendTarget() == target) {
            event.setCancelled(true);
        }
    }

    /**
     * Stoppt den Defend-Checker
     */
    public void shutdown() {
        if (defendTask != null) {
            defendTask.cancel();
        }
    }
}