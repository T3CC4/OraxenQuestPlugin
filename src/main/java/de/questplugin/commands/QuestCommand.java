package de.questplugin.commands;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.QuestManager;
import de.questplugin.utils.BiomeHelper;
import de.questplugin.utils.MobHelper;
import de.questplugin.utils.StructureHelper;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class QuestCommand implements CommandExecutor, TabCompleter {

    private final OraxenQuestPlugin plugin;

    private static final String PREFIX = ChatColor.GOLD + "[Quest] " + ChatColor.RESET;
    private static final String PERMISSION_BASE = "quest.";

    public QuestCommand(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                return handleInfo(sender);

            case "reload":
                return handleReload(sender);

            case "spawnnpc":
                return handleSpawnNPC(sender);

            case "structures":
                return handleStructures(sender);

            case "biomes":
                return handleBiomes(sender);

            case "mobs":
                return handleMobs(sender);

            case "debug":
                return handleDebug(sender, args);

            default:
                sender.sendMessage(PREFIX + ChatColor.RED + "Unbekannter Befehl!");
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "━━━━━ Quest Plugin ━━━━━");
        sender.sendMessage(ChatColor.YELLOW + "/quest info" +
                ChatColor.GRAY + " - Aktuelle Quest");
        sender.sendMessage(ChatColor.YELLOW + "/quest reload" +
                ChatColor.GRAY + " - Config neu laden");
        sender.sendMessage(ChatColor.YELLOW + "/quest spawnnpc" +
                ChatColor.GRAY + " - Quest-NPC spawnen");
        sender.sendMessage(ChatColor.YELLOW + "/quest structures" +
                ChatColor.GRAY + " - Alle Strukturen");
        sender.sendMessage(ChatColor.YELLOW + "/quest biomes" +
                ChatColor.GRAY + " - Alle Biome");
        sender.sendMessage(ChatColor.YELLOW + "/quest mobs" +
                ChatColor.GRAY + " - Alle Mobs");
        sender.sendMessage(ChatColor.YELLOW + "/quest debug <on|off>" +
                ChatColor.GRAY + " - Debug-Mode");
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private boolean handleInfo(CommandSender sender) {
        QuestManager questManager = plugin.getQuestManager();

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━ Quest Info ━━━━━━━");

        if (!questManager.isQuestAvailable()) {
            long timeLeft = questManager.getTimeUntilAvailable();
            long hours = TimeUnit.MILLISECONDS.toHours(timeLeft);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft) % 60;

            sender.sendMessage(ChatColor.RED + "Quest nicht verfügbar!");
            sender.sendMessage(ChatColor.YELLOW + "Verfügbar in: " +
                    hours + "h " + minutes + "min");
            sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        QuestManager.Quest quest = questManager.getCurrentQuest();
        if (quest == null) {
            sender.sendMessage(ChatColor.RED + "Keine Quest geladen!");
            sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Benötigt: " +
                ChatColor.WHITE + quest.getRequiredItem());
        sender.sendMessage(ChatColor.YELLOW + "Belohnung: " +
                ChatColor.GREEN + quest.getRewardItem());

        if (quest.getMoneyReward() > 0) {
            sender.sendMessage(ChatColor.YELLOW + "Geld: " +
                    ChatColor.GOLD + String.format("%.2f", quest.getMoneyReward()) + "$");
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!checkPermission(sender, "reload")) {
            return true;
        }

        sender.sendMessage(PREFIX + ChatColor.YELLOW + "Lade Config neu...");

        try {
            plugin.reloadConfig();
            plugin.getDataManager().reload();
            plugin.getBlockDropManager().reload();
            plugin.getMobDropManager().reload();
            plugin.getChestManager().reload();
            plugin.getCraftingManager().reload();
            plugin.getMobEquipmentManager().reload();
            plugin.getQuestManager().reload();
            plugin.getRaidManager().reload();
            plugin.getEliteMobManager().reload();

            if (plugin.getEliteDropListener() != null) {
                plugin.getEliteDropListener().reload();
            }

            sender.sendMessage(PREFIX + ChatColor.GREEN + "✓ Config neu geladen!");
        } catch (Exception e) {
            sender.sendMessage(PREFIX + ChatColor.RED + "✗ Fehler beim Reload: " + e.getMessage());
            plugin.getLogger().severe("Reload-Fehler: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean handleSpawnNPC(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Nur für Spieler!");
            return true;
        }

        Player player = (Player) sender;

        if (!checkPermission(sender, "spawnnpc")) {
            return true;
        }

        String npcName = plugin.getConfig().getString("quest-npc.name", "&6Quest Händler");
        EntityType npcType;

        try {
            npcType = EntityType.valueOf(
                    plugin.getConfig().getString("quest-npc.type", "VILLAGER")
            );
        } catch (IllegalArgumentException e) {
            npcType = EntityType.VILLAGER;
        }

        LivingEntity npc = (LivingEntity) player.getWorld()
                .spawnEntity(player.getLocation(), npcType);

        npc.setCustomName(ChatColor.translateAlternateColorCodes('&', npcName));
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);

        if (npc instanceof org.bukkit.entity.Mob) {
            org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) npc;
            mob.setAware(false);
        }

        player.sendMessage(PREFIX + ChatColor.GREEN + "✓ Quest-NPC gespawnt!");
        player.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + npc.getUniqueId());

        return true;
    }

    private boolean handleStructures(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(StructureHelper.getFormattedStructureList());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Nutze diese Namen in der config.yml");
        sender.sendMessage(ChatColor.GRAY + "Beispiel: " +
                ChatColor.WHITE + "ancient_city:" +
                ChatColor.GRAY + " oder " +
                ChatColor.WHITE + "village:");
        return true;
    }

    private boolean handleBiomes(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(BiomeHelper.getFormattedBiomeList());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Nutze diese Namen in raids.yml");
        sender.sendMessage(ChatColor.GRAY + "Beispiel: " +
                ChatColor.WHITE + "allowed-biomes: [PLAINS, FOREST]");
        sender.sendMessage(ChatColor.GRAY + "Case-insensitive: " +
                ChatColor.WHITE + "plains" + ChatColor.GRAY + " = " +
                ChatColor.WHITE + "PLAINS");
        sender.sendMessage(ChatColor.GRAY + "Alle Biome: " +
                ChatColor.WHITE + "allowed-biomes: [\"*\"]");
        return true;
    }

    private boolean handleMobs(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(MobHelper.getFormattedMobList());
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Nutze diese Namen in:");
        sender.sendMessage(ChatColor.GRAY + "  - raids.yml (Wellen)");
        sender.sendMessage(ChatColor.GRAY + "  - config.yml (mob-drops, mob-equipment)");
        sender.sendMessage(ChatColor.GRAY + "Beispiel: " +
                ChatColor.WHITE + "type: ZOMBIE");
        sender.sendMessage(ChatColor.GRAY + "Case-insensitive: " +
                ChatColor.WHITE + "zombie" + ChatColor.GRAY + " = " +
                ChatColor.WHITE + "ZOMBIE");
        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "debug")) {
            return true;
        }

        if (args.length < 2) {
            boolean currentDebug = plugin.getConfig().getBoolean("debug-mode", false);
            sender.sendMessage(PREFIX + "Debug-Mode: " +
                    (currentDebug ? ChatColor.GREEN + "AN" : ChatColor.RED + "AUS"));
            sender.sendMessage(ChatColor.GRAY + "Nutze: /quest debug <on|off>");
            return true;
        }

        boolean enable = args[1].equalsIgnoreCase("on") ||
                args[1].equalsIgnoreCase("true");

        plugin.getConfig().set("debug-mode", enable);
        plugin.saveConfig();

        // Update alle Manager
        plugin.getBlockDropManager().setDebugMode(enable);
        plugin.getMobDropManager().setDebugMode(enable);

        sender.sendMessage(PREFIX + "Debug-Mode: " +
                (enable ? ChatColor.GREEN + "AN" : ChatColor.RED + "AUS"));
        sender.sendMessage(ChatColor.YELLOW + "⚠ Produziert viele Logs!");

        return true;
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(PERMISSION_BASE + permission)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Keine Berechtigung!");
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "reload", "spawnnpc", "structures",
                            "biomes", "mobs", "debug")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return Arrays.asList("on", "off")
                    .stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}