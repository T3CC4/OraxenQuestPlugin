package de.questplugin.mobs.api;

/**
 * Verschiedene Modi wie ein Mob sein Target verteidigt
 */
public enum DefendMode {

    /**
     * Wie Hunde: Greift nur an wenn Target angegriffen wird
     */
    PASSIVE,

    /**
     * Greift alle feindlichen Entities im Radius an
     */
    AGGRESSIVE,

    /**
     * Folgt nur dem Target, greift nicht an
     */
    FOLLOW_ONLY,

    /**
     * Steht an Position, greift nur im Radius an
     */
    GUARD_POSITION
}

