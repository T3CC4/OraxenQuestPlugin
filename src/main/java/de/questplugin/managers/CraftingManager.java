package de.questplugin.managers;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.enums.CraftingType;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet Custom Crafting Rezepte für Anvil und Smithing Table
 */
public class CraftingManager extends BaseManager {

    // Rezept-Cache: CraftingType -> RecipeKey -> Recipe
    private final Map<CraftingType, Map<String, CraftingRecipe>> recipes = new ConcurrentHashMap<>();

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
        loadRecipesForType(CraftingType.ANVIL, "anvil-recipes", recipesConfig);

        // Lade Smithing-Rezepte
        loadRecipesForType(CraftingType.SMITHING_TABLE, "smithing-recipes", recipesConfig);

        int totalRecipes = recipes.values().stream()
                .mapToInt(Map::size)
                .sum();

        info("Crafting-Rezepte: " + totalRecipes + " geladen");
    }

    private void loadRecipesForType(CraftingType type, String configPath,
                                    org.bukkit.configuration.file.FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(configPath);
        if (section == null) {
            debug("Keine " + configPath + " Sektion in recipes.yml");
            return;
        }

        Map<String, CraftingRecipe> typeRecipes = new ConcurrentHashMap<>();
        int loaded = 0;

        for (String recipeKey : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(recipeKey);
            if (recipeSection == null) continue;

            CraftingRecipe recipe = loadRecipe(type, recipeKey, recipeSection);
            if (recipe != null) {
                typeRecipes.put(recipeKey, recipe);
                loaded++;

                debug("Rezept geladen: " + type + "/" + recipeKey);
                debug("  Input: " + recipe.getInputItem());
                if (recipe.getSecondInput() != null) {
                    debug("  Second: " + recipe.getSecondInput());
                }
                debug("  Output: " + recipe.getOutputItem() + " x" + recipe.getOutputAmount());
                debug("  Cost: " + recipe.getExpCost() + " XP");
            }
        }

        recipes.put(type, typeRecipes);
        info(type.getDisplayName() + ": " + loaded + " Rezepte");
    }

    private CraftingRecipe loadRecipe(CraftingType type, String key, ConfigurationSection section) {
        String inputItem = section.getString("input");
        String outputItem = section.getString("output");
        int outputAmount = section.getInt("output-amount", 1);
        int expCost = section.getInt("exp-cost", 0);

        // Validierung
        if (!validateItem(inputItem)) {
            warn("Rezept '" + key + "': Input-Item '" + inputItem + "' ungültig");
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

        // Smithing Table: optionales zweites Item
        String secondInput = null;
        if (type == CraftingType.SMITHING_TABLE) {
            secondInput = section.getString("second-input");
            if (secondInput != null && !validateItem(secondInput)) {
                warn("Rezept '" + key + "': Second-Input '" + secondInput + "' ungültig");
                return null;
            }
        }

        return new CraftingRecipe(
                key,
                type,
                inputItem,
                secondInput,
                outputItem,
                outputAmount,
                expCost
        );
    }

    /**
     * Findet Rezept für Anvil (ein Item)
     */
    public CraftingRecipe findAnvilRecipe(ItemStack input) {
        if (input == null) return null;

        String inputId = OraxenItems.getIdByItem(input);
        if (inputId == null) return null;

        return findRecipe(CraftingType.ANVIL, inputId, null);
    }

    /**
     * Findet Rezept für Smithing Table (zwei Items)
     */
    public CraftingRecipe findSmithingRecipe(ItemStack input, ItemStack secondInput) {
        if (input == null) return null;

        String inputId = OraxenItems.getIdByItem(input);
        if (inputId == null) return null;

        String secondInputId = null;
        if (secondInput != null) {
            secondInputId = OraxenItems.getIdByItem(secondInput);
        }

        return findRecipe(CraftingType.SMITHING_TABLE, inputId, secondInputId);
    }

    /**
     * Interne Methode zum Finden eines Rezepts
     */
    private CraftingRecipe findRecipe(CraftingType type, String inputId, String secondInputId) {
        Map<String, CraftingRecipe> typeRecipes = recipes.get(type);
        if (typeRecipes == null || typeRecipes.isEmpty()) {
            return null;
        }

        for (CraftingRecipe recipe : typeRecipes.values()) {
            // Prüfe erstes Item
            if (!recipe.getInputItem().equalsIgnoreCase(inputId)) {
                continue;
            }

            // Anvil: kein zweites Item nötig
            if (type == CraftingType.ANVIL) {
                debug("Match gefunden: " + recipe.getKey());
                return recipe;
            }

            // Smithing: zweites Item prüfen
            if (type == CraftingType.SMITHING_TABLE) {
                // Kein zweites Item konfiguriert = Match
                if (recipe.getSecondInput() == null) {
                    debug("Match gefunden: " + recipe.getKey());
                    return recipe;
                }

                // Zweites Item muss matchen
                if (secondInputId != null &&
                        recipe.getSecondInput().equalsIgnoreCase(secondInputId)) {
                    debug("Match gefunden: " + recipe.getKey());
                    return recipe;
                }
            }
        }

        return null;
    }

    /**
     * Gibt alle Rezepte eines Typs zurück
     */
    public Collection<CraftingRecipe> getRecipes(CraftingType type) {
        Map<String, CraftingRecipe> typeRecipes = recipes.get(type);
        return typeRecipes != null ? typeRecipes.values() : Collections.emptyList();
    }

    /**
     * Gibt Rezept nach Key zurück
     */
    public CraftingRecipe getRecipe(CraftingType type, String key) {
        Map<String, CraftingRecipe> typeRecipes = recipes.get(type);
        return typeRecipes != null ? typeRecipes.get(key) : null;
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
        private final CraftingType type;
        private final String inputItem;
        private final String secondInput;
        private final String outputItem;
        private final int outputAmount;
        private final int expCost;

        public CraftingRecipe(String key, CraftingType type, String inputItem,
                              String secondInput, String outputItem,
                              int outputAmount, int expCost) {
            this.key = key;
            this.type = type;
            this.inputItem = inputItem;
            this.secondInput = secondInput;
            this.outputItem = outputItem;
            this.outputAmount = outputAmount;
            this.expCost = expCost;
        }

        public String getKey() { return key; }
        public CraftingType getType() { return type; }
        public String getInputItem() { return inputItem; }
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