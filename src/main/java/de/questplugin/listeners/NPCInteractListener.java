package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.QuestManager;
import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private final String npcNameLegacy;
    private final EntityType npcType;

    public NPCInteractListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;

        // Cache Config-Werte (Performance)
        this.npcNameLegacy = ChatColor.translateAlternateColorCodes('&',
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
     * Prüft ob Entity der Quest-NPC ist (Paper Component API)
     */
    private boolean isQuestNPC(Entity entity) {
        Component customName = entity.customName();
        if (customName == null) {
            return false;
        }

        // Vergleiche mit legacy String (Config-Kompatibilität)
        String entityNameStripped = ChatColor.stripColor(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .serialize(customName));
        String expectedNameStripped = ChatColor.stripColor(npcNameLegacy);

        return entityNameStripped.equals(expectedNameStripped);
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
            player.sendMessage(Component.text("Momentan sind keine Quests verfügbar!", NamedTextColor.RED));
            player.sendMessage(Component.text("Kontaktiere einen Administrator.", NamedTextColor.GRAY));
        } else {
            long timeLeft = questManager.getTimeUntilAvailable();
            long hours = TimeUnit.MILLISECONDS.toHours(timeLeft);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;

            player.sendMessage(Component.text("Quest im Cooldown!", NamedTextColor.RED));
            player.sendMessage(Component.text("Verfügbar in: ", NamedTextColor.YELLOW)
                    .append(Component.text(hours + "h " + minutes + "min")));
        }

        event.setCancelled(true); // Verhindere GUI-Öffnung
    }

    /**
     * Keine Quest konfiguriert
     */
    private void handleNoQuest(Player player, PlayerInteractEntityEvent event) {
        player.sendMessage(Component.text("Keine Quest verfügbar!", NamedTextColor.RED));
        player.sendMessage(Component.text("Alle Items existieren nicht in Oraxen.", NamedTextColor.GRAY));
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
            player.sendMessage(Component.text("Quest-Items fehlen!", NamedTextColor.RED));
            return;
        }

        // Erstelle Trade-Rezept
        ItemStack requiredItem = OraxenItems.getItemById(quest.getRequiredItem()).build();
        ItemStack rewardItem = OraxenItems.getItemById(quest.getRewardItem()).build();

        if (requiredItem == null || rewardItem == null) {
            plugin.getPluginLogger().severe("Konnte Items nicht erstellen");
            player.sendMessage(Component.text("Quest-Items konnten nicht erstellt werden!", NamedTextColor.RED));
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
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Zusätzliche Belohnung:", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("  +" + String.format("%.2f", quest.getMoneyReward()) + "$", NamedTextColor.GOLD));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        }

        // GUI öffnet sich automatisch durch das Event
        plugin.getPluginLogger().fine("Trade-GUI für " + player.getName() + " aktualisiert");
    }
}