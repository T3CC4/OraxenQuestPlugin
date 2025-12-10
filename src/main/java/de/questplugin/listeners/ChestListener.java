package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class ChestListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public ChestListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * LootGenerateEvent = wird getriggert wenn Loot generiert wird
     * NICHT beim Öffnen, sondern beim ersten Loot-Generation
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLootGenerate(LootGenerateEvent event) {
        plugin.getPluginLogger().info("=== LootGenerateEvent ===");
        plugin.getPluginLogger().info("Holder: " + event.getInventoryHolder());
        plugin.getPluginLogger().info("LootTable: " + (event.getLootTable() != null ? event.getLootTable().getKey() : "NULL"));

        // Prüfe ob es eine Kiste ist
        if (!(event.getInventoryHolder() instanceof Chest)) {
            plugin.getPluginLogger().info("Kein Chest - Skip");
            plugin.getPluginLogger().info("========================");
            return;
        }

        Chest chest = (Chest) event.getInventoryHolder();

        plugin.getLogger().info("Location: " + chest.getLocation());

        // Prüfe ob bereits verarbeitet
        if (plugin.getChestManager().isProcessed(chest.getLocation())) {
            plugin.getPluginLogger().info("Bereits verarbeitet - Skip");
            plugin.getPluginLogger().info("========================");
            return;
        }

        // Markiere als verarbeitet
        plugin.getChestManager().markProcessed(chest.getLocation());
        plugin.getPluginLogger().info("Als verarbeitet markiert");

        // Füge Custom Items hinzu
        plugin.getChestManager().populateChest(chest, event.getLootTable());

        plugin.getPluginLogger().info("Custom Items hinzugefügt");
        plugin.getPluginLogger().info("========================");
    }

    /**
     * Zusätzlicher Debug-Listener für Inventory Open
     * Um zu sehen WANN Kisten geöffnet werden
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof Chest) {
            Chest chest = (Chest) holder;
            plugin.getPluginLogger().info("=== Chest geöffnet ===");
            plugin.getPluginLogger().info("Spieler: " + event.getPlayer().getName());
            plugin.getPluginLogger().info("Location: " + chest.getLocation());
            plugin.getPluginLogger().info("Inventory Size: " + event.getInventory().getSize());
            plugin.getPluginLogger().info("Items in Chest: " + event.getInventory().getContents().length);

            // Zähle nicht-null Items
            int itemCount = 0;
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null) itemCount++;
            }
            plugin.getPluginLogger().info("Nicht-leere Slots: " + itemCount);
            plugin.getPluginLogger().info("======================");
        }
    }
}