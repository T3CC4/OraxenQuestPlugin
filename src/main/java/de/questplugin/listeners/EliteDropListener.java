package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.mobs.api.CustomMob;
import de.questplugin.utils.EnchantmentHelper;
import de.questplugin.utils.DropMechanics;
import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Listener für Elite-Mob Deaths mit Custom-Drops
 *
 * FEATURES:
 * - Elite-spezifische Drops aus config.yml
 * - Level-basierte Chance-Multiplikation
 * - Looting-Unterstützung
 * - Bessere Loot-Effekte
 *
 * OPTIMIERT: Nutzt EnchantmentHelper statt duplizierte Logik
 */
public class EliteDropListener implements Listener {

    private final OraxenQuestPlugin plugin;
    private final Map<String, List<EliteDropEntry>> eliteDrops;
    private boolean debugMode;

    public EliteDropListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.eliteDrops = new HashMap<>();
        this.debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadEliteDrops();
    }

    /**
     * Lädt Elite-Drops direkt aus elite-mobs Config
     */
    private void loadEliteDrops() {
        eliteDrops.clear();
        int totalDrops = 0;

        // Lade aus elite-mobs.biomes
        var biomesSection = plugin.getConfig().getConfigurationSection("elite-mobs.biomes");
        if (biomesSection != null) {
            totalDrops += loadEliteDropsFromSection(biomesSection, "biomes");
        }

        // Lade aus elite-mobs.structures
        var structuresSection = plugin.getConfig().getConfigurationSection("elite-mobs.structures");
        if (structuresSection != null) {
            totalDrops += loadEliteDropsFromSection(structuresSection, "structures");
        }

        if (totalDrops == 0) {
            plugin.getPluginLogger().info("Keine Elite-Drops konfiguriert (optional)");
        } else {
            plugin.getPluginLogger().info("Elite-Drops: " + totalDrops + " Items für " +
                    eliteDrops.size() + " Elite-Typen");
        }
    }

    /**
     * Lädt Drops aus einer Config-Sektion (biomes oder structures)
     */
    private int loadEliteDropsFromSection(org.bukkit.configuration.ConfigurationSection section,
                                          String sectionType) {
        int totalDrops = 0;

        for (String eliteId : section.getKeys(false)) {
            var eliteSection = section.getConfigurationSection(eliteId);
            if (eliteSection == null) continue;

            // Prüfe ob "drops" Sektion existiert
            var dropsSection = eliteSection.getConfigurationSection("drops");
            if (dropsSection == null) {
                plugin.getPluginLogger().debug("Elite '" + eliteId + "' hat keine drops");
                continue;
            }

            List<EliteDropEntry> drops = new ArrayList<>();

            for (String dropKey : dropsSection.getKeys(false)) {
                var dropSection = dropsSection.getConfigurationSection(dropKey);
                if (dropSection == null) continue;

                String itemId = dropSection.getString("oraxen-item");
                double chance = dropSection.getDouble("chance", 0);
                int minAmount = dropSection.getInt("min-amount", 1);
                int maxAmount = dropSection.getInt("max-amount", 1);

                // Validierung
                if (!validateItem(itemId)) {
                    plugin.getPluginLogger().warn("Elite '" + eliteId + "." + dropKey +
                            "': Item '" + itemId + "' ungültig");
                    continue;
                }

                if (chance <= 0 || chance > 100) {
                    plugin.getPluginLogger().warn("Elite '" + eliteId + "." + dropKey +
                            "': Ungültige Chance " + chance + "%");
                    continue;
                }

                drops.add(new EliteDropEntry(itemId, chance, minAmount, maxAmount));
            }

            if (!drops.isEmpty()) {
                eliteDrops.put(eliteId.toLowerCase(), drops);
                totalDrops += drops.size();
                plugin.getPluginLogger().debug("Elite '" + sectionType + "." + eliteId +
                        "': " + drops.size() + " Drops");
            }
        }

        return totalDrops;
    }

    /**
     * Event Handler für Elite-Mob Deaths
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEliteDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Prüfe ob Custom Mob
        CustomMob customMob = plugin.getCustomMobAPI().getCustomMob(entity);
        if (customMob == null) {
            return; // Kein Custom Mob
        }

        // Prüfe ob Elite-Mob (hat Custom Name und Level)
        if (customMob.getCustomName() == null || customMob.getLevel() <= 1) {
            return; // Normaler Custom Mob, kein Elite
        }

        plugin.getPluginLogger().debug("=== Elite Death ===");
        plugin.getPluginLogger().debug("Elite: " + customMob.getCustomName());
        plugin.getPluginLogger().debug("Level: " + customMob.getLevel());

        // Finde Elite-ID aus CustomName oder Config
        String eliteId = findEliteId(customMob);
        if (eliteId == null) {
            plugin.getPluginLogger().debug("Keine Elite-ID gefunden");
            return;
        }

        plugin.getPluginLogger().debug("Elite-ID: " + eliteId);

        // Hole Drops
        List<EliteDropEntry> drops = eliteDrops.get(eliteId.toLowerCase());
        if (drops == null || drops.isEmpty()) {
            plugin.getPluginLogger().debug("Keine Drops konfiguriert");
            return;
        }

        // Hole Killer
        // OPTIMIERT: EnchantmentHelper statt duplizierte Methode
        Player killer = entity.getKiller();
        int lootingLevel = 0;

        if (killer != null) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            lootingLevel = EnchantmentHelper.getLootingLevel(weapon);
        }

        plugin.getPluginLogger().debug("Looting: " + lootingLevel);

        // Generiere Drops
        List<ItemStack> generatedDrops = generateEliteDrops(
                drops,
                customMob.getLevel(),
                lootingLevel
        );

        // Füge Drops hinzu
        Location dropLocation = entity.getLocation();
        for (ItemStack drop : generatedDrops) {
            dropLocation.getWorld().dropItemNaturally(dropLocation, drop);

            plugin.getPluginLogger().debug("Elite-Drop: " + drop.getType() +
                    " x" + drop.getAmount());
        }

        // Special Effects wenn Items gedroppt wurden
        if (!generatedDrops.isEmpty()) {
            playEliteDropEffects(dropLocation, generatedDrops.size());

            if (killer != null) {
                killer.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                        .append(Component.text("Elite-Loot: ", NamedTextColor.YELLOW))
                        .append(Component.text(generatedDrops.size() + " Items!", NamedTextColor.WHITE)));
            }
        }

        plugin.getPluginLogger().debug("Total Drops: " + generatedDrops.size());
        plugin.getPluginLogger().debug("==================");
    }

    /**
     * Findet Elite-ID aus CustomMob
     * Versucht über CustomName-Matching mit Config
     */
    private String findEliteId(CustomMob mob) {
        // Hole alle Elite-Configs
        var eliteConfigs = plugin.getEliteMobManager().getEliteIds();

        for (String eliteId : eliteConfigs) {
            var config = plugin.getEliteMobManager().getEliteConfig(eliteId);
            if (config == null) continue;

            // Vergleiche Custom Name (ohne Color Codes)
            String mobName = ChatColor.stripColor(mob.getCustomName());
            String configName = ChatColor.stripColor(
                    ChatColor.translateAlternateColorCodes('&', config.getEliteName())
            );

            if (mobName.equals(configName)) {
                return eliteId;
            }
        }

        // Fallback: Verwende ersten Teil des Custom Names
        String name = ChatColor.stripColor(mob.getCustomName())
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_");

        return name;
    }

    /**
     * Generiert Elite-Drops mit Level- und Looting-Multiplikation
     */
    private List<ItemStack> generateEliteDrops(List<EliteDropEntry> drops,
                                               int eliteLevel,
                                               int lootingLevel) {
        List<ItemStack> results = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Level-Multiplikator: Level 1 = 1.0x, Level 5 = 1.4x, Level 10 = 1.8x
        double levelMultiplier = 1.0 + (eliteLevel - 1) * 0.1;

        plugin.getPluginLogger().debug("Level-Multiplikator: " +
                String.format("%.2f", levelMultiplier) + "x");

        for (EliteDropEntry drop : drops) {
            double baseChance = drop.chance;

            // 1. Level-Boost
            double finalChance = baseChance * levelMultiplier;

            // 2. Looting-Boost (mit Diminishing Returns)
            // OPTIMIERT: Nutzt DropMechanics direkt
            finalChance = DropMechanics.calculateDropChance(finalChance, lootingLevel);

            // 3. Roll
            double roll = random.nextDouble() * 100;
            boolean success = roll < finalChance;

            if (debugMode) {
                plugin.getPluginLogger().debug("  Drop: " + drop.oraxenItemId);
                plugin.getPluginLogger().debug("    Base: " + baseChance + "%");
                plugin.getPluginLogger().debug("    Final: " +
                        String.format("%.2f%%", finalChance));
                plugin.getPluginLogger().debug("    Roll: " +
                        String.format("%.2f", roll));
                plugin.getPluginLogger().debug("    " + (success ? "✓ SUCCESS" : "✗ FAIL"));
            }

            if (success) {
                ItemStack item = buildItem(drop.oraxenItemId);
                if (item != null) {
                    // Amount mit leichtem Looting-Boost
                    int amount = random.nextInt(drop.minAmount, drop.maxAmount + 1);
                    if (lootingLevel > 0 && random.nextDouble() < (lootingLevel * 0.1)) {
                        amount++; // 10% Chance pro Looting-Level für +1
                    }

                    item.setAmount(amount);
                    results.add(item);
                }
            }
        }

        return results;
    }

    /**
     * Spielt Special Effects beim Elite-Drop
     */
    private void playEliteDropEffects(Location location, int dropCount) {
        // Partikel
        Particle particle = Particle.FIREWORK;
        if (dropCount >= 3) {
            particle = Particle.TOTEM_OF_UNDYING;
        }

        location.getWorld().spawnParticle(
                particle,
                location.clone().add(0, 1, 0),
                50,
                0.5, 0.5, 0.5,
                0.1
        );

        // Sound
        Sound sound = dropCount >= 3 ?
                Sound.ENTITY_PLAYER_LEVELUP :
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

        location.getWorld().playSound(location, sound, 1.0f, 1.2f);
    }

    /**
     * Validiert Oraxen-Item
     */
    private boolean validateItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return false;
        }

        try {
            return OraxenItems.exists(itemId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Baut Oraxen-Item
     */
    private ItemStack buildItem(String oraxenItemId) {
        try {
            var builder = OraxenItems.getItemById(oraxenItemId);
            if (builder != null) {
                return builder.build();
            }

            plugin.getPluginLogger().warn("Oraxen-Item nicht gefunden: " + oraxenItemId);
            return null;

        } catch (Exception e) {
            plugin.getPluginLogger().warn("Fehler beim Laden von Item '" +
                    oraxenItemId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Reload
     */
    public void reload() {
        eliteDrops.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadEliteDrops();
    }

    /**
     * Setzt Debug-Mode
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    /**
     * Elite-Drop Entry
     */
    private static class EliteDropEntry {
        final String oraxenItemId;
        final double chance;
        final int minAmount;
        final int maxAmount;

        EliteDropEntry(String oraxenItemId, double chance, int minAmount, int maxAmount) {
            this.oraxenItemId = oraxenItemId;
            this.chance = chance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }
}