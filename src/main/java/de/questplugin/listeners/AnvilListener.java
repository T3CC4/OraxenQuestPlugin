package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.CraftingManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für Custom Anvil-Rezepte
 */
public class AnvilListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public AnvilListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilClick(InventoryClickEvent event) {
        // Prüfungen
        if (!(event.getInventory() instanceof AnvilInventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        Player player = (Player) event.getWhoClicked();
        AnvilInventory anvil = (AnvilInventory) event.getInventory();

        // Hole Items
        ItemStack input = anvil.getItem(0);
        ItemStack result = anvil.getItem(2);

        if (input == null || result == null) return;

        plugin.getPluginLogger().debug("=== Anvil Click ===");
        plugin.getPluginLogger().debug("Player: " + player.getName());
        plugin.getPluginLogger().debug("Input: " + input.getType());

        // Suche Rezept
        CraftingManager.CraftingRecipe recipe = plugin.getCraftingManager()
                .findAnvilRecipe(input);

        if (recipe == null) {
            plugin.getPluginLogger().debug("Kein Rezept gefunden");
            return;
        }

        plugin.getPluginLogger().debug("Rezept: " + recipe.getKey());

        // Prüfe XP
        if (recipe.getExpCost() > 0) {
            int playerXP = calculateTotalExperience(player);

            if (playerXP < recipe.getExpCost()) {
                plugin.getPluginLogger().debug("Nicht genug XP: " +
                        playerXP + "/" + recipe.getExpCost());
                event.setCancelled(true);
                return;
            }
        }

        // Erstelle Output
        ItemStack output = recipe.createOutput();
        if (output == null) {
            plugin.getPluginLogger().severe("Konnte Output nicht erstellen");
            event.setCancelled(true);
            return;
        }

        // Setze Result
        anvil.setItem(2, output);

        // Ziehe XP ab
        if (recipe.getExpCost() > 0) {
            int newXP = calculateTotalExperience(player) - recipe.getExpCost();
            player.setExp(0);
            player.setLevel(0);
            player.setTotalExperience(0);
            player.giveExp(Math.max(0, newXP));

            plugin.getPluginLogger().debug("XP abgezogen: " + recipe.getExpCost());
        }

        plugin.getPluginLogger().debug("===================");
    }

    /**
     * Berechnet totale XP eines Spielers
     */
    private int calculateTotalExperience(Player player) {
        int level = player.getLevel();
        int exp = Math.round(player.getExp() * player.getExpToLevel());

        // XP für vorherige Level
        if (level <= 16) {
            return level * level + 6 * level + exp;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360 + exp);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220 + exp);
        }
    }
}