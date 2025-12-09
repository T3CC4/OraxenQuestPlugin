package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.QuestManager;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NPCInteractListener implements Listener {

    private final OraxenQuestPlugin plugin;

    public NPCInteractListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        //plugin.getLogger().info("=== PlayerInteractEntityEvent ===");
        //plugin.getLogger().info("Entity: " + entity.getType());
        //plugin.getLogger().info("Custom Name: " + entity.getCustomName());
        //plugin.getLogger().info("Spieler: " + player.getName());

        // Prüfe ob es der Quest-NPC ist
        String npcName = plugin.getConfig().getString("quest-npc.name", "&6Quest Händler");
        EntityType npcType;
        try {
            npcType = EntityType.valueOf(plugin.getConfig().getString("quest-npc.type", "VILLAGER"));
        } catch (IllegalArgumentException e) {
            npcType = EntityType.VILLAGER;
        }

        if (entity.getType() != npcType) {
            //plugin.getLogger().info("Falscher Entity-Type: " + entity.getType() + " != " + npcType);
            return;
        }

        if (entity.getCustomName() == null ||
                !entity.getCustomName().equals(ChatColor.translateAlternateColorCodes('&', npcName))) {
            ///plugin.getLogger().info("Falscher Name: " + entity.getCustomName());
            return;
        }

        plugin.getLogger().info("✓ Quest-NPC erkannt!");

        QuestManager questManager = plugin.getQuestManager();

        // Spieler zum Tracking hinzufügen
        questManager.addTrackedPlayer(player.getUniqueId());

        // Prüfe ob Quest verfügbar ist
        if (!questManager.isQuestAvailable()) {
            QuestManager.Quest quest = questManager.getCurrentQuest();

            if (quest == null) {
                player.sendMessage(ChatColor.RED + "Momentan sind keine Quests verfügbar!");
                player.sendMessage(ChatColor.GRAY + "Bitte kontaktiere einen Administrator.");
                event.setCancelled(true); // Verhindere GUI-Öffnung
                return;
            }

            long timeLeft = questManager.getTimeUntilAvailable();
            long hours = TimeUnit.MILLISECONDS.toHours(timeLeft);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;

            player.sendMessage(ChatColor.RED + "Die Quest ist momentan nicht verfügbar!");
            player.sendMessage(ChatColor.YELLOW + "Verfügbar in: " + hours + "h " + minutes + "min");
            event.setCancelled(true); // Verhindere GUI-Öffnung
            return;
        }

        QuestManager.Quest quest = questManager.getCurrentQuest();
        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Momentan sind keine Quests verfügbar!");
            player.sendMessage(ChatColor.GRAY + "Alle konfigurierten Items existieren nicht in Oraxen.");
            event.setCancelled(true); // Verhindere GUI-Öffnung
            return;
        }

        // GUI darf sich öffnen - Quest ist verfügbar!

        // Öffne Trade-GUI mit echtem Villager
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            updateVillagerTrades(villager, quest);

            // Zeige Geld-Belohnung
            if (quest.getMoneyReward() > 0) {
                player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage(ChatColor.YELLOW + "Zusätzliche Belohnung:");
                player.sendMessage(ChatColor.GOLD + "  +" + quest.getMoneyReward() + "$");
                player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }

            // GUI öffnet sich automatisch durch das Event
            //plugin.getLogger().info("Trades aktualisiert - GUI öffnet sich automatisch");
        }
    }

    private void updateVillagerTrades(Villager villager, QuestManager.Quest quest) {
        // Prüfe ob Items existieren bevor wir sie laden
        if (!OraxenItems.exists(quest.getRequiredItem()) || !OraxenItems.exists(quest.getRewardItem())) {
            //plugin.getLogger().severe("Quest-Items nicht gefunden: " + quest.getRequiredItem() + " oder " + quest.getRewardItem());
            return;
        }

        // Erstelle Trade-Rezept
        ItemStack requiredItem = OraxenItems.getItemById(quest.getRequiredItem()).build();
        ItemStack rewardItem = OraxenItems.getItemById(quest.getRewardItem()).build();

        if (requiredItem == null || rewardItem == null) {
            //plugin.getLogger().severe("Konnte Items nicht erstellen");
            return;
        }

        MerchantRecipe recipe = new MerchantRecipe(rewardItem, 999999);
        recipe.addIngredient(requiredItem);

        ArrayList<MerchantRecipe> recipes = new ArrayList<>();
        recipes.add(recipe);

        villager.setRecipes(recipes);

        plugin.getLogger().info("Villager Trades aktualisiert: " + quest.getRequiredItem() + " -> " + quest.getRewardItem());
    }
}