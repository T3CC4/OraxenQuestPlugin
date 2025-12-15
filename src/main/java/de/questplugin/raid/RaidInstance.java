package de.questplugin.raid;

import de.questplugin.OraxenQuestPlugin;
import de.questplugin.managers.MobEquipmentManager;
import de.questplugin.utils.MobHelper;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Aktive Raid-Instanz mit MobHelper für Equipment
 *
 * FEATURES:
 * - BossBar mit Mob-Counter
 * - Spectator-System (andere Spieler sehen/hören alles, kriegen aber keine Drops)
 * - Phasen-Tracking
 */
public class RaidInstance {

    private final OraxenQuestPlugin plugin;
    private final RaidConfig config;
    private final Player player;
    private final Location spawnLocation;

    private BossBar bossBar;
    private RaidState state;
    private int currentWave;
    private final Set<UUID> aliveMobs = new HashSet<>();
    private BukkitTask currentTask;

    // SPECTATOR-SYSTEM
    private final Set<UUID> spectators = new HashSet<>();
    private static final int SPECTATOR_RADIUS = 50; // Blöcke

    public RaidInstance(RaidConfig config, Player player, OraxenQuestPlugin plugin) {
        this.plugin = plugin;
        this.config = config;
        this.player = player;
        this.spawnLocation = player.getLocation().clone();
        this.state = RaidState.PREPARING;
        this.currentWave = 0;
    }

    /**
     * Startet den Raid
     */
    public void start() {
        createBossBar();
        startPreparation();
    }

    /**
     * Stoppt den Raid vorzeitig
     */
    public void stop() {
        cancel("Manuell gestoppt");
    }

    private void createBossBar() {
        String title = ChatColor.translateAlternateColorCodes('&',
                config.getDisplayName() + " &7- Vorbereitung");

        bossBar = Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
    }

    private void startPreparation() {
        int prepTime = config.getPreparationTime();

        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                config.getDisplayName()));
        player.sendMessage(ChatColor.YELLOW + "Vorbereitung: " + prepTime + " Sekunden");
        player.sendMessage(ChatColor.GRAY + "Wellen: " + config.getWaves().size());
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

        final int[] countdown = {prepTime};

        currentTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !isNearSpawnLocation()) {
                cancel("Spieler zu weit entfernt!");
                return;
            }

            countdown[0]--;

            // BossBar Update
            double progress = (double) countdown[0] / prepTime;
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));
            bossBar.setTitle(ChatColor.YELLOW + "Vorbereitung: " + countdown[0] + "s");

            // Spectators updaten
            updateSpectators();

            // Sound bei 10, 5, 3, 2, 1
            if (countdown[0] <= 10 && countdown[0] >= 1) {
                // Sound für Hauptspieler
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);

                // Sound für Spectators
                playSpectatorSound(Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
            }

            if (countdown[0] <= 0) {
                currentTask.cancel();
                startNextWave();
            }

        }, 0L, 20L);
    }

    private void startNextWave() {
        currentWave++;

        if (currentWave > config.getWaves().size()) {
            complete();
            return;
        }

        state = RaidState.ACTIVE;
        WaveConfig wave = config.getWaves().get(currentWave - 1);

        plugin.getPluginLogger().debug("=== Start Wave " + currentWave + " ===");
        plugin.getPluginLogger().debug("Wave Name: " + wave.getDisplayName());
        plugin.getPluginLogger().debug("Mob Types: " + wave.getMobs().size());

        String title = ChatColor.translateAlternateColorCodes('&',
                config.getDisplayName() + " &7- " + wave.getDisplayName());
        bossBar.setTitle(title);
        bossBar.setColor(BarColor.RED);
        bossBar.setProgress(1.0);

        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', wave.getDisplayName()));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

        // Spectator-Effekte
        playSpectatorSound(Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.0f);
        sendSpectatorMessage(ChatColor.translateAlternateColorCodes('&', wave.getDisplayName()));

        spawnWave(wave);
        startWaveMonitoring(wave);
    }

    private void spawnWave(WaveConfig wave) {
        double difficultyMultiplier = config.getDifficultyMultiplier();
        int spawnRadius = config.getSpawnRadius();

        plugin.getPluginLogger().debug("=== Spawn Wave " + currentWave + " ===");
        plugin.getPluginLogger().debug("Mob-Typen: " + wave.getMobs().size());

        for (WaveConfig.MobSpawn mobSpawn : wave.getMobs()) {
            plugin.getPluginLogger().debug("Spawne " + mobSpawn.getAmount() + "x " + mobSpawn.getType());

            for (int i = 0; i < mobSpawn.getAmount(); i++) {
                try {
                    Location loc = getRandomSpawnLocation(spawnRadius);

                    plugin.getPluginLogger().debug("  Spawn-Location: " +
                            loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

                    if (loc.getWorld() == null) {
                        plugin.getLogger().severe("Spawn-Location hat keine World!");
                        continue;
                    }

                    // Spawne Entity
                    Entity entity = loc.getWorld().spawnEntity(loc, mobSpawn.getType());

                    if (entity instanceof LivingEntity) {
                        LivingEntity mob = (LivingEntity) entity;
                        aliveMobs.add(mob.getUniqueId());

                        // Health
                        double health = mobSpawn.getHealth() * difficultyMultiplier;
                        if (mob.getAttribute(Attribute.MAX_HEALTH) != null) {
                            mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
                            mob.setHealth(health);
                        }

                        // Damage
                        if (mob.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                            double damage = mobSpawn.getDamage() * difficultyMultiplier;
                            mob.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
                        }

                        // Custom Name
                        if (mobSpawn.getCustomName() != null) {
                            mob.setCustomName(ChatColor.translateAlternateColorCodes('&',
                                    mobSpawn.getCustomName()));
                            mob.setCustomNameVisible(true);
                        }

                        // Equipment
                        if (mobSpawn.hasEquipment()) {
                            applyEquipment(mob);
                        }

                        mob.setRemoveWhenFarAway(false);
                        mob.setPersistent(true);

                        plugin.getPluginLogger().debug("  ✓ Mob gespawnt: " + mob.getType() +
                                " (UUID: " + mob.getUniqueId() + ")");
                    }

                } catch (Exception e) {
                    plugin.getLogger().severe("Fehler beim Spawnen von " + mobSpawn.getType() +
                            ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        plugin.getPluginLogger().info("Welle " + currentWave + ": " +
                aliveMobs.size() + " Mobs gespawnt");
        plugin.getPluginLogger().debug("==========================");
    }

    private void applyEquipment(LivingEntity mob) {
        MobEquipmentManager equipmentManager = plugin.getMobEquipmentManager();
        List<MobEquipmentManager.EquipmentEntry> equipment =
                equipmentManager.getEquipment(mob.getType());

        if (equipment.isEmpty()) {
            plugin.getPluginLogger().debug("Kein Equipment für " + mob.getType());
            return;
        }

        EntityEquipment entityEquipment = mob.getEquipment();
        if (entityEquipment == null) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int appliedItems = 0;

        for (MobEquipmentManager.EquipmentEntry entry : equipment) {
            double roll = random.nextDouble() * 100;

            if (roll < entry.getChance()) {
                ItemStack item = buildOraxenItem(entry.getOraxenItemId());

                if (item != null) {
                    switch (entry.getSlot()) {
                        case MAIN_HAND:
                            entityEquipment.setItemInMainHand(item);
                            entityEquipment.setItemInMainHandDropChance(entry.getDropChance());
                            break;
                        case OFF_HAND:
                            entityEquipment.setItemInOffHand(item);
                            entityEquipment.setItemInOffHandDropChance(entry.getDropChance());
                            break;
                        case HELMET:
                            entityEquipment.setHelmet(item);
                            entityEquipment.setHelmetDropChance(entry.getDropChance());
                            break;
                        case CHESTPLATE:
                            entityEquipment.setChestplate(item);
                            entityEquipment.setChestplateDropChance(entry.getDropChance());
                            break;
                        case LEGGINGS:
                            entityEquipment.setLeggings(item);
                            entityEquipment.setLeggingsDropChance(entry.getDropChance());
                            break;
                        case BOOTS:
                            entityEquipment.setBoots(item);
                            entityEquipment.setBootsDropChance(entry.getDropChance());
                            break;
                    }

                    appliedItems++;

                    plugin.getPluginLogger().debug("Equipment: " + mob.getType() +
                            " → " + entry.getSlot().getDisplayName() +
                            " = " + entry.getOraxenItemId() +
                            " (Drop: " + (entry.getDropChance() * 100) + "%)");
                }
            }
        }

        if (appliedItems > 0) {
            plugin.getPluginLogger().debug("Mob " + mob.getType() +
                    ": " + appliedItems + " Equipment-Items");
        }
    }

    private ItemStack buildOraxenItem(String oraxenId) {
        try {
            io.th0rgal.oraxen.items.ItemBuilder builder =
                    io.th0rgal.oraxen.api.OraxenItems.getItemById(oraxenId);

            if (builder != null) {
                return builder.build();
            }

            plugin.getPluginLogger().warn("Oraxen-Item nicht gefunden: " + oraxenId);
            return null;

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Laden von Item '" +
                    oraxenId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * FIX: BossBar zeigt jetzt Mob-Counter
     */
    private void startWaveMonitoring(WaveConfig wave) {
        int totalMobs = wave.getTotalMobCount();

        currentTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancel("Spieler offline!");
                return;
            }

            // Tote Mobs entfernen
            aliveMobs.removeIf(uuid -> {
                Entity entity = Bukkit.getEntity(uuid);
                return entity == null || entity.isDead();
            });

            // BossBar Update mit Counter
            double progress = (double) aliveMobs.size() / totalMobs;
            bossBar.setProgress(Math.max(0, Math.min(1, progress)));

            // UPDATE: Zeige verbleibende Mobs
            String title = ChatColor.translateAlternateColorCodes('&',
                    config.getDisplayName() + " &7- " + wave.getDisplayName());
            bossBar.setTitle(title + ChatColor.GRAY + " [" +
                    ChatColor.RED + aliveMobs.size() + ChatColor.GRAY + "/" +
                    ChatColor.WHITE + totalMobs + ChatColor.GRAY + "]");

            // Spectators updaten
            updateSpectators();

            // Welle abgeschlossen?
            if (aliveMobs.isEmpty()) {
                currentTask.cancel();
                onWaveComplete(wave);
            }

        }, 0L, 20L);
    }

    private void onWaveComplete(WaveConfig wave) {
        player.sendMessage(ChatColor.GREEN + "✓ " + wave.getDisplayName() +
                " abgeschlossen!");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Spectator-Effekte
        playSpectatorSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.0f);
        sendSpectatorMessage(ChatColor.GREEN + "✓ " + wave.getDisplayName() + " abgeschlossen!");

        if (currentWave >= config.getWaves().size()) {
            complete();
            return;
        }

        // Delay vor nächster Welle
        int delay = wave.getDelayAfterWave();
        if (delay > 0) {
            bossBar.setTitle(ChatColor.YELLOW + "Nächste Welle in " + delay + "s");
            bossBar.setColor(BarColor.YELLOW);
            bossBar.setProgress(1.0);

            Bukkit.getScheduler().runTaskLater(plugin, this::startNextWave, delay * 20L);
        } else {
            startNextWave();
        }
    }

    private void complete() {
        state = RaidState.COMPLETED;

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage(ChatColor.GREEN + "✔ RAID ABGESCHLOSSEN!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getDisplayName()));
        player.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);

        // Spectator-Finale
        playSpectatorSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
        sendSpectatorMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sendSpectatorMessage(ChatColor.GREEN + "✔ RAID ABGESCHLOSSEN!");
        sendSpectatorMessage(ChatColor.translateAlternateColorCodes('&', config.getDisplayName()));
        sendSpectatorMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Belohnungen (nur für Hauptspieler!)
        config.getRewards().giveRewards(player, plugin);

        cleanup();
    }

    public void cancel(String reason) {
        state = RaidState.CANCELLED;

        player.sendMessage(ChatColor.RED + "Raid abgebrochen: " + reason);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

        // Spectator-Info
        sendSpectatorMessage(ChatColor.RED + "Raid abgebrochen: " + reason);

        cleanup();
    }

    private void cleanup() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        // Raid-Mobs entfernen
        for (UUID uuid : aliveMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        aliveMobs.clear();

        spectators.clear();
        plugin.getRaidManager().removeRaid(player.getUniqueId());
    }

    private Location getRandomSpawnLocation(int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble(radius / 2.0, radius);

        double x = spawnLocation.getX() + distance * Math.cos(angle);
        double z = spawnLocation.getZ() + distance * Math.sin(angle);

        World world = spawnLocation.getWorld();
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);

        int y = world.getHighestBlockYAt(blockX, blockZ) + 1;

        if (y < world.getMinHeight()) {
            y = world.getMinHeight() + 1;
        }

        if (y > world.getMaxHeight() - 2) {
            y = world.getMaxHeight() - 2;
        }

        return new Location(world, x, y, z);
    }

    private boolean isNearSpawnLocation() {
        return player.getLocation().distance(spawnLocation) <= 50;
    }

    // ==================== SPECTATOR-SYSTEM ====================

    /**
     * Aktualisiert Spectator-Liste (Spieler im Umkreis)
     */
    private void updateSpectators() {
        Set<UUID> newSpectators = new HashSet<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Nicht der Hauptspieler
            if (p.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            // In selber Welt
            if (!p.getWorld().equals(spawnLocation.getWorld())) {
                continue;
            }

            // Im Umkreis
            if (p.getLocation().distance(spawnLocation) <= SPECTATOR_RADIUS) {
                newSpectators.add(p.getUniqueId());

                // Neu hinzugefügt?
                if (!spectators.contains(p.getUniqueId())) {
                    onSpectatorJoin(p);
                }

                // BossBar zeigen
                if (bossBar != null && !bossBar.getPlayers().contains(p)) {
                    bossBar.addPlayer(p);
                }
            }
        }

        // Entfernte Spectators
        for (UUID uuid : spectators) {
            if (!newSpectators.contains(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    onSpectatorLeave(p);
                    if (bossBar != null) {
                        bossBar.removePlayer(p);
                    }
                }
            }
        }

        spectators.clear();
        spectators.addAll(newSpectators);
    }

    private void onSpectatorJoin(Player spectator) {
        spectator.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        spectator.sendMessage(ChatColor.YELLOW + "Du beobachtest einen Raid!");
        spectator.sendMessage(ChatColor.GRAY + "Spieler: " + ChatColor.WHITE + player.getName());
        spectator.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getDisplayName()));
        spectator.sendMessage(ChatColor.GRAY + "Du erhältst keine Drops, aber siehst alles!");
        spectator.sendMessage(ChatColor.GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━");
        spectator.playSound(spectator.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
    }

    private void onSpectatorLeave(Player spectator) {
        spectator.sendMessage(ChatColor.GRAY + "Du bist zu weit vom Raid entfernt!");
    }

    /**
     * Spielt Sound für alle Spectators
     */
    private void playSpectatorSound(Sound sound, float volume, float pitch) {
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), sound, volume, pitch);
            }
        }
    }

    /**
     * Sendet Message an alle Spectators
     */
    private void sendSpectatorMessage(String message) {
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    /**
     * Gibt alle aktuellen Spectators zurück
     */
    public Set<Player> getSpectators() {
        Set<Player> players = new HashSet<>();
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                players.add(p);
            }
        }
        return players;
    }

    // ==================== GETTER ====================

    public Player getPlayer() { return player; }
    public RaidConfig getConfig() { return config; }
    public RaidState getState() { return state; }
    public int getCurrentWave() { return currentWave; }
    public int getAliveMobCount() { return aliveMobs.size(); }
    public int getSpectatorCount() { return spectators.size(); }

    public enum RaidState {
        PREPARING,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }
}