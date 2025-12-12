package de.questplugin.commands;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.raid.RaidInstance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console-only Command für Raid-Management
 * Kann von anderen Plugins genutzt werden
 */
public class RaidCommand implements CommandExecutor, TabCompleter {

    private final OraxenQuestPlugin plugin;

    public RaidCommand(OraxenQuestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // NUR Console oder Command-Block
        if (sender instanceof Player) {
            sender.sendMessage(ChatColor.RED + "Dieser Befehl kann nur von der Console ausgeführt werden!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                return handleStart(sender, args);

            case "stop":
                return handleStop(sender, args);

            case "info":
                return handleInfo(sender, args);

            case "list":
                return handleList(sender);

            case "reload":
                return handleReload(sender);

            default:
                sender.sendMessage(ChatColor.RED + "Unbekannter Befehl!");
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "━━━━━ Raid Commands ━━━━━");
        sender.sendMessage(ChatColor.YELLOW + "/raid start <raid-id> <spieler>" +
                ChatColor.GRAY + " - Raid starten");
        sender.sendMessage(ChatColor.YELLOW + "/raid stop <spieler>" +
                ChatColor.GRAY + " - Raid stoppen");
        sender.sendMessage(ChatColor.YELLOW + "/raid info <raid-id>" +
                ChatColor.GRAY + " - Raid-Info");
        sender.sendMessage(ChatColor.YELLOW + "/raid list" +
                ChatColor.GRAY + " - Alle Raids");
        sender.sendMessage(ChatColor.YELLOW + "/raid reload" +
                ChatColor.GRAY + " - Raids neu laden");
        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * Startet einen Raid
     * /raid start <raid-id> <spieler>
     */
    private boolean handleStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /raid start <raid-id> <spieler>");
            return true;
        }

        String raidId = args[1].toLowerCase();
        String playerName = args[2];

        // Prüfe Raid-Config
        if (plugin.getRaidManager().getRaidConfig(raidId) == null) {
            sender.sendMessage(ChatColor.RED + "Raid nicht gefunden: " + raidId);
            sender.sendMessage(ChatColor.GRAY + "Verfügbar: " +
                    plugin.getRaidManager().getRaidIds());
            return true;
        }

        // Prüfe Spieler
        Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Spieler nicht online: " + playerName);
            return true;
        }

        // Starte Raid
        boolean success = plugin.getRaidManager().startRaid(raidId, player);

        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Raid '" + raidId + "' gestartet für " + playerName);
        } else {
            sender.sendMessage(ChatColor.RED + "✗ Raid konnte nicht gestartet werden");
            sender.sendMessage(ChatColor.GRAY + "Mögliche Gründe:");
            sender.sendMessage(ChatColor.GRAY + "  - Spieler bereits in Raid");
            sender.sendMessage(ChatColor.GRAY + "  - Falsches Biom");
        }

        return true;
    }

    /**
     * Stoppt einen Raid
     * /raid stop <spieler>
     */
    private boolean handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /raid stop <spieler>");
            return true;
        }

        String playerName = args[1];
        Player player = Bukkit.getPlayer(playerName);

        if (player == null || !player.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Spieler nicht online: " + playerName);
            return true;
        }

        boolean success = plugin.getRaidManager().stopRaid(player);

        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Raid gestoppt für " + playerName);
            player.sendMessage(ChatColor.RED + "Dein Raid wurde abgebrochen!");
        } else {
            sender.sendMessage(ChatColor.RED + "✗ Spieler ist nicht in einem Raid");
        }

        return true;
    }

    /**
     * Zeigt Raid-Info
     * /raid info <raid-id>
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /raid info <raid-id>");
            return true;
        }

        String raidId = args[1].toLowerCase();
        de.questplugin.raid.RaidConfig config = plugin.getRaidManager().getRaidConfig(raidId);

        if (config == null) {
            sender.sendMessage(ChatColor.RED + "Raid nicht gefunden: " + raidId);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━ Raid Info ━━━━━");
        sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + config.getId());
        sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + config.getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "Wellen: " + ChatColor.WHITE + config.getTotalWaves());
        sender.sendMessage(ChatColor.YELLOW + "Schwierigkeit: " + ChatColor.WHITE + config.getDifficultyMultiplier() + "x");
        sender.sendMessage(ChatColor.YELLOW + "Spawn-Radius: " + ChatColor.WHITE + config.getSpawnRadius() + "m");
        sender.sendMessage(ChatColor.YELLOW + "Vorbereitung: " + ChatColor.WHITE + config.getPreparationTime() + "s");

        if (config.getAllowedBiomes().size() < 10) {
            sender.sendMessage(ChatColor.YELLOW + "Biome: " + ChatColor.WHITE +
                    config.getAllowedBiomes().toString());
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Biome: " + ChatColor.WHITE + "Alle");
        }

        // Wellen-Info
        sender.sendMessage(ChatColor.GOLD + "Wellen:");
        for (int i = 0; i < config.getWaves().size(); i++) {
            de.questplugin.raid.WaveConfig wave = config.getWaves().get(i);
            sender.sendMessage(ChatColor.GRAY + "  " + (i + 1) + ". " +
                    ChatColor.WHITE + wave.getDisplayName() +
                    ChatColor.GRAY + " (" + wave.getTotalMobCount() + " Mobs)");
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━");
        return true;
    }

    /**
     * Listet alle verfügbaren Raids
     * /raid list
     */
    private boolean handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "━━━━━ Verfügbare Raids ━━━━━");

        for (String raidId : plugin.getRaidManager().getRaidIds()) {
            de.questplugin.raid.RaidConfig config = plugin.getRaidManager().getRaidConfig(raidId);
            sender.sendMessage(ChatColor.YELLOW + "• " + raidId +
                    ChatColor.GRAY + " - " +
                    ChatColor.WHITE + config.getDisplayName() +
                    ChatColor.GRAY + " (" + config.getTotalWaves() + " Wellen)");
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Aktive Raids: " +
                ChatColor.WHITE + plugin.getRaidManager().getActiveRaids().size());

        // Zeige aktive Raids
        for (RaidInstance raid : plugin.getRaidManager().getActiveRaids()) {
            sender.sendMessage(ChatColor.GRAY + "  - " +
                    ChatColor.WHITE + raid.getPlayer().getName() +
                    ChatColor.GRAY + " (" + raid.getConfig().getId() +
                    " - Welle " + raid.getCurrentWave() + ")");
        }

        sender.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━");
        return true;
    }

    /**
     * Lädt Raids neu
     * /raid reload
     */
    private boolean handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Lade Raids neu...");

        try {
            plugin.getRaidManager().reload();
            sender.sendMessage(ChatColor.GREEN + "✓ Raids neu geladen!");
            sender.sendMessage(ChatColor.GRAY + "Verfügbar: " +
                    plugin.getRaidManager().getRaidIds().size() + " Raids");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "✗ Fehler beim Reload: " + e.getMessage());
            plugin.getLogger().severe("Raid-Reload-Fehler: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        // Keine Tab-Complete für Spieler
        if (sender instanceof Player) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("start", "stop", "info", "list", "reload")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("info")) {
                // Raid-IDs
                return new ArrayList<>(plugin.getRaidManager().getRaidIds())
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("stop")) {
                // Online-Spieler
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            // Spieler-Namen
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}