package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.CraftingManager;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für Custom Anvil-Rezepte mit zwei Input-Slots
 *
 * FIXES:
 * - PrepareAnvilEvent setzt Preview (Slot 2)
 * - InventoryClickEvent konsumiert Items und XP
 * - Vanilla-Anvil wird für Custom-Rezepte blockiert
 */
public class AnvilListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public AnvilListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * PrepareAnvilEvent - Zeigt Preview des Outputs
     * Wird getriggert wenn Items in Anvil gelegt werden
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();

        // Hole Input-Items
        ItemStack firstInput = anvil.getItem(0);
        ItemStack secondInput = anvil.getItem(1);

        // Validierung
        if (firstInput == null) {
            return;
        }

        plugin.getPluginLogger().debug("=== Anvil Prepare ===");
        plugin.getPluginLogger().debug("First: " + firstInput.getType());
        if (secondInput != null) {
            plugin.getPluginLogger().debug("Second: " + secondInput.getType());
        }

        // Hole Oraxen-IDs (mit Fallback auf ItemMeta)
        String firstId = getOraxenId(firstInput);
        String secondId = secondInput != null ? getOraxenId(secondInput) : null;

        plugin.getPluginLogger().debug("First ID: " + firstId);
        plugin.getPluginLogger().debug("Second ID: " + secondId);

        if (firstId == null) {
            plugin.getPluginLogger().debug("First Input ist kein Oraxen-Item");
            return;
        }

        // Suche Rezept direkt über IDs
        CraftingManager.CraftingRecipe recipe = plugin.getCraftingManager()
                .findRecipeByIds(firstId, secondId);

        if (recipe == null) {
            plugin.getPluginLogger().debug("Kein Rezept gefunden");
            return;
        }

        plugin.getPluginLogger().debug("Rezept gefunden: " + recipe.getKey());

        // Erstelle Output-Preview
        ItemStack output = recipe.createOutput();
        if (output == null) {
            plugin.getPluginLogger().severe("Output konnte nicht erstellt werden");
            return;
        }

        // WICHTIG: Setze Result sowohl im Event als auch direkt im Inventory
        event.setResult(output);

        // Direkt ins Inventory setzen (Slot 2 = Result-Slot)
        // Mit Delay weil Minecraft das Result manchmal überschreibt
        final ItemStack finalOutput = output.clone();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            anvil.setItem(2, finalOutput);
            plugin.getPluginLogger().debug("Output delayed ins Inventory gesetzt");
        });

        // Setze Repair-Cost (XP-Anzeige im Anvil)
        // NEU in 1.21: Nutze AnvilView statt AnvilInventory
        if (event.getView() instanceof org.bukkit.inventory.view.AnvilView) {
            org.bukkit.inventory.view.AnvilView anvilView =
                    (org.bukkit.inventory.view.AnvilView) event.getView();
            anvilView.setRepairCost(recipe.getExpCost());

            // WICHTIG: Setze Maximum höher als die Kosten
            // Vanilla = 40, wir setzen es dynamisch auf mindestens die Kosten + 10
            int maxCost = Math.max(100, recipe.getExpCost() + 10);
            anvilView.setMaximumRepairCost(maxCost);

            plugin.getPluginLogger().debug("Max Repair Cost gesetzt: " + maxCost);
        }

        plugin.getPluginLogger().debug("Output gesetzt: " + output.getType() + " x" + output.getAmount());
        plugin.getPluginLogger().debug("XP-Cost: " + recipe.getExpCost());
        plugin.getPluginLogger().debug("=====================");
    }

    /**
     * Holt Oraxen-ID aus ItemStack
     * Nutzt OraxenItems API und Fallback auf PersistentDataContainer
     */
    private String getOraxenId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        // Versuch 1: OraxenItems.getIdByItem (funktioniert manchmal nicht im Anvil)
        String id = OraxenItems.getIdByItem(item);
        if (id != null) {
            plugin.getPluginLogger().debug("  Oraxen-ID via API: " + id);
            return id;
        }

        // Versuch 2: PersistentDataContainer (direkter Zugriff auf Oraxen-Metadaten)
        try {
            org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("oraxen", "id");

            if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                String pdcId = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                plugin.getPluginLogger().debug("  Oraxen-ID via PDC: " + pdcId);
                return pdcId;
            }
        } catch (Exception e) {
            plugin.getPluginLogger().debug("  PDC-Fehler: " + e.getMessage());
        }

        plugin.getPluginLogger().debug("  Kein Oraxen-Item gefunden");
        return null;
    }

    /**
     * InventoryClickEvent - Konsumiert Items und XP
     * Wird getriggert wenn Spieler Output nimmt
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilClick(InventoryClickEvent event) {
        // Prüfungen
        if (!(event.getInventory() instanceof AnvilInventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        Player player = (Player) event.getWhoClicked();
        AnvilInventory anvil = (AnvilInventory) event.getInventory();

        // Hole Items
        ItemStack firstInput = anvil.getItem(0);
        ItemStack secondInput = anvil.getItem(1);
        ItemStack result = anvil.getItem(2);

        if (firstInput == null || result == null) return;

        plugin.getPluginLogger().debug("=== Anvil Click ===");
        plugin.getPluginLogger().debug("Player: " + player.getName());

        // Hole Oraxen-IDs (mit PDC-Fallback)
        String firstId = getOraxenId(firstInput);
        String resultId = getOraxenId(result);

        plugin.getPluginLogger().debug("First ID: " + firstId);
        plugin.getPluginLogger().debug("Result ID: " + resultId);

        if (firstId == null || resultId == null) {
            plugin.getPluginLogger().debug("Keine Oraxen-Items - Vanilla-Anvil");
            return; // Vanilla-Anvil durchlassen
        }

        // Hole second-input ID
        String secondId = secondInput != null ? getOraxenId(secondInput) : null;
        plugin.getPluginLogger().debug("Second ID: " + secondId);

        // Suche Rezept direkt über IDs
        CraftingManager.CraftingRecipe recipe = plugin.getCraftingManager()
                .findRecipeByIds(firstId, secondId);

        if (recipe == null) {
            plugin.getPluginLogger().debug("Kein Custom-Rezept - Vanilla-Anvil");
            return; // Vanilla-Anvil durchlassen
        }

        // Validiere dass Result mit Rezept übereinstimmt
        if (!resultId.equals(recipe.getOutputItem())) {
            plugin.getPluginLogger().warn("Result-Item stimmt nicht mit Rezept überein!");
            plugin.getPluginLogger().warn("  Erwartet: " + recipe.getOutputItem());
            plugin.getPluginLogger().warn("  Erhalten: " + resultId);
            event.setCancelled(true);
            return;
        }

        plugin.getPluginLogger().debug("Custom-Rezept: " + recipe.getKey());

        // Prüfe XP
        int totalXP = calculateTotalExperience(player);
        int requiredXP = recipe.getExpCost();

        if (requiredXP > 0 && totalXP < requiredXP) {
            plugin.getPluginLogger().debug("Nicht genug XP: " + totalXP + "/" + requiredXP);
            event.setCancelled(true);
            player.sendMessage(org.bukkit.ChatColor.RED +
                    "Nicht genug XP! Benötigt: " + requiredXP + " (Du hast: " + totalXP + ")");
            return;
        }

        // ERFOLG - Konsumiere Items und XP

        // 1) Items konsumieren (wird NACH dem Event gemacht)
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            // First Input -1
            if (firstInput.getAmount() > 1) {
                firstInput.setAmount(firstInput.getAmount() - 1);
            } else {
                anvil.setItem(0, null);
            }

            // Second Input -1 (falls vorhanden)
            if (secondInput != null) {
                if (secondInput.getAmount() > 1) {
                    secondInput.setAmount(secondInput.getAmount() - 1);
                } else {
                    anvil.setItem(1, null);
                }
            }

            plugin.getPluginLogger().debug("Items konsumiert");
        });

        // 2) XP abziehen
        if (requiredXP > 0) {
            int newXP = totalXP - requiredXP;
            setTotalExperience(player, newXP);
            plugin.getPluginLogger().debug("XP abgezogen: " + requiredXP + " (Rest: " + newXP + ")");
        }

        // 3) Erfolgs-Nachricht
        player.sendMessage(org.bukkit.ChatColor.GREEN + "✓ " +
                recipe.getOutputItem() + " x" + recipe.getOutputAmount() + " hergestellt!");

        plugin.getPluginLogger().debug("===================");
    }

    /**
     * Berechnet totale XP eines Spielers
     */
    private int calculateTotalExperience(Player player) {
        int level = player.getLevel();
        float exp = player.getExp();

        // XP für vorherige Level
        int xpForLevel = getXPForLevel(level);

        // XP zum nächsten Level
        int xpToNext = getXPToNextLevel(level);

        // Aktuelle XP in diesem Level
        int currentLevelXP = Math.round(exp * xpToNext);

        return xpForLevel + currentLevelXP;
    }

    /**
     * Gibt XP zurück die benötigt wurden um zu diesem Level zu kommen
     */
    private int getXPForLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * Gibt XP zurück die benötigt werden um zum nächsten Level zu kommen
     */
    private int getXPToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    /**
     * Setzt totale XP eines Spielers
     */
    private void setTotalExperience(Player player, int amount) {
        // Reset
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        // Neue XP geben
        if (amount > 0) {
            player.giveExp(amount);
        }
    }
}