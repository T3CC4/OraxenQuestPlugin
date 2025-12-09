package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

public class ChestListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public ChestListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLootGenerate(LootGenerateEvent event) {
        // Prüfe ob es eine Kiste ist
        if (!(event.getInventoryHolder() instanceof Chest chest)) {
            return;
        }

        // Prüfe ob bereits verarbeitet
        if (plugin.getChestManager().isProcessed(chest.getLocation())) {
            return;
        }

        // Markiere als verarbeitet
        plugin.getChestManager().markProcessed(chest.getLocation());

        // Füge Custom Items basierend auf LootTable hinzu
        plugin.getChestManager().populateChest(chest, event.getLootTable());
    }
}