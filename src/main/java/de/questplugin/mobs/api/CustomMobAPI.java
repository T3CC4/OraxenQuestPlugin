package de.questplugin.mobs.api;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.*;

/**
 * Haupt-API für Custom Mob Verwaltung
 */
public class CustomMobAPI {

    private static CustomMobAPI instance;
    private final OraxenQuestPlugin plugin;
    private final Map<UUID, CustomMob> activeMobs;
    private final MobAbilityManager abilityManager;
    private final DefendBehaviorManager defendManager;

    public CustomMobAPI(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.activeMobs = new HashMap<>();
        this.abilityManager = new MobAbilityManager(this);
        this.defendManager = new DefendBehaviorManager(this);
        instance = this;
    }

    /**
     * Spawnt einen Custom Mob
     * @param location Spawn-Location
     * @param type Mob-Typ
     * @return CustomMob Instanz
     */
    public CustomMob spawnCustomMob(Location location, EntityType type) {
        if (!type.isAlive()) {
            throw new IllegalArgumentException("EntityType muss lebendig sein!");
        }

        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        CustomMob customMob = new CustomMob(entity, this);
        activeMobs.put(entity.getUniqueId(), customMob);

        return customMob;
    }

    /**
     * Spawnt einen Custom Mob mit Builder
     */
    public CustomMobBuilder createMob(EntityType type) {
        return new CustomMobBuilder(this, type);
    }

    /**
     * Holt einen Custom Mob anhand der Entity UUID
     */
    public CustomMob getCustomMob(UUID entityUUID) {
        return activeMobs.get(entityUUID);
    }

    /**
     * Holt einen Custom Mob anhand der Entity
     */
    public CustomMob getCustomMob(LivingEntity entity) {
        return activeMobs.get(entity.getUniqueId());
    }

    /**
     * Prüft ob eine Entity ein Custom Mob ist
     */
    public boolean isCustomMob(LivingEntity entity) {
        return activeMobs.containsKey(entity.getUniqueId());
    }

    /**
     * Entfernt einen Custom Mob aus der Verwaltung
     */
    public void removeCustomMob(UUID entityUUID) {
        CustomMob mob = activeMobs.remove(entityUUID);
        if (mob != null) {
            mob.cleanup();
        }
    }

    /**
     * Holt alle aktiven Custom Mobs
     */
    public Collection<CustomMob> getActiveMobs() {
        return Collections.unmodifiableCollection(activeMobs.values());
    }

    /**
     * Holt den Ability Manager
     */
    public MobAbilityManager getAbilityManager() {
        return abilityManager;
    }

    public OraxenQuestPlugin getPlugin() {
        return plugin;
    }

    public static CustomMobAPI getInstance() {
        return instance;
    }

    /**
     * Cleanup aller Mobs
     */
    public void shutdown() {
        activeMobs.values().forEach(CustomMob::cleanup);
        activeMobs.clear();
        abilityManager.shutdown();
        defendManager.shutdown();
    }
}