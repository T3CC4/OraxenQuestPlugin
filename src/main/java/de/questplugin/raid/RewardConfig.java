package de.questplugin.raid;

import de.questplugin.OraxenQuestPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Belohnungs-Konfiguration für Raids mit Multiplayer-Bonus
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
     * Gibt Belohnungen mit Schwierigkeits-Multiplikator
     * Items werden random an Spieler verteilt
     * Geld wird fair aufgeteilt mit Bonus
     *
     * @param player Spieler
     * @param plugin Plugin-Instanz
     * @param difficultyMultiplier Schwierigkeitsmultiplikator (z.B. 1.4 für 2 Spieler)
     */
    public void giveRewards(Player player, OraxenQuestPlugin plugin, double difficultyMultiplier) {
        giveRewards(player, plugin, difficultyMultiplier, 1);
    }

    /**
     * Gibt Belohnungen mit Spieleranzahl-Berücksichtigung
     *
     * @param player Spieler
     * @param plugin Plugin-Instanz
     * @param difficultyMultiplier Schwierigkeitsmultiplikator
     * @param playerCount Anzahl der Spieler im Raid
     */
    public void giveRewards(Player player, OraxenQuestPlugin plugin, double difficultyMultiplier, int playerCount) {
        boolean rewardsGiven = false;
        Random random = new Random();

        player.sendMessage(Component.text("━━━━ BELOHNUNGEN ━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Schwierigkeit: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f", difficultyMultiplier) + "x", NamedTextColor.YELLOW)));

        // Oraxen Items - RANDOM verteilt
        if (!oraxenItems.isEmpty()) {
            int itemsReceived = 0;

            for (String itemId : oraxenItems) {
                // Jeder Spieler hat Chance basierend auf Spieleranzahl
                // Bei 1 Spieler: 100% Chance
                // Bei 2 Spielern: ~70% Chance pro Item
                // Bei 3 Spielern: ~55% Chance pro Item
                double baseChance = 1.0 / Math.sqrt(playerCount);

                if (random.nextDouble() < baseChance) {
                    try {
                        io.th0rgal.oraxen.items.ItemBuilder builder =
                                io.th0rgal.oraxen.api.OraxenItems.getItemById(itemId);

                        if (builder != null) {
                            ItemStack item = builder.build();

                            // Menge bleibt Standard - Items sind schon wertvoll genug
                            player.getInventory().addItem(item);

                            Component itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName()
                                    ? item.getItemMeta().displayName()
                                    : Component.text(itemId);

                            player.sendMessage(Component.text("✓ ", NamedTextColor.GREEN).append(itemName));
                            itemsReceived++;
                            rewardsGiven = true;
                        } else {
                            plugin.getLogger().warning("Oraxen-Item nicht gefunden: " + itemId);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Fehler beim Geben von Item '" + itemId + "': " + e.getMessage());
                    }
                }
            }

            if (itemsReceived == 0 && !oraxenItems.isEmpty()) {
                player.sendMessage(Component.text("✗ Keine Items erhalten (Pech gehabt!)", NamedTextColor.GRAY));
            }
        }

        // Geld - GETEILT aber mit Bonus
        // Formel: (BaseGeld * Schwierigkeit * 1.2) / Spieleranzahl
        // 1.2 = 20% Bonus damit es sich lohnt
        if (money > 0) {
            if (plugin.hasEconomy()) {
                try {
                    double totalMoney = money * difficultyMultiplier * 1.2;
                    double playerMoney = totalMoney / playerCount;

                    net.milkbowl.vault.economy.Economy economy = plugin.getEconomy();
                    economy.depositPlayer(player, playerMoney);
                    player.sendMessage(Component.text("✓ Geld: +", NamedTextColor.GOLD)
                            .append(Component.text(String.format("%.2f", playerMoney) + "$")));
                    rewardsGiven = true;
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Geben von Geld: " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("Vault Economy nicht verfügbar!");
                player.sendMessage(Component.text("✗ Geld-Belohnung nicht verfügbar (Vault fehlt)", NamedTextColor.RED));
            }
        }

        // Erfahrung - GETEILT mit Bonus
        // Ähnlich wie Geld
        if (experience > 0) {
            int totalExp = (int) Math.ceil(experience * difficultyMultiplier * 1.2);
            int playerExp = Math.max(1, totalExp / playerCount);

            player.giveExp(playerExp);
            player.sendMessage(Component.text("✓ Erfahrung: +", NamedTextColor.AQUA)
                    .append(Component.text(playerExp + " XP")));
            rewardsGiven = true;
        }

        // Commands
        if (!commands.isEmpty()) {
            for (String command : commands) {
                try {
                    String processedCommand = command
                            .replace("%player%", player.getName())
                            .replace("%multiplier%", String.format("%.2f", difficultyMultiplier))
                            .replace("%playercount%", String.valueOf(playerCount));

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                    plugin.getPluginLogger().debug("Command ausgeführt: " + processedCommand);
                    rewardsGiven = true;
                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Ausführen von Command '" + command + "': " + e.getMessage());
                }
            }
        }

        if (rewardsGiven) {
            player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
}