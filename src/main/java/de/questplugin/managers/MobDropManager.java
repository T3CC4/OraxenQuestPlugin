package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.utils.DropMechanics;
import de.questplugin.utils.MobHelper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verwaltet Mob-Drops mit fortgeschrittener Drop-Mechanik
 */
public class MobDropManager extends BaseManager {

    private final Map<EntityType, List<DropEntry>> mobDrops = new ConcurrentHashMap<>();

    // Gleiche Drop-Methode wie BlockDropManager
    private BlockDropManager.DropMethod dropMethod = BlockDropManager.DropMethod.HYBRID;

    public MobDropManager(OraxenQuestPlugin plugin) {
        super(plugin);
        loadConfig();
        loadMobDrops();
    }

    private void loadConfig() {
        String methodStr = plugin.getConfig().getString("drop-mechanics.method", "HYBRID");
        try {
            dropMethod = BlockDropManager.DropMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            dropMethod = BlockDropManager.DropMethod.HYBRID;
        }
    }

    private void loadMobDrops() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob-drops");
        if (section == null) {
            warn("Keine mob-drops Sektion in Config!");
            return;
        }

        mobDrops.clear();
        int totalDrops = 0;
        int invalidMobs = 0;

        for (String mobType : section.getKeys(false)) {
            EntityType entityType = MobHelper.parseConfigMob(mobType);

            if (entityType == null) {
                warn("Ungültiger Mob-Typ in mob-drops: '" + mobType + "'");
                invalidMobs++;
                continue;
            }

            List<DropEntry> drops = loadDropEntries(
                    section.getConfigurationSection(mobType),
                    "mob-drops." + mobType
            );

            if (!drops.isEmpty()) {
                mobDrops.put(entityType, drops);
                totalDrops += drops.size();

                debug("Mob-Drops: " + entityType + " → " + drops.size() + " Items");

                for (DropEntry entry : drops) {
                    debug("  - " + entry.oraxenItemId + " (" + entry.chance + "%)");

                    if (debugMode) {
                        debugLootingScaling(entry.chance);
                    }
                }
            }
        }

        info("Mob-Drops: " + totalDrops + " Items für " + mobDrops.size() + " Mobs" +
                (invalidMobs > 0 ? " (" + invalidMobs + " ungültig)" : ""));
    }

    private void debugLootingScaling(double baseChance) {
        debug("  Looting-Skalierung für " + baseChance + "%:");
        for (int looting = 0; looting <= 10; looting += 3) {
            String result = switch (dropMethod) {
                case DIMINISHING -> {
                    double finalChance = DropMechanics.calculateDropChance(baseChance, looting);
                    yield String.format("    Looting %d: %.3f%% (+%.3f%%)",
                            looting, finalChance, finalChance - baseChance);
                }
                case BONUS_ROLLS -> {
                    var rolls = DropMechanics.calculateBonusRolls(baseChance, looting);
                    yield String.format("    Looting %d: %d rolls @ %.3f%% = %.3f%% total",
                            looting, rolls.rolls, rolls.chancePerRoll, rolls.totalChance);
                }
                case HYBRID -> {
                    var hybrid = DropMechanics.calculateHybridDrop(baseChance, looting);
                    yield String.format("    Looting %d: %d rolls @ %.3f%% = %.3f%% total",
                            looting, hybrid.rolls, hybrid.chancePerRoll, hybrid.totalChance);
                }
            };
            debug(result);
        }
    }

    /**
     * Holt Drops für einen Mob mit Looting-Level
     */
    public List<ItemStack> getDrops(EntityType entityType, int lootingLevel) {
        List<DropEntry> entries = mobDrops.get(entityType);

        debug("getMobDrops(" + entityType + ", Looting=" + lootingLevel + ")");
        debug("  Methode: " + dropMethod);
        debug("  Einträge: " + (entries != null ? entries.size() : 0));

        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        return processDrops(entries, lootingLevel);
    }

    /**
     * Identisch zu BlockDropManager, aber für Mobs
     */
    private List<ItemStack> processDrops(List<DropEntry> entries, int lootingLevel) {
        List<ItemStack> drops = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (DropEntry entry : entries) {
            debug("  Drop: " + entry.oraxenItemId + " (Base: " + entry.chance + "%)");

            boolean dropped = false;
            int amount = 0;

            switch (dropMethod) {
                case DIMINISHING:
                    dropped = rollDiminishing(entry.chance, lootingLevel, random);
                    if (dropped) {
                        amount = DropMechanics.calculateDropAmount(
                                entry.minAmount, entry.maxAmount, lootingLevel);
                    }
                    break;

                case BONUS_ROLLS:
                    amount = rollBonusRolls(entry.chance, lootingLevel,
                            entry.minAmount, entry.maxAmount, random);
                    dropped = amount > 0;
                    break;

                case HYBRID:
                    amount = rollHybrid(entry.chance, lootingLevel,
                            entry.minAmount, entry.maxAmount, random);
                    dropped = amount > 0;
                    break;
            }

            if (dropped && amount > 0) {
                ItemStack item = buildItem(entry.oraxenItemId);
                if (item != null) {
                    item.setAmount(amount);
                    drops.add(item);
                    debug("    ✓ ERFOLG: " + item.getType() + " x" + amount);
                }
            } else {
                debug("    ✗ MISS");
            }
        }

        debug("  TOTAL: " + drops.size() + " Drops");
        return drops;
    }

    private boolean rollDiminishing(double baseChance, int lootingLevel, ThreadLocalRandom random) {
        double finalChance = DropMechanics.calculateDropChance(baseChance, lootingLevel);
        double roll = random.nextDouble() * 100;

        if (debugMode) {
            debug("    Diminishing: " + baseChance + "% → " +
                    String.format("%.3f%%", finalChance) +
                    " | Roll: " + String.format("%.3f", roll));
        }

        return roll < finalChance;
    }

    private int rollBonusRolls(double baseChance, int lootingLevel,
                               int minAmount, int maxAmount, ThreadLocalRandom random) {
        var result = DropMechanics.calculateBonusRolls(baseChance, lootingLevel);

        if (debugMode) {
            debug("    Bonus Rolls: " + result);
        }

        int totalAmount = 0;

        for (int i = 0; i < result.rolls; i++) {
            double roll = random.nextDouble() * 100;

            if (roll < result.chancePerRoll) {
                int amount = random.nextInt(minAmount, maxAmount + 1);
                totalAmount += amount;

                if (debugMode) {
                    debug("      Roll " + (i + 1) + ": " +
                            String.format("%.3f < %.3f", roll, result.chancePerRoll) +
                            " → +" + amount);
                }
            } else if (debugMode) {
                debug("      Roll " + (i + 1) + ": " +
                        String.format("%.3f >= %.3f", roll, result.chancePerRoll) +
                        " → Miss");
            }
        }

        return totalAmount;
    }

    private int rollHybrid(double baseChance, int lootingLevel,
                           int minAmount, int maxAmount, ThreadLocalRandom random) {
        var result = DropMechanics.calculateHybridDrop(baseChance, lootingLevel);

        if (debugMode) {
            debug("    Hybrid: " + result);
        }

        int totalAmount = 0;

        for (int i = 0; i < result.rolls; i++) {
            double roll = random.nextDouble() * 100;

            if (roll < result.chancePerRoll) {
                int amount = random.nextInt(minAmount, maxAmount + 1);
                totalAmount += amount;

                if (debugMode) {
                    debug("      Roll " + (i + 1) + ": " +
                            String.format("%.3f < %.3f", roll, result.chancePerRoll) +
                            " → +" + amount);
                }
            } else if (debugMode) {
                debug("      Roll " + (i + 1) + ": " +
                        String.format("%.3f >= %.3f", roll, result.chancePerRoll) +
                        " → Miss");
            }
        }

        return totalAmount;
    }

    public void setDropMethod(BlockDropManager.DropMethod method) {
        this.dropMethod = method;
        info("Drop-Methode geändert zu: " + method);
    }

    public BlockDropManager.DropMethod getDropMethod() {
        return dropMethod;
    }

    @Override
    public void reload() {
        mobDrops.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadConfig();
        loadMobDrops();
    }
}