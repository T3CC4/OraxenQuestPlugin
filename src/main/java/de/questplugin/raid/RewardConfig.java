package de.questplugin.raid;

import de.questplugin.OraxenQuestPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Belohnungs-Konfiguration für Raids
 */
public class RewardConfig {

    private final List<String> oraxenItems;
    private final double money;
    private final int experience;
    private final List<String> commands;

    private RewardConfig(List<String> oraxenItems, double money, int experience, List<String> commands) {
        this.oraxenItems = oraxenItems;
        this.money = money;
        this.experience = experience;
        this.commands = commands;
    }

    /**
     * Lädt RewardConfig aus ConfigurationSection
     */
    public static RewardConfig load(ConfigurationSection section, OraxenQuestPlugin plugin) {
        if (section == null) {
            return new RewardConfig(new ArrayList<>(), 0, 0, new ArrayList<>());
        }

        List<String> oraxenItems = section.getStringList("oraxen-items");
        double money = section.getDouble("money", 0);
        int experience = section.getInt("experience", 0);
        List<String> commands = section.getStringList("commands");

        return new RewardConfig(oraxenItems, money, experience, commands);
    }

    public List<String> getOraxenItems() {
        return oraxenItems;
    }

    public double getMoney() {
        return money;
    }

    public int getExperience() {
        return experience;
    }

    public List<String> getCommands() {
        return commands;
    }

    public boolean hasRewards() {
        return !oraxenItems.isEmpty() || money > 0 || experience > 0 || !commands.isEmpty();
    }

    /**
     * Gibt Belohnungen an Spieler aus
     */
    public void giveRewards(org.bukkit.entity.Player player, OraxenQuestPlugin plugin) {
        boolean rewardsGiven = false;

        // Oraxen Items
        if (!oraxenItems.isEmpty()) {
            for (String itemId : oraxenItems) {
                try {
                    io.th0rgal.oraxen.items.ItemBuilder builder =
                            io.th0rgal.oraxen.api.OraxenItems.getItemById(itemId);

                    if (builder != null) {
                        org.bukkit.inventory.ItemStack item = builder.build();
                        player.getInventory().addItem(item);
                        player.sendMessage(org.bukkit.ChatColor.GREEN +
                                "✓ Belohnung: " + item.getItemMeta().getDisplayName());
                        rewardsGiven = true;
                    } else {
                        plugin.getLogger().warning("Oraxen-Item nicht gefunden: " + itemId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Geben von Item '" + itemId + "': " + e.getMessage());
                }
            }
        }

        // Geld (Vault)
        if (money > 0) {
            if (plugin.hasEconomy()) {
                try {
                    net.milkbowl.vault.economy.Economy economy = plugin.getEconomy();
                    economy.depositPlayer(player, money);
                    player.sendMessage(org.bukkit.ChatColor.GOLD +
                            "✓ Geld: +" + String.format("%.2f", money) + "$");
                    rewardsGiven = true;
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Geben von Geld: " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("Vault Economy nicht verfügbar - Geld-Belohnung übersprungen!");
                player.sendMessage(org.bukkit.ChatColor.RED +
                        "✗ Geld-Belohnung nicht verfügbar (Vault fehlt)");
            }
        }

        // Erfahrung
        if (experience > 0) {
            player.giveExp(experience);
            player.sendMessage(org.bukkit.ChatColor.AQUA +
                    "✓ Erfahrung: +" + experience + " XP");
            rewardsGiven = true;
        }

        // Commands
        if (!commands.isEmpty()) {
            for (String command : commands) {
                try {
                    String processedCommand = command.replace("%player%", player.getName());
                    org.bukkit.Bukkit.dispatchCommand(
                            org.bukkit.Bukkit.getConsoleSender(),
                            processedCommand
                    );
                    plugin.getPluginLogger().debug("Command ausgeführt: " + processedCommand);
                    rewardsGiven = true;
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Ausführen von Command '" + command + "': " + e.getMessage());
                }
            }
        }

        if (rewardsGiven) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
}