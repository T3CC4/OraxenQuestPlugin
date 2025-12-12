package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Custom Crafting Rezepte für Anvil
 */
public class CraftingManager extends BaseManager {

    // Rezept-Cache: RecipeKey -> Recipe
    private final Map<String, CraftingRecipe> recipes = new ConcurrentHashMap<>();

    public CraftingManager(OraxenQuestPlugin plugin) {
        super(plugin);
        loadRecipes();
    }

    private void loadRecipes() {
        recipes.clear();

        // Lade recipes.yml
        File recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        if (!recipesFile.exists()) {
            plugin.saveResource("recipes.yml", false);
        }

        org.bukkit.configuration.file.FileConfiguration recipesConfig =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(recipesFile);

        // Lade Anvil-Rezepte
        loadAnvilRecipes("anvil-recipes", recipesConfig);

        info("Crafting-Rezepte: " + recipes.size() + " geladen");
    }

    private void loadAnvilRecipes(String configPath,
                                  org.bukkit.configuration.file.FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(configPath);
        if (section == null) {
            debug("Keine " + configPath + " Sektion in recipes.yml");
            return;
        }

        int loaded = 0;

        for (String recipeKey : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(recipeKey);
            if (recipeSection == null) continue;

            CraftingRecipe recipe = loadRecipe(recipeKey, recipeSection);
            if (recipe != null) {
                recipes.put(recipeKey, recipe);
                loaded++;

                debug("Rezept geladen: " + recipeKey);
                debug("  First Input: " + recipe.getFirstInput());
                if (recipe.getSecondInput() != null) {
                    debug("  Second Input: " + recipe.getSecondInput());
                }
                debug("  Output: " + recipe.getOutputItem() + " x" + recipe.getOutputAmount());
                debug("  Cost: " + recipe.getExpCost() + " XP");
            }
        }

        info("Anvil-Rezepte: " + loaded + " geladen");
    }

    private CraftingRecipe loadRecipe(String key, ConfigurationSection section) {
        String firstInput = section.getString("first-input");
        String secondInput = section.getString("second-input");
        String outputItem = section.getString("output");
        int outputAmount = section.getInt("output-amount", 1);
        int expCost = section.getInt("exp-cost", 0);

        // Validierung
        if (!validateItem(firstInput)) {
            warn("Rezept '" + key + "': First-Input '" + firstInput + "' ungültig");
            return null;
        }

        // Second-Input ist optional
        if (secondInput != null && !validateItem(secondInput)) {
            warn("Rezept '" + key + "': Second-Input '" + secondInput + "' ungültig");
            return null;
        }

        if (!validateItem(outputItem)) {
            warn("Rezept '" + key + "': Output-Item '" + outputItem + "' ungültig");
            return null;
        }

        if (outputAmount < 1) {
            warn("Rezept '" + key + "': output-amount muss mindestens 1 sein");
            return null;
        }

        return new CraftingRecipe(
                key,
                firstInput,
                secondInput,
                outputItem,
                outputAmount,
                expCost
        );
    }

    /**
     * Findet Rezept für Anvil (zwei Items)
     */
    public CraftingRecipe findAnvilRecipe(ItemStack firstInput, ItemStack secondInput) {
        if (firstInput == null) return null;

        String firstInputId = OraxenItems.getIdByItem(firstInput);
        if (firstInputId == null) return null;

        String secondInputId = null;
        if (secondInput != null) {
            secondInputId = OraxenItems.getIdByItem(secondInput);
        }

        return findRecipe(firstInputId, secondInputId);
    }

    /**
     * Interne Methode zum Finden eines Rezepts
     */
    private CraftingRecipe findRecipe(String firstInputId, String secondInputId) {
        if (recipes.isEmpty()) {
            return null;
        }

        for (CraftingRecipe recipe : recipes.values()) {
            // Prüfe erstes Item
            if (!recipe.getFirstInput().equalsIgnoreCase(firstInputId)) {
                continue;
            }

            // Kein zweites Item konfiguriert = Match
            if (recipe.getSecondInput() == null && secondInputId == null) {
                debug("Match gefunden: " + recipe.getKey());
                return recipe;
            }

            // Zweites Item muss matchen
            if (recipe.getSecondInput() != null && secondInputId != null &&
                    recipe.getSecondInput().equalsIgnoreCase(secondInputId)) {
                debug("Match gefunden: " + recipe.getKey());
                return recipe;
            }
        }

        return null;
    }

    /**
     * Gibt alle Rezepte zurück
     */
    public Collection<CraftingRecipe> getRecipes() {
        return recipes.values();
    }

    /**
     * Gibt Rezept nach Key zurück
     */
    public CraftingRecipe getRecipe(String key) {
        return recipes.get(key);
    }

    @Override
    public void reload() {
        recipes.clear();
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        loadRecipes();
    }

    /**
     * Rezept-Datenklasse
     */
    public static class CraftingRecipe {
        private final String key;
        private final String firstInput;
        private final String secondInput;
        private final String outputItem;
        private final int outputAmount;
        private final int expCost;

        public CraftingRecipe(String key, String firstInput, String secondInput,
                              String outputItem, int outputAmount, int expCost) {
            this.key = key;
            this.firstInput = firstInput;
            this.secondInput = secondInput;
            this.outputItem = outputItem;
            this.outputAmount = outputAmount;
            this.expCost = expCost;
        }

        public String getKey() { return key; }
        public String getFirstInput() { return firstInput; }
        public String getSecondInput() { return secondInput; }
        public String getOutputItem() { return outputItem; }
        public int getOutputAmount() { return outputAmount; }
        public int getExpCost() { return expCost; }

        /**
         * Erstellt Output ItemStack
         */
        public ItemStack createOutput() {
            ItemStack item = OraxenItems.getItemById(outputItem).build();
            if (item != null) {
                item.setAmount(outputAmount);
            }
            return item;
        }
    }
}