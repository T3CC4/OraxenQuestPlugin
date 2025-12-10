package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verwaltet Mob-Drops
 */
public class MobDropManager extends BaseManager {

    private final Map<EntityType, List<DropEntry>> mobDrops = new ConcurrentHashMap<>();

    public MobDropManager(OraxenQuestPlugin plugin) {
        super(plugin);
        loadMobDrops();
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
            try {
                EntityType entityType = EntityType.valueOf(mobType.toUpperCase());
                List<DropEntry> drops = loadDropEntries(
                        section.getConfigurationSection(mobType),
                        "mob-drops." + mobType
                );

                if (!drops.isEmpty()) {
                    mobDrops.put(entityType, drops);
                    totalDrops += drops.size();

                    debug("Mob-Drops: " + entityType + " → " + drops.size() + " Items");
                }
            } catch (IllegalArgumentException e) {
                warn("Ungültiger Mob-Typ: " + mobType);
                invalidMobs++;
            }
        }

        info("Mob-Drops: " + totalDrops + " Items für " + mobDrops.size() + " Mobs" +
                (invalidMobs > 0 ? " (" + invalidMobs + " ungültig)" : ""));
    }

    /**
     * Holt Drops für einen Mob mit Looting-Level
     */
    public List<ItemStack> getDrops(EntityType entityType, int lootingLevel) {
        List<DropEntry> entries = mobDrops.get(entityType);

        debug("getMobDrops(" + entityType + ", Looting=" + lootingLevel + ")");
        debug("  Einträge: " + (entries != null ? entries.size() : 0));

        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        return processDrops(entries, lootingLevel);
    }

    /**
     * Verarbeitet Drops mit Enchantment-Bonus
     */
    private List<ItemStack> processDrops(List<DropEntry> entries, int enchantLevel) {
        List<ItemStack> drops = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (DropEntry entry : entries) {
            double modifiedChance = Math.min(100.0, entry.chance + enchantLevel);
            double roll = random.nextDouble() * 100;
            boolean success = roll < modifiedChance;

            if (debugMode) {
                debug("  Drop: " + entry.oraxenItemId);
                debug("    Chance: " + entry.chance + "% → " + modifiedChance + "% (+" + enchantLevel + ")");
                debug("    Roll: " + String.format("%.2f", roll));
                debug("    " + (success ? "✓ ERFOLG" : "✗ MISS"));
            }

            if (success) {
                ItemStack item = buildItem(entry.oraxenItemId);

                if (item != null) {
                    int amount = random.nextInt(entry.minAmount, entry.maxAmount + 1);

                    if (amount > 0) {
                        item.setAmount(amount);
                        drops.add(item);
                        debug("    → Item: " + item.getType() + " x" + amount);
                    }
                }
            }
        }

        debug("  TOTAL: " + drops.size() + " Drops");
        return drops;
    }

    @Override
    public void reload() {
        mobDrops.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadMobDrops();
    }
}