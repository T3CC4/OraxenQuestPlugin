package de.questplugin.listeners;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.QuestManager;
import io.th0rgal.oraxen.api.OraxenItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private static final Set<InventoryAction> PURCHASE_ACTIONS = EnumSet.of(
            InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_SOME,
            InventoryAction.DROP_ONE_SLOT,
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.HOTBAR_SWAP
    );

    public TradeCompleteListener(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    public static class VillagerTradeEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        final HumanEntity player;
        final AbstractVillager villager;
        final MerchantRecipe recipe;
        final int orders;
        boolean cancelled = false;

        public VillagerTradeEvent(HumanEntity player, AbstractVillager villager,
                                  MerchantRecipe recipe, int orders) {
            this.player = player;
            this.villager = villager;
            this.recipe = recipe;
            this.orders = orders;
        }

        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancel) { cancelled = cancel; }
        public HumanEntity getPlayer() { return player; }
        public AbstractVillager getVillager() { return villager; }
        public MerchantRecipe getRecipe() { return recipe; }
        public int getOrders() { return orders; }

        @NotNull
        @Override
        public HandlerList getHandlers() { return handlers; }

        @NotNull
        public static HandlerList getHandlerList() { return handlers; }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        // Validierung
        if (event.getAction() == InventoryAction.NOTHING) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(event.getInventory().getHolder() instanceof AbstractVillager)) return;

        AbstractVillager villager = (AbstractVillager) event.getInventory().getHolder();

        HumanEntity player = event.getWhoClicked();
        MerchantInventory merchantInventory = (MerchantInventory) event.getInventory();
        MerchantRecipe recipe = merchantInventory.getSelectedRecipe();

        if (recipe == null) return;

        VillagerTradeEvent vtEvent = null;

        if (PURCHASE_ACTIONS.contains(event.getAction())) {
            // Einzelner Kauf
            vtEvent = new VillagerTradeEvent(player, villager, recipe, 1);
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // SHIFT+CLICK - Mehrfach-Kauf
            List<ItemStack> ingredients = recipe.getIngredients();
            ItemStack input1 = merchantInventory.getItem(0);

            int maxOrders = 1;
            if (input1 != null && !ingredients.isEmpty() && ingredients.get(0) != null) {
                maxOrders = input1.getAmount() / ingredients.get(0).getAmount();
            }

            vtEvent = new VillagerTradeEvent(player, villager, recipe, maxOrders);
        }

        if (vtEvent != null) {
            vtEvent.setCancelled(event.isCancelled());
            Bukkit.getPluginManager().callEvent(vtEvent);
            event.setCancelled(vtEvent.isCancelled());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVillagerTrade(VillagerTradeEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!(event.getVillager() instanceof Villager)) return;

        Player player = (Player) event.getPlayer();
        Villager villager = (Villager) event.getVillager();

        // Quest-Villager prüfen (Legacy String für Config-Kompatibilität)
        String expectedName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("quest-npc.name", "&6Quest Händler"));

        Component villagerName = villager.customName();
        if (villagerName == null) return;

        // Vergleiche mit legacy String (Paper serialisiert Component zu Legacy für Vergleich)
        String villagerNameLegacy = org.bukkit.ChatColor.stripColor(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .serialize(villagerName));
        String expectedNameStripped = org.bukkit.ChatColor.stripColor(expectedName);

        if (!villagerNameLegacy.equals(expectedNameStripped)) {
            return;
        }

        QuestManager questManager = plugin.getQuestManager();
        QuestManager.Quest quest = questManager.getCurrentQuest();

        // Validierung
        if (quest == null) {
            player.sendMessage(Component.text("Keine Quest verfügbar!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (!questManager.isQuestAvailable()) {
            long timeLeft = questManager.getTimeUntilAvailable();
            long minutes = timeLeft / (60 * 1000);
            player.sendMessage(Component.text("Quest im Cooldown!", NamedTextColor.RED));
            player.sendMessage(Component.text("Verfügbar in: ", NamedTextColor.YELLOW)
                    .append(Component.text(minutes + " Minuten")));
            event.setCancelled(true);
            return;
        }

        // Item-Validierung
        ItemStack result = event.getRecipe().getResult();
        String resultId = OraxenItems.getIdByItem(result);

        if (resultId == null || !resultId.equals(quest.getRewardItem())) {
            return; // Falsches Item
        }

        // Prüfe ob bereits abgeschlossen
        if (questManager.hasCompletedCurrentQuest(player.getUniqueId())) {
            player.sendMessage(Component.text("Du hast diese Quest bereits abgeschlossen!", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        // Quest abschließen
        questManager.markQuestCompleted(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin, () -> {
            questManager.completeQuestForPlayer(player);

            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN));
            player.sendMessage(Component.text("✔ Quest abgeschlossen!", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Belohnung: ", NamedTextColor.YELLOW)
                    .append(Component.text(quest.getRewardItem(), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN));
        });
    }
}