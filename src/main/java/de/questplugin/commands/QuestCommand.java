package de.questplugin.commands;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.QuestManager;
import de.questplugin.utils.StructureHelper;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class QuestCommand implements CommandExecutor {

    private final OraxenQuestPlugin plugin;

    public QuestCommand(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "━━━━━ Quest Plugin ━━━━━");
            sender.sendMessage(ChatColor.YELLOW + "/quest info" + ChatColor.GRAY + " - Zeigt aktuelle Quest");
            sender.sendMessage(ChatColor.YELLOW + "/quest reload" + ChatColor.GRAY + " - Lädt Config neu");
            sender.sendMessage(ChatColor.YELLOW + "/quest spawnnpc" + ChatColor.GRAY + " - Spawnt Quest-NPC");
            sender.sendMessage(ChatColor.YELLOW + "/quest structures" + ChatColor.GRAY + " - Liste aller Strukturen");
            sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                showQuestInfo(sender);
                break;

            case "reload":
                if (!sender.hasPermission("quest.reload")) {
                    sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
                    return true;
                }
                plugin.reloadConfig();
                plugin.getDataManager().reload();
                plugin.getDropManager().reload();
                plugin.getChestManager().reload();
                plugin.getQuestManager().reload();
                sender.sendMessage(ChatColor.GREEN + "✔ Config neu geladen!");
                break;

            case "spawnnpc":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Nur für Spieler!");
                    return true;
                }
                if (!sender.hasPermission("quest.spawnnpc")) {
                    sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
                    return true;
                }
                spawnQuestNPC((Player) sender);
                break;

            case "structures":
                showStructures(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unbekannter Befehl!");
                break;
        }

        return true;
    }

    private void showQuestInfo(CommandSender sender) {
        QuestManager questManager = plugin.getQuestManager();

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━ Quest Info ━━━━━━━");

        if (!questManager.isQuestAvailable()) {
            long timeLeft = questManager.getTimeUntilAvailable();
            long hours = TimeUnit.MILLISECONDS.toHours(timeLeft);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;

            sender.sendMessage(ChatColor.RED + "Quest nicht verfügbar!");
            sender.sendMessage(ChatColor.YELLOW + "Verfügbar in: " + hours + "h " + minutes + "min");
            sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        QuestManager.Quest quest = questManager.getCurrentQuest();
        if (quest == null) {
            sender.sendMessage(ChatColor.RED + "Keine Quest geladen!");
            sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Benötigt: " + ChatColor.WHITE + quest.getRequiredItem());
        sender.sendMessage(ChatColor.YELLOW + "Belohnung: " + ChatColor.GREEN + quest.getRewardItem());
        if (quest.getMoneyReward() > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Geld: " + ChatColor.GOLD + quest.getMoneyReward() + "$");
        }
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void spawnQuestNPC(Player player) {
        String npcName = plugin.getConfig().getString("quest-npc.name", "&6Quest Händler");
        EntityType npcType;
        try {
            npcType = EntityType.valueOf(plugin.getConfig().getString("quest-npc.type", "VILLAGER"));
        } catch (IllegalArgumentException e) {
            npcType = EntityType.VILLAGER;
        }

        LivingEntity npc = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), npcType);
        npc.setCustomName(ChatColor.translateAlternateColorCodes('&', npcName));
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);

        // Zusätzliche Mob-Einstellungen
        if (npc instanceof org.bukkit.entity.Mob) {
            org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) npc;
            mob.setAware(false);
        }

        player.sendMessage(ChatColor.GREEN + "✔ Quest-NPC gespawnt!");
        player.sendMessage(ChatColor.YELLOW + "Name: " + npc.getCustomName());
        player.sendMessage(ChatColor.YELLOW + "UUID: " + npc.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Invulnerable: " + npc.isInvulnerable());
        player.sendMessage(ChatColor.YELLOW + "AI: " + npc.hasAI());

        plugin.getLogger().info("NPC gespawnt: " + npc.getCustomName() + " (" + npc.getUniqueId() + ")");
    }

    private void showStructures(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(StructureHelper.getFormattedStructureList());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Nutze diese Namen in der config.yml unter 'chest-loot'");
        sender.sendMessage(ChatColor.GRAY + "Beispiel: " + ChatColor.WHITE + "ancient_city:" + ChatColor.GRAY + " oder " +
                ChatColor.WHITE + "village:" + ChatColor.GRAY + " (matched alle Dorf-Strukturen)");
        sender.sendMessage(ChatColor.GRAY + "Siehe auch: " + ChatColor.YELLOW + "StructureType.java");
    }
}