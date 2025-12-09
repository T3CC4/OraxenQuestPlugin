package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.QuestManager;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TradeCompleteListener implements Listener {

    private final OraxenQuestPlugin plugin;
    private final Set<UUID> completedPlayers;

    private static final Set<InventoryAction> purchaseSingleItemActions;
    static {
        purchaseSingleItemActions = new HashSet<>();
        purchaseSingleItemActions.add(InventoryAction.PICKUP_ONE);
        purchaseSingleItemActions.add(InventoryAction.PICKUP_ALL);
        purchaseSingleItemActions.add(InventoryAction.PICKUP_HALF);
        purchaseSingleItemActions.add(InventoryAction.PICKUP_SOME);
        purchaseSingleItemActions.add(InventoryAction.DROP_ONE_SLOT);
        purchaseSingleItemActions.add(InventoryAction.DROP_ALL_SLOT);
        purchaseSingleItemActions.add(InventoryAction.HOTBAR_SWAP);
    }

    public TradeCompleteListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.completedPlayers = new HashSet<>();
    }

    // Custom Event Klasse
    public static class VillagerTradeEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        final HumanEntity player;
        final AbstractVillager villager;
        final MerchantRecipe recipe;
        final int orders;
        boolean cancelled = false;

        public VillagerTradeEvent(HumanEntity player, AbstractVillager villager, MerchantRecipe recipe, int orders) {
            this.player = player;
            this.villager = villager;
            this.recipe = recipe;
            this.orders = orders;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelled(boolean toCancel) {
            cancelled = toCancel;
        }

        public HumanEntity getPlayer() {
            return player;
        }

        public AbstractVillager getVillager() {
            return villager;
        }

        public MerchantRecipe getRecipe() {
            return recipe;
        }

        public int getOrders() {
            return orders;
        }

        @NotNull
        @Override
        public HandlerList getHandlers() {
            return handlers;
        }

        @NotNull
        public static HandlerList getHandlerList() {
            return handlers;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClickEvent(final InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.NOTHING) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getInventory().getHolder() instanceof AbstractVillager villager)) return;

        final HumanEntity player = event.getWhoClicked();
        final MerchantInventory merchantInventory = (MerchantInventory) event.getInventory();
        final MerchantRecipe recipe = merchantInventory.getSelectedRecipe();

        if (recipe == null) return;

        plugin.getLogger().info("=== Villager Trade Detected ===");
        plugin.getLogger().info("Player: " + player.getName());
        plugin.getLogger().info("Action: " + event.getAction());
        plugin.getLogger().info("Villager: " + villager.getCustomName());

        VillagerTradeEvent vtEvent = null;
        if (purchaseSingleItemActions.contains(event.getAction())) {
            vtEvent = new VillagerTradeEvent(player, villager, recipe, 1);
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // SHIFT+CLICK - multiple orders
            List<ItemStack> ingredients = recipe.getIngredients();
            ItemStack input1 = merchantInventory.getItem(0);

            int maxOrders = 1;
            if (input1 != null && ingredients.get(0) != null) {
                maxOrders = input1.getAmount() / ingredients.get(0).getAmount();
            }

            vtEvent = new VillagerTradeEvent(player, villager, recipe, maxOrders);
        }

        if (vtEvent != null) {
            plugin.getLogger().info("Firing VillagerTradeEvent");
            vtEvent.setCancelled(event.isCancelled());
            Bukkit.getPluginManager().callEvent(vtEvent);
            event.setCancelled(vtEvent.isCancelled());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVillagerTradeEvent(VillagerTradeEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getVillager() instanceof Villager villager)) return;

        plugin.getLogger().info("=== VillagerTradeEvent Handler ===");
        plugin.getLogger().info("Player: " + player.getName());
        plugin.getLogger().info("Villager: " + villager.getCustomName());

        // Prüfe ob es unser Quest-Villager ist
        String npcName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("quest-npc.name", "&6Quest Händler"));

        if (villager.getCustomName() == null || !villager.getCustomName().equals(npcName)) {
            plugin.getLogger().info("Falscher Villager");
            return;
        }

        plugin.getLogger().info("✓ Quest-Villager erkannt!");

        QuestManager questManager = plugin.getQuestManager();
        QuestManager.Quest quest = questManager.getCurrentQuest();

        if (quest == null) {
            plugin.getLogger().info("✗ Keine Quest");
            player.sendMessage(ChatColor.RED + "Keine Quest verfügbar!");
            event.setCancelled(true);
            return;
        }

        // Prüfe Quest-Verfügbarkeit
        if (!questManager.isQuestAvailable()) {
            plugin.getLogger().info("✗ Quest im Cooldown");
            long timeLeft = questManager.getTimeUntilAvailable();
            long minutes = timeLeft / (60 * 1000);
            player.sendMessage(ChatColor.RED + "Quest im Cooldown!");
            player.sendMessage(ChatColor.YELLOW + "Verfügbar in: " + minutes + " Minuten");
            event.setCancelled(true);
            return;
        }

        // Prüfe Trade Result
        ItemStack result = event.getRecipe().getResult();
        plugin.getLogger().info("Trade Result: " + result.getType());

        String resultId = OraxenItems.getIdByItem(result);
        plugin.getLogger().info("Result Oraxen ID: " + resultId);
        plugin.getLogger().info("Expected: " + quest.getRewardItem());

        if (resultId == null || !resultId.equals(quest.getRewardItem())) {
            plugin.getLogger().info("✗ Item-ID falsch");
            return;
        }

        plugin.getLogger().info("✓ Item-ID korrekt!");

        UUID playerUUID = player.getUniqueId();

        // Prüfe ob bereits abgeschlossen
        if (completedPlayers.contains(playerUUID)) {
            plugin.getLogger().info("✗ Bereits abgeschlossen!");
            player.sendMessage(ChatColor.RED + "Du hast diese Quest bereits abgeschlossen!");
            event.setCancelled(true);
            return;
        }

        plugin.getLogger().info("========================================");
        plugin.getLogger().info("✓✓✓ QUEST COMPLETION ✓✓✓");
        plugin.getLogger().info("========================================");

        // Markiere als completed
        completedPlayers.add(playerUUID);

        // Quest abschließen
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info(">>> Quest Completion Task");

            questManager.completeQuestForPlayer(player);

            player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.GREEN + "✔ Quest abgeschlossen!");
            player.sendMessage(ChatColor.YELLOW + "Belohnung: " + ChatColor.WHITE + quest.getRewardItem());
            player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Leere Liste für neue Quest
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (questManager.isQuestAvailable()) {
                    completedPlayers.clear();
                    plugin.getLogger().info("Completed-Liste geleert");
                }
            }, 100L);
        });
    }
}