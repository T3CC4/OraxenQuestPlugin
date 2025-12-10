package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verwaltet Block-Drops
 */
public class BlockDropManager extends BaseManager {

    private final Map<Material, List<DropEntry>> blockDrops = new ConcurrentHashMap<>();

    public BlockDropManager(OraxenQuestPlugin plugin) {
        super(plugin);
        loadBlockDrops();
    }

    private void loadBlockDrops() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("block-drops");
        if (section == null) {
            warn("Keine block-drops Sektion in Config!");
            return;
        }

        blockDrops.clear();
        int totalDrops = 0;
        int invalidBlocks = 0;

        for (String blockType : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(blockType.toUpperCase());
                List<DropEntry> drops = loadDropEntries(
                        section.getConfigurationSection(blockType),
                        "block-drops." + blockType
                );

                if (!drops.isEmpty()) {
                    blockDrops.put(material, drops);
                    totalDrops += drops.size();

                    debug("Block-Drops: " + material + " → " + drops.size() + " Items");
                    for (DropEntry entry : drops) {
                        debug("  - " + entry.oraxenItemId + " (" + entry.chance + "%)");
                    }
                }
            } catch (IllegalArgumentException e) {
                warn("Ungültiger Block-Typ: " + blockType);
                invalidBlocks++;
            }
        }

        info("Block-Drops: " + totalDrops + " Items für " + blockDrops.size() + " Blöcke" +
                (invalidBlocks > 0 ? " (" + invalidBlocks + " ungültig)" : ""));
    }

    /**
     * Holt Drops für einen Block mit Fortune-Level
     */
    public List<ItemStack> getDrops(Material material, int fortuneLevel) {
        List<DropEntry> entries = blockDrops.get(material);

        debug("getBlockDrops(" + material + ", Fortune=" + fortuneLevel + ")");
        debug("  Einträge: " + (entries != null ? entries.size() : 0));

        if (entries == null || entries.isEmpty()) {
            debug("  → KEINE Drops konfiguriert");
            return Collections.emptyList();
        }

        return processDrops(entries, fortuneLevel);
    }

    /**
     * Verarbeitet Drops mit Fortune-Bonus
     */
    private List<ItemStack> processDrops(List<DropEntry> entries, int fortuneLevel) {
        List<ItemStack> drops = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (DropEntry entry : entries) {
            double modifiedChance = Math.min(100.0, entry.chance + fortuneLevel);
            double roll = random.nextDouble() * 100;
            boolean success = roll < modifiedChance;

            if (debugMode) {
                debug("  Drop: " + entry.oraxenItemId);
                debug("    Chance: " + entry.chance + "% → " + modifiedChance + "% (+" + fortuneLevel + ")");
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
        blockDrops.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadBlockDrops();
    }
}