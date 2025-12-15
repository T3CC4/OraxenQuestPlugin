package de.questplugin.mobs.api;

/**
 * Interface für Mob-Fähigkeiten
 */
public interface MobAbility {

    /**
     * Name der Fähigkeit
     */
    String getName();

    /**
     * Cooldown in Ticks (20 ticks = 1 Sekunde)
     */
    long getCooldown();

    /**
     * Wird aufgerufen wenn die Fähigkeit dem Mob hinzugefügt wird
     */
    default void onApply(CustomMob mob) {}

    /**
     * Wird aufgerufen wenn die Fähigkeit entfernt wird
     */
    default void onRemove(CustomMob mob) {}

    /**
     * Führt die Fähigkeit aus
     * @return true wenn erfolgreich ausgeführt
     */
    boolean execute(CustomMob mob);

    /**
     * Prüft ob die Fähigkeit bereit ist
     */
    boolean isReady();

    /**
     * Setzt den letzten Ausführungszeitpunkt
     */
    void setLastUsed(long time);

    /**
     * Holt den letzten Ausführungszeitpunkt
     */
    long getLastUsed();
}