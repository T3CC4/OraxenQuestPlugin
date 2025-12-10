package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.QuestManager;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Listener für NPC-Interaktionen (Quest-Händler)
 */
public class NPCInteractListener implements Listener {

    private final OraxenQuestPlugin plugin;
    private final String npcName;
    private final EntityType npcType;

    public NPCInteractListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;

        // Cache Config-Werte (Performance)
        this.npcName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("quest-npc.name", "&6Quest Händler"));

        try {
            this.npcType = EntityType.valueOf(
                    plugin.getConfig().getString("quest-npc.type", "VILLAGER"));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Ungültiger NPC-Typ in Config: " +
                    plugin.getConfig().getString("quest-npc.type"));
        }
    }

    @EventHandler
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Schnelle Checks zuerst (Performance)
        if (entity.getType() != npcType) {
            return;
        }

        if (!isQuestNPC(entity)) {
            return;
        }

        // Quest-NPC erkannt
        handleQuestNPCInteraction(player, entity, event);
    }

    /**
     * Prüft ob Entity der Quest-NPC ist
     */
    private boolean isQuestNPC(Entity entity) {
        if (entity.getCustomName() == null) {
            return false;
        }
        return entity.getCustomName().equals(npcName);
    }

    /**
     * Behandelt Interaktion mit Quest-NPC
     */
    private void handleQuestNPCInteraction(Player player, Entity entity, PlayerInteractEntityEvent event) {
        QuestManager questManager = plugin.getQuestManager();

        // Spieler zum Tracking hinzufügen
        questManager.addTrackedPlayer(player.getUniqueId());

        // Prüfe Quest-Verfügbarkeit
        if (!questManager.isQuestAvailable()) {
            handleUnavailableQuest(player, questManager, event);
            return;
        }

        QuestManager.Quest quest = questManager.getCurrentQuest();
        if (quest == null) {
            handleNoQuest(player, event);
            return;
        }

        // Quest verfügbar - öffne Trade-GUI
        if (entity instanceof Villager) {
            openQuestTradeGUI(player, (Villager) entity, quest);
        }
    }

    /**
     * Quest nicht verfügbar - zeige Cooldown
     */
    private void handleUnavailableQuest(Player player, QuestManager questManager, PlayerInteractEntityEvent event) {
        QuestManager.Quest quest = questManager.getCurrentQuest();

        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Momentan sind keine Quests verfügbar!");
            player.sendMessage(ChatColor.GRAY + "Kontaktiere einen Administrator.");
        } else {
            long timeLeft = questManager.getTimeUntilAvailable();
            long hours = TimeUnit.MILLISECONDS.toHours(timeLeft);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;

            player.sendMessage(ChatColor.RED + "Quest im Cooldown!");
            player.sendMessage(ChatColor.YELLOW + "Verfügbar in: " + hours + "h " + minutes + "min");
        }

        event.setCancelled(true); // Verhindere GUI-Öffnung
    }

    /**
     * Keine Quest konfiguriert
     */
    private void handleNoQuest(Player player, PlayerInteractEntityEvent event) {
        player.sendMessage(ChatColor.RED + "Keine Quest verfügbar!");
        player.sendMessage(ChatColor.GRAY + "Alle Items existieren nicht in Oraxen.");
        event.setCancelled(true);
    }

    /**
     * Öffnet Trade-GUI mit Quest
     */
    private void openQuestTradeGUI(Player player, Villager villager, QuestManager.Quest quest) {
        // Validiere Items
        if (!OraxenItems.exists(quest.getRequiredItem()) ||
                !OraxenItems.exists(quest.getRewardItem())) {
            plugin.getPluginLogger().severe("Quest-Items nicht in Oraxen: " +
                    quest.getRequiredItem() + " oder " + quest.getRewardItem());
            player.sendMessage(ChatColor.RED + "Quest-Items fehlen!");
            return;
        }

        // Erstelle Trade-Rezept
        ItemStack requiredItem = OraxenItems.getItemById(quest.getRequiredItem()).build();
        ItemStack rewardItem = OraxenItems.getItemById(quest.getRewardItem()).build();

        if (requiredItem == null || rewardItem == null) {
            plugin.getPluginLogger().severe("Konnte Items nicht erstellen");
            player.sendMessage(ChatColor.RED + "Quest-Items konnten nicht erstellt werden!");
            return;
        }

        // Erstelle Rezept
        MerchantRecipe recipe = new MerchantRecipe(rewardItem, 999999);
        recipe.addIngredient(requiredItem);

        ArrayList<MerchantRecipe> recipes = new ArrayList<>();
        recipes.add(recipe);

        villager.setRecipes(recipes);

        // Zeige Geld-Belohnung
        if (quest.getMoneyReward() > 0) {
            player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.YELLOW + "Zusätzliche Belohnung:");
            player.sendMessage(ChatColor.GOLD + "  +" +
                    String.format("%.2f", quest.getMoneyReward()) + "$");
            player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }

        // GUI öffnet sich automatisch durch das Event
        plugin.getPluginLogger().fine("Trade-GUI für " + player.getName() + " aktualisiert");
    }
}