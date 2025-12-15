package de.questplugin.mobs.api;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Verwaltet alle Mob-Fähigkeiten
 */
public class MobAbilityManager {

    private final CustomMobAPI api;
    private final Map<String, MobAbility> registeredAbilities;
    private BukkitTask abilityTask;

    public MobAbilityManager(CustomMobAPI api) {
        this.api = api;
        this.registeredAbilities = new HashMap<>();
        startAbilityTicker();
    }

    /**
     * Registriert eine neue Fähigkeit
     */
    public void registerAbility(String id, MobAbility ability) {
        registeredAbilities.put(id, ability);
    }

    /**
     * Holt eine registrierte Fähigkeit
     */
    public MobAbility getAbility(String id) {
        return registeredAbilities.get(id);
    }

    /**
     * Erstellt eine neue Instanz einer registrierten Fähigkeit
     */
    public MobAbility createAbilityInstance(String id) {
        MobAbility template = registeredAbilities.get(id);
        if (template == null) return null;

        // Erstelle neue Instanz (muss von Ability-Klasse implementiert werden)
        try {
            return template.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gibt alle registrierten Ability-IDs zurück
     */
    public Set<String> getRegisteredAbilities() {
        return registeredAbilities.keySet();
    }

    /**
     * Prüft ob eine Ability registriert ist
     */
    public boolean isRegistered(String id) {
        return registeredAbilities.containsKey(id);
    }

    /**
     * Startet den Ability-Ticker der alle Mobs prüft
     */
    private void startAbilityTicker() {
        abilityTask = Bukkit.getScheduler().runTaskTimer(api.getPlugin(), () -> {
            api.getActiveMobs().forEach(mob -> {
                if (mob.isAlive() && !mob.getAbilities().isEmpty()) {
                    // Zufällige Chance für Fähigkeitsausführung
                    if (Math.random() < 0.1) { // 10% Chance pro Tick
                        mob.getAbilities().stream()
                                .filter(MobAbility::isReady)
                                .findFirst()
                                .ifPresent(ability -> ability.execute(mob));
                    }
                }
            });
        }, 20L, 20L); // Alle 1 Sekunde
    }

    /**
     * Stoppt den Ability-Ticker
     */
    public void shutdown() {
        if (abilityTask != null) {
            abilityTask.cancel();
        }
    }
}