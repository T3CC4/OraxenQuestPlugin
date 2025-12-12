package de.questplugin.raid;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.utils.MobHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Konfiguration einer Raid-Welle
 * Nutzt MobHelper für case-insensitive Mob-Namen
 */
public class WaveConfig {

    private final int waveNumber;
    private final String displayName;
    private final List<MobSpawn> mobs;
    private final int delayAfterWave; // Sekunden

    private WaveConfig(int waveNumber, String displayName, List<MobSpawn> mobs, int delayAfterWave) {
        this.waveNumber = waveNumber;
        this.displayName = displayName;
        this.mobs = mobs;
        this.delayAfterWave = delayAfterWave;
    }

    /**
     * Lädt WaveConfig aus ConfigurationSection
     */
    public static WaveConfig load(String key, ConfigurationSection section, OraxenQuestPlugin plugin) {
        try {
            int waveNumber = Integer.parseInt(key.replaceAll("\\D+", ""));
            String displayName = section.getString("name", "Welle " + waveNumber);
            int delayAfterWave = section.getInt("delay-after", 5);

            // Mobs laden
            List<MobSpawn> mobs = new ArrayList<>();
            ConfigurationSection mobsSection = section.getConfigurationSection("mobs");

            if (mobsSection != null) {
                for (String mobKey : mobsSection.getKeys(false)) {
                    ConfigurationSection mobSection = mobsSection.getConfigurationSection(mobKey);
                    if (mobSection != null) {
                        MobSpawn mob = MobSpawn.load(mobSection, plugin);
                        if (mob != null) {
                            mobs.add(mob);
                            plugin.getPluginLogger().debug("Welle " + waveNumber + ": Mob '" +
                                    mob.getType() + "' geladen");
                        }
                    }
                }
            }

            if (mobs.isEmpty()) {
                plugin.getLogger().warning("Welle '" + key + "' hat keine Mobs!");
                return null;
            }

            return new WaveConfig(waveNumber, displayName, mobs, delayAfterWave);

        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Laden von Welle '" + key + "': " + e.getMessage());
            return null;
        }
    }

    // ==================== GETTER ====================

    public int getWaveNumber() {
        return waveNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<MobSpawn> getMobs() {
        return mobs;
    }

    public int getDelayAfterWave() {
        return delayAfterWave;
    }

    /**
     * Berechnet Gesamtzahl der Mobs in dieser Welle
     */
    public int getTotalMobCount() {
        return mobs.stream().mapToInt(MobSpawn::getAmount).sum();
    }

    /**
     * Mob-Spawn Datenklasse mit MobHelper
     */
    public static class MobSpawn {
        private final EntityType type;
        private final int amount;
        private final double health;
        private final double damage;
        private final String customName;
        private final boolean hasEquipment;

        private MobSpawn(EntityType type, int amount, double health, double damage,
                         String customName, boolean hasEquipment) {
            this.type = type;
            this.amount = amount;
            this.health = health;
            this.damage = damage;
            this.customName = customName;
            this.hasEquipment = hasEquipment;
        }

        public static MobSpawn load(ConfigurationSection section, OraxenQuestPlugin plugin) {
            try {
                String typeStr = section.getString("type");
                if (typeStr == null) {
                    plugin.getLogger().warning("Mob hat keinen type!");
                    return null;
                }

                // CASE-INSENSITIVE mit MobHelper
                EntityType type = MobHelper.parseConfigMob(typeStr);

                if (type == null) {
                    plugin.getLogger().warning("Ungültiger Mob-Typ: '" + typeStr + "'");
                    plugin.getLogger().warning("  Nutze /quest mobs für gültige Mob-Namen");
                    plugin.getLogger().warning("  Beispiel: ZOMBIE, zombie, Zombie (alle funktionieren)");
                    return null;
                }

                int amount = section.getInt("amount", 1);
                double health = section.getDouble("health", 20.0);
                double damage = section.getDouble("damage", 1.0);
                String customName = section.getString("custom-name");
                boolean hasEquipment = section.getBoolean("use-equipment", false);

                plugin.getPluginLogger().debug("Mob geladen: " + type +
                        " (Original: '" + typeStr + "')");

                return new MobSpawn(type, amount, health, damage, customName, hasEquipment);

            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Laden von Mob: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        public EntityType getType() { return type; }
        public int getAmount() { return amount; }
        public double getHealth() { return health; }
        public double getDamage() { return damage; }
        public String getCustomName() { return customName; }
        public boolean hasEquipment() { return hasEquipment; }
    }
}