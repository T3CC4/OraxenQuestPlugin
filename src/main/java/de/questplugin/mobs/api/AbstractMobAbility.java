package de.questplugin.mobs.api;

/**
 * Abstrakte Basis-Klasse für Fähigkeiten
 */
public abstract class AbstractMobAbility implements MobAbility {

    private final String name;
    private final long cooldown;
    private long lastUsed;

    public AbstractMobAbility(String name, long cooldownTicks) {
        this.name = name;
        this.cooldown = cooldownTicks;
        this.lastUsed = 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public boolean isReady() {
        long currentTime = System.currentTimeMillis();
        long cooldownMs = (cooldown * 50); // Ticks zu Millisekunden
        return (currentTime - lastUsed) >= cooldownMs;
    }

    @Override
    public void setLastUsed(long time) {
        this.lastUsed = time;
    }

    @Override
    public long getLastUsed() {
        return lastUsed;
    }

    @Override
    public boolean execute(CustomMob mob) {
        if (!isReady() || !mob.isAlive()) {
            return false;
        }

        boolean success = performAbility(mob);
        if (success) {
            setLastUsed(System.currentTimeMillis());
        }
        return success;
    }

    /**
     * Implementierung der eigentlichen Fähigkeit
     */
    protected abstract boolean performAbility(CustomMob mob);
}