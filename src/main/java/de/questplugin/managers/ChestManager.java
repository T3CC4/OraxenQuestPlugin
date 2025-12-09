package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.StructureType;
import de.questplugin.utils.StructureHelper;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;

import java.util.*;

public class ChestManager {

    private final OraxenQuestPlugin plugin;
    private final Set<Location> processedChests;
    private final Map<String, List<ChestLoot>> lootTableLoots;
    private final Random random;

    public ChestManager(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.processedChests = new HashSet<>();
        this.lootTableLoots = new HashMap<>();
        this.random = new Random();
        loadChestLoots();
        loadProcessedChests();
    }

    private void loadChestLoots() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("chest-loot");
        if (section == null) return;

        int validStructures = 0;
        int unknownStructures = 0;

        for (String lootTableKey : section.getKeys(false)) {
            List<ChestLoot> loots = new ArrayList<>();
            ConfigurationSection lootSection = section.getConfigurationSection(lootTableKey);

            if (lootSection != null) {
                for (String key : lootSection.getKeys(false)) {
                    String oraxenItem = lootSection.getString(key + ".oraxen-item");
                    double chance = lootSection.getDouble(key + ".chance");
                    int minAmount = lootSection.getInt(key + ".min-amount", 1);
                    int maxAmount = lootSection.getInt(key + ".max-amount", 1);

                    loots.add(new ChestLoot(oraxenItem, chance, minAmount, maxAmount));
                }
            }

            // Speichere mit lowercase key für einfacheren Vergleich
            lootTableLoots.put(lootTableKey.toLowerCase(), loots);

            // Validiere gegen StructureType (optional - nur zur Info)
            StructureType structureType = StructureHelper.fromString(lootTableKey);
            if (structureType != null) {
                validStructures++;
                plugin.getLogger().info("Chest-Loot geladen für Struktur: " +
                        structureType.getDisplayName() + " (" + loots.size() + " Items)");
            } else {
                // Prüfe ob es ein Partial-Match ist (z.B. "village" für alle Village-Strukturen)
                boolean isPartialMatch = false;
                for (StructureType type : StructureType.values()) {
                    if (StructureHelper.matchesStructure(lootTableKey, type)) {
                        isPartialMatch = true;
                        break;
                    }
                }

                if (isPartialMatch) {
                    validStructures++;
                    plugin.getLogger().info("Chest-Loot geladen (Partial-Match): " +
                            lootTableKey + " (" + loots.size() + " Items)");
                } else {
                    unknownStructures++;
                    plugin.getLogger().warning("Unbekannte Struktur in Config: " + lootTableKey +
                            " (siehe StructureType.java für verfügbare Strukturen)");
                }
            }
        }

        plugin.getLogger().info("Chest-Loot geladen: " + validStructures + " Strukturen, " +
                unknownStructures + " unbekannte");
    }

    private void loadProcessedChests() {
        processedChests.addAll(plugin.getDataManager().loadProcessedChests());
        plugin.getLogger().info(processedChests.size() + " verarbeitete Kisten geladen");
    }

    public boolean isProcessed(Location location) {
        return processedChests.contains(location);
    }

    public void markProcessed(Location location) {
        if (processedChests.add(location)) {
            // Speichere in regelmäßigen Intervallen (alle 10 Kisten)
            if (processedChests.size() % 10 == 0) {
                saveData();
            }
        }
    }

    public void populateChest(Chest chest, LootTable lootTable) {
        if (lootTable == null) {
            return;
        }

        // Hole LootTable Key und extrahiere den Namen
        String lootTableKey = lootTable.getKey().getKey();

        // Finde passende Config basierend auf LootTable
        List<ChestLoot> loots = findMatchingLoots(lootTableKey);

        if (loots == null || loots.isEmpty()) {
            return;
        }

        Inventory inv = chest.getInventory();

        for (ChestLoot loot : loots) {
            // Prüfe zuerst ob Item existiert
            if (!OraxenItems.exists(loot.oraxenItemId)) {
                continue;
            }

            if (random.nextDouble() * 100 < loot.chance) {
                ItemStack item = OraxenItems.getItemById(loot.oraxenItemId).build();
                if (item != null) {
                    int amount = loot.minAmount + random.nextInt(loot.maxAmount - loot.minAmount + 1);
                    item.setAmount(amount);

                    // Finde einen freien Slot
                    int slot = random.nextInt(inv.getSize());
                    int attempts = 0;
                    while (inv.getItem(slot) != null && attempts < inv.getSize()) {
                        slot = (slot + 1) % inv.getSize();
                        attempts++;
                    }

                    if (inv.getItem(slot) == null) {
                        inv.setItem(slot, item);
                    }
                }
            }
        }
    }

    private List<ChestLoot> findMatchingLoots(String lootTableKey) {
        String normalizedKey = lootTableKey.toLowerCase();

        plugin.getLogger().fine("Suche Loot für LootTable: " + lootTableKey);

        // 1. Direkter Match
        if (lootTableLoots.containsKey(normalizedKey)) {
            plugin.getLogger().fine("✓ Direkt-Match gefunden: " + normalizedKey);
            return lootTableLoots.get(normalizedKey);
        }

        // 2. Extrahiere den reinen Struktur-Namen aus dem LootTable-Key
        // z.B. "minecraft:chests/village_weaponsmith" → "village_weaponsmith"
        String structureName = normalizedKey;
        if (structureName.contains("/")) {
            structureName = structureName.substring(structureName.lastIndexOf("/") + 1);
        }
        if (structureName.contains(":")) {
            structureName = structureName.substring(structureName.indexOf(":") + 1);
        }

        plugin.getLogger().fine("Extrahierter Struktur-Name: " + structureName);

        // 3. Prüfe gegen StructureType Enum
        StructureType structureType = StructureHelper.fromString(structureName);
        if (structureType != null) {
            // Suche nach Config-Einträgen die zu diesem StructureType passen
            for (Map.Entry<String, List<ChestLoot>> entry : lootTableLoots.entrySet()) {
                if (StructureHelper.matchesStructure(entry.getKey(), structureType)) {
                    plugin.getLogger().fine("✓ StructureType-Match gefunden: " +
                            entry.getKey() + " → " + structureType.getDisplayName());
                    return entry.getValue();
                }
            }
        }

        // 4. Teil-Matches (Legacy-Support)
        // z.B. "village_weaponsmith" matched mit "village"
        for (Map.Entry<String, List<ChestLoot>> entry : lootTableLoots.entrySet()) {
            String configKey = entry.getKey();

            // Prüfe ob der LootTable-Key den Config-Key enthält
            if (structureName.contains(configKey)) {
                plugin.getLogger().fine("✓ Partial-Match gefunden: " + configKey + " in " + structureName);
                return entry.getValue();
            }

            // Prüfe auch umgekehrt (für spezifischere Matches)
            if (configKey.contains(structureName)) {
                plugin.getLogger().fine("✓ Reverse-Match gefunden: " + structureName + " in " + configKey);
                return entry.getValue();
            }
        }

        plugin.getLogger().fine("✗ Kein Match gefunden für: " + lootTableKey);
        return null;
    }

    public void saveData() {
        plugin.getDataManager().saveProcessedChests(processedChests);
    }

    public void reload() {
        processedChests.clear();
        lootTableLoots.clear();
        loadChestLoots();
        loadProcessedChests();
        plugin.getLogger().info("Chest-Loot-Config neu geladen: " + lootTableLoots.size() + " LootTable-Typen");
    }

    private static class ChestLoot {
        String oraxenItemId;
        double chance;
        int minAmount;
        int maxAmount;

        ChestLoot(String oraxenItemId, double chance, int minAmount, int maxAmount) {
            this.oraxenItemId = oraxenItemId;
            this.chance = chance;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }
}