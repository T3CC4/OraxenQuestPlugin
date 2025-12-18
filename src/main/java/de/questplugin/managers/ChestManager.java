package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.StructureType;
import de.questplugin.utils.StructureHelper;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Verwaltet Chest-Loot
 */
public class ChestManager extends BaseManager {

    private final Set<Location> processedChests = ConcurrentHashMap.newKeySet();
    private final Map<String, List<DropEntry>> lootCache = new ConcurrentHashMap<>();
    private final Queue<Location> processedQueue = new LinkedList<>();

    private static final int MAX_PROCESSED_CHESTS = 10000;
    private static final int SAVE_INTERVAL = 50;

    private volatile boolean isLoaded = false;
    private int saveCounter = 0;

    public ChestManager(OraxenQuestPlugin plugin) {
        super(plugin);
        loadChestLoots();
        loadProcessedChests();
        isLoaded = true;
    }

    private void loadChestLoots() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("chest-loot");
        if (section == null) {
            warn("Keine chest-loot Sektion in Config!");
            return;
        }

        lootCache.clear();
        int totalItems = 0;

        for (String lootTableKey : section.getKeys(false)) {
            List<DropEntry> drops = loadDropEntries(
                    section.getConfigurationSection(lootTableKey),
                    "chest-loot." + lootTableKey
            );

            if (!drops.isEmpty()) {
                lootCache.put(lootTableKey.toLowerCase(), drops);
                totalItems += drops.size();

                debug("Chest-Loot: '" + lootTableKey + "' → " + drops.size() + " Items");
            }
        }

        info("Chest-Loot: " + totalItems + " Items für " + lootCache.size() + " Strukturen");
    }

    private void loadProcessedChests() {
        Set<Location> loaded = plugin.getDataManager().loadProcessedChests();

        if (loaded.size() > MAX_PROCESSED_CHESTS) {
            warn("Zu viele Kisten (" + loaded.size() + "), limitiere auf " + MAX_PROCESSED_CHESTS);
            List<Location> sorted = new ArrayList<>(loaded);
            loaded = new HashSet<>(sorted.subList(sorted.size() - MAX_PROCESSED_CHESTS, sorted.size()));
        }

        processedChests.addAll(loaded);
        processedQueue.addAll(loaded);

        info(processedChests.size() + " verarbeitete Kisten geladen");
    }

    public boolean isProcessed(Location location) {
        return processedChests.contains(location);
    }

    public void markProcessed(Location location) {
        if (!isLoaded) return;

        if (processedChests.size() >= MAX_PROCESSED_CHESTS && !processedChests.contains(location)) {
            Location oldest = processedQueue.poll();
            if (oldest != null) {
                processedChests.remove(oldest);
            }
        }

        if (processedChests.add(location)) {
            processedQueue.offer(location);

            saveCounter++;
            if (saveCounter >= SAVE_INTERVAL) {
                saveData();
                saveCounter = 0;
            }
        }
    }

    public void populateChest(Chest chest, LootTable lootTable) {
        if (lootTable == null || chest == null) {
            warn("populateChest: Chest oder LootTable null!");
            return;
        }

        String lootTableKey = lootTable.getKey().getKey();

        debug("=== Chest Populate ===");
        debug("LootTable: " + lootTableKey);

        List<DropEntry> loots = findMatchingLoots(lootTableKey);

        if (loots == null || loots.isEmpty()) {
            debug("  → KEIN Match");
            debug("======================");
            return;
        }

        debug("  → " + loots.size() + " Loots gefunden");

        Inventory inv = chest.getInventory();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<ItemStack> drops = new ArrayList<>();

        for (DropEntry loot : loots) {
            double roll = random.nextDouble() * 100;
            boolean success = roll < loot.chance;

            if (debugMode) {
                debug("  Loot: " + loot.oraxenItemId);
                debug("    Chance: " + loot.chance + "%");
                debug("    Roll: " + String.format("%.2f", roll));
                debug("    " + (success ? "✓ JA" : "✗ NEIN"));
            }

            if (success) {
                ItemStack item = buildItem(loot.oraxenItemId);
                if (item != null) {
                    int amount = random.nextInt(loot.minAmount, loot.maxAmount + 1);
                    item.setAmount(amount);
                    drops.add(item);

                    debug("    → " + item.getType() + " x" + amount);
                }
            }
        }

        // Platziere Items
        if (!drops.isEmpty()) {
            List<Integer> freeSlots = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) {
                    freeSlots.add(i);
                }
            }

            Collections.shuffle(freeSlots);

            int placed = 0;
            for (int i = 0; i < Math.min(drops.size(), freeSlots.size()); i++) {
                inv.setItem(freeSlots.get(i), drops.get(i));
                placed++;
            }

            debug("  → " + placed + " Items platziert");
        } else {
            debug("  → 0 Items (Rolls fehlgeschlagen)");
        }

        debug("======================");
    }

    private List<DropEntry> findMatchingLoots(String lootTableKey) {
        String normalized = lootTableKey.toLowerCase();

        debug("  Suche: '" + lootTableKey + "'");
        debug("  Keys: " + lootCache.keySet());

        // 1. Direct Match
        if (lootCache.containsKey(normalized)) {
            debug("  → Direct Match");
            return lootCache.get(normalized);
        }

        // 2. Extrahierter Name
        String extracted = extractStructureName(normalized);
        debug("  Extrahiert: '" + extracted + "'");

        if (lootCache.containsKey(extracted)) {
            debug("  → Match mit extrahiertem Namen");
            return lootCache.get(extracted);
        }

        // 3. StructureType Match
        StructureType structureType = StructureHelper.fromString(extracted);
        if (structureType != null) {
            debug("  StructureType: " + structureType);

            for (Map.Entry<String, List<DropEntry>> entry : lootCache.entrySet()) {
                if (StructureHelper.matchesStructure(entry.getKey(), structureType)) {
                    debug("  → StructureType Match: '" + entry.getKey() + "'");
                    return entry.getValue();
                }
            }
        }

        // 4. Partial Match
        debug("  Versuche Partial Match...");
        for (Map.Entry<String, List<DropEntry>> entry : lootCache.entrySet()) {
            String configKey = entry.getKey();

            if (extracted.contains(configKey)) {
                debug("  → Partial: '" + extracted + "' contains '" + configKey + "'");
                return entry.getValue();
            }

            if (configKey.contains(extracted)) {
                debug("  → Reverse: '" + configKey + "' contains '" + extracted + "'");
                return entry.getValue();
            }
        }

        debug("  → KEIN Match");
        return null;
    }

    private String extractStructureName(String lootTableKey) {
        String name = lootTableKey;

        int colonIndex = name.indexOf(':');
        if (colonIndex != -1) {
            name = name.substring(colonIndex + 1);
        }

        int slashIndex = name.lastIndexOf('/');
        if (slashIndex != -1) {
            name = name.substring(slashIndex + 1);
        }

        return name;
    }

    public void saveData() {
        try {
            plugin.getDataManager().saveProcessedChests(processedChests);
        } catch (Exception e) {
            warn("Speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    @Override
    public void reload() {
        isLoaded = false;

        processedChests.clear();
        processedQueue.clear();
        lootCache.clear();
        saveCounter = 0;

        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadChestLoots();
        loadProcessedChests();

        isLoaded = true;
        info("ChestManager neu geladen");
    }
}