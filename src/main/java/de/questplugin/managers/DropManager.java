package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.*;

public class DropManager {

    private final OraxenQuestPlugin plugin;
    private final Map<EntityType, List<DropEntry>> mobDrops;
    private final Map<Material, List<DropEntry>> blockDrops;
    private final Random random;

    public DropManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.mobDrops = new HashMap<>();
        this.blockDrops = new HashMap<>();
        this.random = new Random();
        loadDrops();
    }

    private void loadDrops() {
        // Mob Drops laden
        ConfigurationSection mobSection = plugin.getConfig().getConfigurationSection("mob-drops");
        if (mobSection != null) {
            for (String mobType : mobSection.getKeys(false)) {
                try {
                    EntityType entityType = EntityType.valueOf(mobType.toUpperCase());
                    List<DropEntry> drops = new ArrayList<>();

                    ConfigurationSection dropSection = mobSection.getConfigurationSection(mobType);
                    if (dropSection != null) {
                        for (String key : dropSection.getKeys(false)) {
                            String itemId = dropSection.getString(key + ".oraxen-item");
                            double chance = dropSection.getDouble(key + ".chance");
                            int minAmount = dropSection.getInt(key + ".min-amount", 1);
                            int maxAmount = dropSection.getInt(key + ".max-amount", 1);

                            drops.add(new DropEntry(itemId, chance, minAmount, maxAmount));
                        }
                    }

                    mobDrops.put(entityType, drops);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ungültiger Mob-Typ: " + mobType);
                }
            }
        }

        // Block Drops laden
        ConfigurationSection blockSection = plugin.getConfig().getConfigurationSection("block-drops");
        if (blockSection != null) {
            for (String blockType : blockSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(blockType.toUpperCase());
                    List<DropEntry> drops = new ArrayList<>();

                    ConfigurationSection dropSection = blockSection.getConfigurationSection(blockType);
                    if (dropSection != null) {
                        for (String key : dropSection.getKeys(false)) {
                            String itemId = dropSection.getString(key + ".oraxen-item");
                            double chance = dropSection.getDouble(key + ".chance");
                            int minAmount = dropSection.getInt(key + ".min-amount", 1);
                            int maxAmount = dropSection.getInt(key + ".max-amount", 1);

                            drops.add(new DropEntry(itemId, chance, minAmount, maxAmount));
                        }
                    }

                    blockDrops.put(material, drops);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ungültiger Block-Typ: " + blockType);
                }
            }
        }
    }

    public List<ItemStack> getMobDrops(EntityType entityType, int lootingLevel) {
        List<ItemStack> drops = new ArrayList<>();
        List<DropEntry> entries = mobDrops.get(entityType);

        if (entries != null) {
            for (DropEntry entry : entries) {
                // Prüfe zuerst ob Item existiert
                if (!OraxenItems.exists(entry.oraxenItemId)) {
                    continue;
                }

                // Berechne modifizierte Chance mit Looting
                // Jedes Looting-Level erhöht Chance um 1% (0.01)
                double modifiedChance = entry.chance + (lootingLevel * 1.0);

                // Chance darf max 100% sein
                modifiedChance = Math.min(modifiedChance, 100.0);

                if (random.nextDouble() * 100 < modifiedChance) {
                    ItemStack item = OraxenItems.getItemById(entry.oraxenItemId).build();
                    if (item != null) {
                        int amount = entry.minAmount + random.nextInt(entry.maxAmount - entry.minAmount + 1);

                        // Skip wenn Amount 0 oder negativ
                        if (amount <= 0) {
                            continue;
                        }

                        item.setAmount(amount);
                        drops.add(item);
                    }
                }
            }
        }

        return drops;
    }

    public List<ItemStack> getBlockDrops(Material material, int fortuneLevel) {
        List<ItemStack> drops = new ArrayList<>();
        List<DropEntry> entries = blockDrops.get(material);

        if (entries == null) {
            plugin.getLogger().fine("Keine Drops konfiguriert für: " + material);
            return drops;
        }

        plugin.getLogger().fine("Prüfe " + entries.size() + " Drop-Einträge für " + material);

        for (DropEntry entry : entries) {
            // Prüfe zuerst ob Item existiert
            if (!OraxenItems.exists(entry.oraxenItemId)) {
                plugin.getLogger().warning("Oraxen-Item existiert nicht: " + entry.oraxenItemId);
                continue;
            }

            // Berechne modifizierte Chance mit Fortune/Luck
            // Jedes Fortune-Level erhöht Chance um 1%
            double modifiedChance = entry.chance + (fortuneLevel * 1.0);

            // Chance darf max 100% sein
            modifiedChance = Math.min(modifiedChance, 100.0);

            double roll = random.nextDouble() * 100;
            boolean success = roll < modifiedChance;

            plugin.getLogger().fine("  Drop '" + entry.oraxenItemId + "': " +
                    "Base=" + entry.chance + "% " +
                    "Modified=" + modifiedChance + "% " +
                    "Roll=" + String.format("%.2f", roll) + " " +
                    (success ? "✓ SUCCESS" : "✗ FAIL"));

            if (success) {
                ItemStack item = OraxenItems.getItemById(entry.oraxenItemId).build();
                if (item != null) {
                    // Berechne Amount (min-max inclusive)
                    int amount = entry.minAmount + random.nextInt(entry.maxAmount - entry.minAmount + 1);

                    // Wenn Amount 0 ist: Skip diesen Drop
                    if (amount <= 0) {
                        plugin.getLogger().fine("    → Amount ist 0, Skip!");
                        continue;
                    }

                    item.setAmount(amount);
                    drops.add(item);
                    plugin.getLogger().fine("    → Item gebaut: " + item.getType() + " x" + amount);
                } else {
                    plugin.getLogger().warning("    → Item konnte nicht gebaut werden!");
                }
            }
        }

        return drops;
    }

    // Legacy-Methode für backwards compatibility
    public List<ItemStack> getBlockDrops(Material material) {
        return getBlockDrops(material, 0);
    }

    public void reload() {
        mobDrops.clear();
        blockDrops.clear();
        loadDrops();
        plugin.getLogger().info("Drop-Config neu geladen: " + mobDrops.size() + " Mob-Typen, " + blockDrops.size() + " Block-Typen");
    }

    private static class DropEntry {
        String oraxenItemId;
        double chance;
        int minAmount;
        int maxAmount;

        DropEntry(String oraxenItemId, double chance, int minAmount, int maxAmount) {
            this.oraxenItemId = oraxenItemId;
            this.chance = chance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }
}